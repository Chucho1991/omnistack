package com.omnistack.backend.application.service;

import com.omnistack.backend.application.dto.ProviderTokenRefreshRequest;
import com.omnistack.backend.application.dto.ProviderTokenRefreshResponse;
import com.omnistack.backend.application.dto.StatusDetail;
import com.omnistack.backend.application.port.in.ProviderTokenAdministrationUseCase;
import com.omnistack.backend.application.port.in.ProviderTokenResolverUseCase;
import com.omnistack.backend.application.port.out.ProviderTokenLoginPort;
import com.omnistack.backend.config.properties.AppProperties;
import com.omnistack.backend.domain.model.ProviderToken;
import com.omnistack.backend.domain.model.ProviderTokenLoginCommand;
import com.omnistack.backend.domain.model.ProviderTokenLoginResult;
import com.omnistack.backend.shared.exception.BusinessException;
import com.omnistack.backend.shared.exception.IntegrationException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Comparator;
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

    private final AppProperties appProperties;
    private final ProviderTokenLoginPort providerTokenLoginPort;
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
        AppProperties.ProviderProperties provider = getRequiredProvider(categoryCode, subcategoryCode, serviceProviderCode);
        if (usesStaticToken(provider)) {
            return getStaticToken(provider);
        }
        return getDynamicToken(provider, false).getToken();
    }

    /**
     * Fuerza el refresco manual del token para el proveedor indicado.
     *
     * @param request solicitud de refresco
     * @return metadata del token actualizado
     */
    @Override
    public ProviderTokenRefreshResponse refreshToken(ProviderTokenRefreshRequest request) {
        AppProperties.ProviderProperties provider = getRequiredProvider(
                request.getCategoryCode(),
                request.getSubcategoryCode(),
                request.getServiceProviderCode());
        if (usesStaticToken(provider)) {
            throw new BusinessException("El proveedor " + provider.getServiceProviderCode()
                    + " para category_code=" + request.getCategoryCode()
                    + " y subcategory_code=" + request.getSubcategoryCode()
                    + " no requiere refresco dinamico de token");
        }

        ProviderToken refreshedToken = getDynamicToken(provider, true);
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
        for (AppProperties.ProviderProperties provider : appProperties.getIntegration().getProviders().values()) {
            if (!requiresStartupRefresh(provider)) {
                continue;
            }
            try {
                ProviderToken token = getDynamicToken(provider, true);
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

    private synchronized ProviderToken getDynamicToken(AppProperties.ProviderProperties provider, boolean forceRefresh) {
        Instant now = clock.instant();
        String cacheKey = cacheKey(provider.getCategoryCode(), provider.getSubcategoryCode(), provider.getServiceProviderCode());
        CachedProviderToken current = tokenCache.get(cacheKey);
        if (!forceRefresh && current != null && !current.isExpired(now)) {
            return current.token();
        }

        ProviderToken refreshedToken = requestNewToken(provider, now);
        tokenCache.put(cacheKey, new CachedProviderToken(refreshedToken));
        return refreshedToken;
    }

    private ProviderToken requestNewToken(AppProperties.ProviderProperties provider, Instant now) {
        AppProperties.ProviderTokenProperties auth = Objects.requireNonNull(provider.getAuth());
        AppProperties.ProviderLoginProperties login = Objects.requireNonNull(auth.getLogin());
        validateLoginConfiguration(provider, login);

        ProviderTokenLoginResult loginResult = providerTokenLoginPort.login(ProviderTokenLoginCommand.builder()
                .categoryCode(provider.getCategoryCode())
                .subcategoryCode(provider.getSubcategoryCode())
                .serviceProviderCode(provider.getServiceProviderCode())
                .providerName(provider.getProviderName())
                .baseUrl(provider.getBaseUrl())
                .path(login.getPath())
                .username(login.getUsername())
                .password(login.getPassword())
                .productToSell(login.getProductToSell())
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
        if (provider.getBaseUrl() == null || provider.getBaseUrl().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene baseUrl configurado");
        }
        if (login.getPath() == null || login.getPath().isBlank()) {
            throw new IntegrationException("El proveedor " + provider.getProviderName() + " no tiene login.path configurado");
        }
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

    private AppProperties.ProviderProperties getRequiredProvider(
            String categoryCode,
            String subcategoryCode,
            String serviceProviderCode) {
        return appProperties.getIntegration().getProviders().values().stream()
                .filter(provider -> normalize(provider.getServiceProviderCode()) != null)
                .filter(provider -> normalize(provider.getServiceProviderCode()).equals(normalize(serviceProviderCode)))
                .filter(provider -> matchesContext(provider, categoryCode, subcategoryCode))
                .max(Comparator.comparingInt(this::specificityScore))
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

    private String cacheKey(String categoryCode, String subcategoryCode, String serviceProviderCode) {
        return String.join("|",
                defaultKeyPart(categoryCode),
                defaultKeyPart(subcategoryCode),
                defaultKeyPart(serviceProviderCode));
    }

    private String defaultKeyPart(String value) {
        String normalized = normalize(value);
        return normalized != null ? normalized : "*";
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
