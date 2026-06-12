-- Capabilities derivadas de IN_OMNI_PROVEEDOR_WS filtradas por WS_KEY valido.
-- Correlacion solo por service_provider_code — el join por subcategory_code fue removido
-- porque IN_OMNI_PROVEEDOR_CONFIG tiene UNIQUE(PROVEEDOR_KEY, CONFIG_KEY) y los
-- SUBCLASS_ID reales de Oracle Retail no coinciden con los codigos logicos del config.
SELECT DISTINCT
    TO_CHAR(c.CLASS)        AS category_code,
    TO_CHAR(sc.SUBCLASS_ID) AS subcategory_code,
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    sp.CODIGO_ITEM_RMS      AS rms_item_code,
    REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+') AS capability_code
FROM AD_SERVICIO_PARAMETROS sp
JOIN AD_CANAL_SERVICIO cs
    ON cs.ID_CONFIG     = sp.ID_CONFIG
    AND cs.CODIGO_CANAL = :canal_codigo
    AND cs.ACTIVO       = 'S'
JOIN ITEM_MASTER im ON TRIM(im.ITEM) = TRIM(sp.CODIGO_ITEM_RMS)
JOIN CLASS       c  ON c.CLASS = im.CLASS
JOIN SUBCLASS    sc ON sc.CLASS = im.CLASS AND sc.SUBCLASS_ID = im.SUBCLASS
JOIN IN_OMNI_PROVEEDOR_CONFIG cfg_spc
    ON cfg_spc.CONFIG_KEY   = 'service_provider_code'
    AND cfg_spc.CONFIG_VALOR = TO_CHAR(sp.TERCERO)
JOIN IN_OMNI_PROVEEDOR_WS ws
    ON ws.PROVEEDOR_KEY = cfg_spc.PROVEEDOR_KEY
    AND ws.ENABLED      = 'S'
WHERE REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')
      IN ('PRECHECK', 'CREATE_TICKET', 'EXECUTE', 'VERIFY', 'REVERSE')
ORDER BY c.CLASS, sc.SUBCLASS_ID, sp.TERCERO, sp.CODIGO_ITEM_RMS
