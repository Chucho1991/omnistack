-- ============================================================
-- SCRIPTS DIAGNOSTICOS para el flujo PRECHECK CASH_OUT
-- rms_item_code = 100708848 (BET593 PREMIO)
-- service_provider_code = 408403
-- category_code = 983, subcategory_code = 1121
--
-- Estos son los queries que la aplicacion ejecuta internamente
-- para resolver la parametrizacion de un item.
-- ============================================================


-- ============================================================
-- 1) CATALOGO: AD_SERVICIO_PARAMETROS + AD_CANAL_SERVICIO
--    (Ejecutar en BD RMS como gpf_lectura)
--    La app lo consulta al cargar el snapshot del catalogo.
--    El item debe existir aqui con ACTIVO='S' para canal_codigo=1 (POS).
-- ============================================================
SELECT
    TO_CHAR(sp.TERCERO)     AS service_provider_code,
    TRIM(sp.CODIGO_ITEM_RMS) AS rms_item_code,
    sp.ID_CONFIG,
    CASE WHEN cs.ACTIVO = 'S' THEN 'SI' ELSE 'NO' END AS activo_en_canal,
    sp.FLG_ITEM,
    sp.MONTO_MIN,
    sp.MONTO_MAX,
    sp.TIMEOUT_WS_MAX,
    sp.RETRIES_WS_MAX
FROM gpf_omnistack.AD_SERVICIO_PARAMETROS sp
LEFT JOIN gpf_omnistack.AD_CANAL_SERVICIO cs
    ON cs.ID_CONFIG    = sp.ID_CONFIG
   AND cs.CODIGO_CANAL = 1  -- POS
WHERE TRIM(sp.CODIGO_ITEM_RMS) = '100708848';


-- ============================================================
-- 2) RMS: ITEM_MASTER — CLASS y SUBCLASS (category/subcategory)
--    (Ejecutar en BD RMS)
--    Determina el category_code y subcategory_code del item.
-- ============================================================
SELECT
    TRIM(im.ITEM)        AS rms_item_code,
    im.ITEM_DESC         AS description,
    TO_CHAR(c.CLASS)     AS category_code,
    c.CLASS_NAME         AS category_name,
    TO_CHAR(sc.SUBCLASS) AS subcategory_code,
    sc.SUB_NAME          AS subcategory_name
FROM ITEM_MASTER im
JOIN CLASS c
    ON c.CLASS = im.CLASS
JOIN SUBCLASS sc
    ON sc.CLASS    = im.CLASS
   AND sc.SUBCLASS = im.SUBCLASS
WHERE TRIM(im.ITEM) = '100708848';


-- ============================================================
-- 3) RMS: ITEM_SUPPLIER — Proveedor del item
--    (Ejecutar en BD RMS)
-- ============================================================
SELECT
    TRIM(isup.ITEM)        AS rms_item_code,
    TO_CHAR(isup.SUPPLIER) AS supplier_code,
    s.SUP_NAME             AS provider_name
FROM ITEM_SUPPLIER isup
JOIN SUPS s ON s.SUPPLIER = isup.SUPPLIER
WHERE TRIM(isup.ITEM) = '100708848';


-- ============================================================
-- 4) IN_OMNI_PROVEEDOR_CONFIG — Configuracion del proveedor 'loteria'
--    (Ejecutar en BD TUKUNAFUNC)
--    Muestra service_provider_code, category_code, subcategory_code
--    mapeados al PROVEEDOR_KEY='loteria'.
-- ============================================================
SELECT PROVEEDOR_KEY, CONFIG_KEY, CONFIG_VALOR
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
 WHERE PROVEEDOR_KEY = 'loteria'
 ORDER BY CONFIG_KEY;


-- ============================================================
-- 5) IN_OMNI_PROVEEDOR_WS — URLs de operaciones del proveedor 'loteria'
--    (Ejecutar en BD TUKUNAFUNC)
--    Verifica que exista URL para PRECHECK.CASHOUT con ENABLED='S'.
-- ============================================================
SELECT ID_WS, PROVEEDOR_KEY, WS_KEY, ENABLED, URL, NOMBRE_OPERACION
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
 WHERE PROVEEDOR_KEY = 'loteria'
   AND WS_KEY LIKE '%.CASHOUT'
 ORDER BY WS_KEY;


-- ============================================================
-- 6) IN_OMNI_PROVEEDOR_WS_DEFS — Definiciones de items por operacion
--    (Ejecutar en BD TUKUNAFUNC)
--    *** AQUI ESTA EL PROBLEMA ***
--    Debe existir una fila con DEFAULT_CLAVE = 'item.100708848'
--    (o DEFAULT_CLAVE = 'item' con DEFAULT_VALOR_TEXT = '100708848')
--    para el ID_WS de PRECHECK.CASHOUT del proveedor 'loteria'.
-- ============================================================
SELECT w.PROVEEDOR_KEY, w.WS_KEY, w.ENABLED,
       d.DEFAULT_CLAVE, d.DEFAULT_VALOR_TEXT, d.DEFAULT_VALOR_NUM, d.TIPO_DEF
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
  JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d ON d.ID_WS = w.ID_WS
 WHERE w.PROVEEDOR_KEY = 'loteria'
   AND w.WS_KEY LIKE '%.CASHOUT'
 ORDER BY w.WS_KEY, d.DEFAULT_CLAVE;


-- ============================================================
-- 7) IN_OMNI_PROVEEDOR_WS — Capabilities habilitadas
--    (Ejecutar en BD TUKUNAFUNC)
--    La app usa este query para construir el mapa de capabilities
--    por service_provider_code. Verifica que exista PRECHECK para 408403.
-- ============================================================
SELECT DISTINCT
    cfg.CONFIG_VALOR                    AS service_provider_code,
    REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+') AS capability_code
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG cfg
JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS ws
    ON ws.PROVEEDOR_KEY = cfg.PROVEEDOR_KEY
   AND ws.ENABLED       = 'S'
WHERE cfg.CONFIG_KEY = 'service_provider_code'
  AND cfg.CONFIG_VALOR = '408403'
  AND REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')
      IN ('PRECHECK', 'CREATE_TICKET', 'EXECUTE', 'VERIFY', 'REVERSE');


-- ============================================================
-- 8) IN_OMNI_PROVEEDOR_WS_DEFS — Movement Type del item
--    (Ejecutar en BD TUKUNAFUNC)
--    Determina si el item es CASH_OUT (WS_KEY termina en .CASHOUT).
-- ============================================================
SELECT DISTINCT
    d.DEFAULT_VALOR_TEXT         AS rms_item_code,
    CASE
        WHEN UPPER(ws.WS_KEY) LIKE '%.CASHOUT' THEN 'CASH_OUT'
        ELSE 'CASH_IN'
    END AS movement_type
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS ws
    ON ws.ID_WS   = d.ID_WS
   AND ws.ENABLED = 'S'
WHERE d.TIPO_DEF = 'CONFIG'
  AND (d.DEFAULT_CLAVE = 'item' OR d.DEFAULT_CLAVE LIKE 'item.%')
  AND d.DEFAULT_VALOR_TEXT = '100708848';


-- ============================================================
-- 9) IN_OMNI_INPUT_FIELDS — Campos de entrada para el item
--    (Ejecutar en BD TUKUNAFUNC)
-- ============================================================
SELECT FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, FIELD_ORDER
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE = '100708848'
   AND ENABLED = 'S'
 ORDER BY CAPABILITY, FIELD_ORDER;


-- ============================================================
-- 10) RESUMEN: Cache key que construye la app para hasConfiguredOperation
--     (Ejecutar en BD TUKUNAFUNC)
--     Simula la logica Java: busca "loteria|PRECHECK.CASHOUT|item.100708848"
--     Si no retorna filas, hasConfiguredOperation() devuelve false.
-- ============================================================
SELECT
    LOWER(w.PROVEEDOR_KEY) || '|' || UPPER(w.WS_KEY) || '|' || LOWER(d.DEFAULT_CLAVE) AS cache_key,
    COALESCE(d.DEFAULT_VALOR_TEXT, TO_CHAR(d.DEFAULT_VALOR_NUM)) AS valor
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d ON d.ID_WS = w.ID_WS
WHERE w.ENABLED = 'S'
  AND w.PROVEEDOR_KEY = 'loteria'
  AND w.WS_KEY = 'PRECHECK.CASHOUT'
  AND (d.DEFAULT_CLAVE = 'item' OR d.DEFAULT_CLAVE = 'item.100708848');
