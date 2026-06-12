SELECT DISTINCT
    cfg.CONFIG_VALOR                    AS service_provider_code,
    REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+') AS capability_code
FROM IN_OMNI_PROVEEDOR_CONFIG cfg
JOIN IN_OMNI_PROVEEDOR_WS ws
    ON ws.PROVEEDOR_KEY = cfg.PROVEEDOR_KEY
   AND ws.ENABLED       = 'S'
WHERE cfg.CONFIG_KEY = 'service_provider_code'
  AND REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')
      IN ('PRECHECK', 'CREATE_TICKET', 'EXECUTE', 'VERIFY', 'REVERSE')
