-- Capabilities por service_provider_code derivadas de IN_OMNI_PROVEEDOR_CONFIG
-- + IN_OMNI_PROVEEDOR_WS. Cubre proveedores que NO usan item.* en WS_DEFS
-- (ej: claro) y cuyos items se obtienen de AD_SERVICIO_PARAMETROS.
-- El adapter Java usa este resultado como fallback para items sin capabilities.
SELECT DISTINCT
    cfg.CONFIG_VALOR                       AS service_provider_code,
    REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')    AS capability_code
FROM IN_OMNI_PROVEEDOR_CONFIG cfg
JOIN IN_OMNI_PROVEEDOR_WS ws
    ON ws.PROVEEDOR_KEY = cfg.PROVEEDOR_KEY
   AND ws.ENABLED       = 'S'
WHERE cfg.CONFIG_KEY = 'service_provider_code'
  AND REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')
      IN ('PRECHECK', 'CREATE_TICKET', 'EXECUTE', 'VERIFY', 'REVERSE')
ORDER BY service_provider_code, capability_code
