package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.dto.ProviderTokenRefreshResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.ProviderTokenLoginPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ProviderToken;
import java.util.Map.Entry;
import com.omnistack.backend.domain.model.ProviderTokenLoginCommand;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;
import com.omnistack.backend.shared.exception.BusinessException;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Servicio de aplicacion para resolucion y refresco de tokens de integracion.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProviderTokenService implements ProviderTokenResolverUseCase, ProviderTokenAdministrationUseCase {

    private static final String TOKEN_MODE_LOGIN = "LOGIN";
    private static final String TOKEN_MODE_STATIC = "STATIC";

    private final ProviderConfigService providerConfigService;
    private final ProviderTokenLoginPort providerTokenLoginPort;
    private final ProviderWsService providerWsService;
    private final Clock clock;
    private final ConcurrentMap<String, CachedProviderToken> tokenCache = new ConcurrentHashMap<>();

    /**
     * Obtiene el token vigente para el proveedor indicado.
     *
     * @param categoryCode categoria comercial
     * @param subcategoryCode subcategoria comercial
     * @param serviceProviderCode codigo de proveedor
     * @return token aplicable
     */
    @Override
    public String getToken(String categoryCode, String subcategoryCode, String serviceProviderCode) {
        Entry<String, AppProperties.ProviderProperties> entry = getRequiredProviderEntry(categoryCode, subcategoryCode, serviceProviderCode);
        AppProperties.ProviderProperties provider = entry.getValue();
        if (usesStaticToken(provider)) {
            return getStaticToken(provider);
        }
        return getDynamicToken(entry.getKey(), provider, false).getToken();
    }

    /**
     * Resuelve el token directamente por clave de proveedor, sin pasar por la
     * busqueda ambigua por categoria/subcategoria/codigo de proveedor.
     */
    @Override
    public String getToken(String providerKey) {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(providerKey);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor " + providerKey);
        }
        if (usesStaticToken(provider)) {
            return getStaticToken(provider);
        }
        return getDynamicToken(providerKey, provider, false).getToken();
    }

    /**
     * Regenera el token vigente para el proveedor indicado y actualiza el cache.
     *
     * @param categoryCode categoria comercial
     * @param subcategoryCode subcategoria comercial
     * @param serviceProviderCode codigo de proveedor
     * @return token regenerado
     */
    @Override
    public String refreshToken(String categoryCode, String subcategoryCode, String serviceProviderCode) {
        Entry<String, AppProperties.ProviderProperties> entry = getRequiredProviderEntry(categoryCode, subcategoryCode, serviceProviderCode);
        AppProperties.ProviderProperties provider = entry.getValue();
        if (usesStaticToken(provider)) {
            return getStaticToken(provider);
        }
        return getDynamicToken(entry.getKey(), provider, true).getToken();
    }

    /**
     * Regenera el token vigente para el proveedor indicado (por clave de
     * proveedor) y actualiza el cache. Ver {@link #getToken(String)}.
     */
    @Override
    public String refreshToken(String providerKey) {
        AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(providerKey);
        if (provider == null) {
            throw new IntegrationException("No existe configuracion para el proveedor " + providerKey);
        }
        if (usesStaticToken(provider)) {
            return getStaticToken(provider);
        }
        return getDynamicToken(providerKey, provider, true).getToken();
    }

    /**
     * Fuerza el refresco manual del token para el proveedor indicado.
     *
     * @param request solicitud de refresco
     * @return metadata del token actualizado
     */
    @Override
    public ProviderTokenRefreshResponse refreshToken(ProviderTokenRefreshRequest request) {
        Entry<String, AppProperties.ProviderProperties> entry = getRequiredProviderEntry(
                request.getCategoryCode(),
                request.getSubcategoryCode(),
                request.getServiceProviderCode());
        AppProperties.ProviderProperties provider = entry.getValue();
        if (usesStaticToken(provider)) {
            throw new BusinessException("El proveedor " + provider.getServiceProviderCode()
                    + " para category_code=" + request.getCategoryCode()
                    + " y subcategory_code=" + request.getSubcategoryCode()
                    + " no requiere refresco dinamico de token");
        }

        ProviderToken refreshedToken = getDynamicToken(entry.getKey(), provider, true);
        return ProviderTokenRefreshResponse.builder()
                .errorFlag(false)
                .status(StatusDetail.builder()
                        .code("00")
                        .message("Token actualizado correctamente")
                        .build())
                .categoryCode(refreshedToken.getCategoryCode())
                .subcategoryCode(refreshedToken.getSubcategoryCode())
                .serviceProviderCode(refreshedToken.getServiceProviderCode())
                .providerName(refreshedToken.getProviderName())
                .refreshedAt(refreshedToken.getRefreshedAt())
                .expiresAt(refreshedToken.getExpiresAt())
                .build();
    }

    /**
     * Intenta refrescar al arranque todos los proveedores con token dinamico configurado.
     */
    @Override
    public void refreshTokensOnStartup() {
        for (String providerKey : providerConfigService.allProviderKeys()) {
            AppProperties.ProviderProperties provider = providerConfigService.getProviderProperties(providerKey);
            if (provider == null || !requiresStartupRefresh(provider)) {
                continue;
            }
            if (!providerWsService.hasUrl(providerKey, "LOGIN")) {
                log.warn(
                        "Provider LOGIN URL not configured in DB, skipping startup token refresh. providerKey={}, providerName={}",
                        providerKey,
                        provider.getProviderName());
                continue;
            }
            try {
                ProviderToken token = getDynamicToken(providerKey, provider, true);
                log.info(
                        "Provider token refreshed on startup. categoryCode={}, subcategoryCode={}, serviceProviderCode={}, providerName={}, expiresAt={}",
                        token.getCategoryCode(),
                        token.getSubcategoryCode(),
                        token.getServiceProviderCode(),
                        token.getProviderName(),
                        token.getExpiresAt());
            } catch (RuntimeException exception) {
                log.error(
                        "Provider token refresh failed on startup. categoryCode={}, subcategoryCode={}, serviceProviderCode={}, providerName={}",
                        provider.getCategoryCode(),
                        provider.getSubcategoryCode(),
                        provider.getServiceProviderCode(),
                        provider.getProviderName(),
                        exception);
            }
        }
    }

    private synchronized ProviderToken getDynamicToken(String providerKey, AppProperties.ProviderProperties provider, boolean forceRefresh) {
        Instant now = clock.instant();
        // Cache por providerKey (no por categoria/subcategoria/codigo de proveedor: dos
        // proveedores distintos, ej. tradicional y pega3, pueden compartir esos 3 valores
        // cuando ninguno tiene subcategoryCode propio configurado).
        //
        // OJO: se probo cachear por "sesion compartida" (loginUrl+usuario) asumiendo que
        // Loteria Nacional acepta cualquier token vigente de la cuenta sin importar el
        // productoVender usado al loguear (tradicional/loteria/pega3 comparten cuenta).
        // La evidencia real lo contradice: un token obtenido con productoVender=Pega3 fue
        // rechazado por un endpoint de Tradicionales aun siendo la sesion mas reciente de
        // esa cuenta ("Usuario sin token de sesion valido"). Es decir, el token SI esta
        // atado a su propio producto — lo que SI parece cierto es que loguearse para un
        // producto invalida la sesion vigente de los otros del lado del proveedor. Por eso
        // el cache debe seguir siendo por providerKey (cada producto con su propio token),
        // y la mitigacion real es reintentar con refresh forzado cuando el proveedor
        // rechaza el token (ver TradicionalWebClientAdapter, patron ya usado en
        // Bet593WithdrawWebClientAdapter.isInvalidTokenResponse).
        CachedProviderToken current = tokenCache.get(providerKey);
        if (!forceRefresh && current != null && !current.isExpired(now)) {
            return current.token();
        }

        ProviderToken refreshedToken = requestNewToken(providerKey, provider, now);
        tokenCache.put(providerKey, new CachedProviderToken(refreshedToken));
        return refreshedToken;
    }

    private ProviderToken requestNewToken(String providerKey, AppProperties.ProviderProperties provider, Instant now) {
        AppProperties.ProviderTokenProperties auth = Objects.requireNonNull(provider.getAuth());
        AppProperties.ProviderLoginProperties login = Objects.requireNonNull(auth.getLogin());
        validateLoginConfiguration(provider, login);

        String loginUrl = providerWsService.requireUrl(providerKey, "LOGIN", provider.getProviderName());

        ProviderTokenLoginResult loginResult = providerTokenLoginPort.login(ProviderTokenLoginCommand.builder()
                .categoryCode(provider.getCategoryCode())
                .subcategoryCode(provider.getSubcategoryCode())
                .serviceProviderCode(provider.getServiceProviderCode())
                .providerName(provider.getProviderName())
                .loginUrl(loginUrl)
                .username(login.getUsername())
                .password(login.getPassword())
                .productToSell(login.getProductToSell())
                .deviceId(login.getUsername())
                .build());

        if (loginResult.getToken() == null || loginResult.getToken().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no retorno un token valido");
        }

        Duration ttl = resolveTtl(auth.getTtlHours(), provider);
        OffsetDateTime refreshedAt = OffsetDateTime.ofInstant(now, clock.getZone());
        return ProviderToken.builder()
                .categoryCode(provider.getCategoryCode())
                .subcategoryCode(provider.getSubcategoryCode())
                .serviceProviderCode(provider.getServiceProviderCode())
                .providerName(provider.getProviderName())
                .token(loginResult.getToken())
                .refreshedAt(refreshedAt)
                .expiresAt(refreshedAt.plus(ttl))
                .build();
    }

    private void validateLoginConfiguration(
            AppProperties.ProviderProperties provider,
            AppProperties.ProviderLoginProperties login) {
        if (login.getUsername() == null || login.getUsername().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene login.username configurado");
        }
        if (login.getPassword() == null || login.getPassword().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene login.password configurado");
        }
        if (login.getProductToSell() == null || login.getProductToSell().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene login.productToSell configurado");
        }
    }

    private Duration resolveTtl(long ttlHours, AppProperties.ProviderProperties provider) {
        if (ttlHours <= 0) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " tiene auth.ttlHours invalido");
        }
        return Duration.ofHours(ttlHours);
    }

    private boolean requiresStartupRefresh(AppProperties.ProviderProperties provider) {
        return usesDynamicToken(provider) && provider.getAuth().isRefreshOnStartup();
    }

    private boolean usesStaticToken(AppProperties.ProviderProperties provider) {
        String mode = normalize(provider.getAuth() != null ? provider.getAuth().getMode() : null);
        return mode == null || TOKEN_MODE_STATIC.equals(mode);
    }

    private boolean usesDynamicToken(AppProperties.ProviderProperties provider) {
        return TOKEN_MODE_LOGIN.equals(normalize(provider.getAuth() != null ? provider.getAuth().getMode() : null));
    }

    private String getStaticToken(AppProperties.ProviderProperties provider) {
        if (provider.getToken() == null || provider.getToken().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene token configurado");
        }
        return provider.getToken();
    }

    private Entry<String, AppProperties.ProviderProperties> getRequiredProviderEntry(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode) {
        return providerConfigService.allProviderKeys().stream()
                .map(key -> Map.entry(key, providerConfigService.getProviderProperties(key)))
                .filter(e -> e.getValue() != null)
                .filter(e -> normalize(e.getValue().getServiceProviderCode()) != null)
                .filter(e -> normalize(e.getValue().getServiceProviderCode()).equals(normalize(serviceProviderCode)))
                .filter(e -> matchesContext(e.getValue(), categoryCode, subcategoryCode))
                .max(Comparator.comparingInt(e -> specificityScore(e.getValue())))
                .orElseThrow(() -> new BusinessException(
                        "No existe configuracion para category_code=" + categoryCode
                                + ", subcategory_code=" + subcategoryCode
                                + ", service_provider_code=" + serviceProviderCode));
    }

    private boolean matchesContext(
            AppProperties.ProviderProperties provider,
            String categoryCode,
            String subcategoryCode) {
        String providerCategoryCode = normalize(provider.getCategoryCode());
        String providerSubcategoryCode = normalize(provider.getSubcategoryCode());
        String expectedCategoryCode = normalize(categoryCode);
        String expectedSubcategoryCode = normalize(subcategoryCode);

        boolean categoryMatches = providerCategoryCode == null || providerCategoryCode.equals(expectedCategoryCode);
        boolean subcategoryMatches = providerSubcategoryCode == null || providerSubcategoryCode.equals(expectedSubcategoryCode);
        return categoryMatches && subcategoryMatches;
    }

    private int specificityScore(AppProperties.ProviderProperties provider) {
        int score = 0;
        if (normalize(provider.getCategoryCode()) != null) {
            score++;
        }
        if (normalize(provider.getSubcategoryCode()) != null) {
            score++;
        }
        return score;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase();
    }

    private record CachedProviderToken(ProviderToken token) {
        boolean isExpired(Instant now) {
            return !token.getExpiresAt().toInstant().isAfter(now);
        }
    }
}
