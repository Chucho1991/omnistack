-- ============================================================
-- Ejecutar como: usuario con acceso a GPF_OMNISTACK (rms datasource)
--                + TUKUNAFUNC (prod datasource)
--
-- OFFERID y EXTERNALOPERATION son valores por item (rms_item_code),
-- no valores fijos por operacion. Este script:
--   1. Agrega TAG='OFFERID' en AD_ITEM_SERVICIO para cada item CLARO
--   2. Corrige TAG='EXTERNALOPERATION' de numeros erroneos a strings correctos
--   3. Elimina los offer_id.* de IN_OMNI_PROVEEDOR_WS_DEFS (estaban en lugar incorrecto)
-- ============================================================

-- ---- 1. OFFERID en AD_ITEM_SERVICIO -------------------------
-- 291843: Recarga generica monto libre → OFFERID=132 (Recarga Datos valor entero)
INSERT INTO gpf_omnistack.AD_ITEM_SERVICIO (ID_CONFIG, TAG, VALOR_TAG, ACTIVO)
SELECT ID_CONFIG, 'OFFERID', '132', 'S'
  FROM gpf_omnistack.AD_SERVICIO_PARAMETROS
 WHERE TRIM(CODIGO_ITEM_RMS) = '291843';

-- 100733842..100733854: Paquetes fijos → OFFERID=150 (Recarga Datos Fraccionario)
INSERT INTO gpf_omnistack.AD_ITEM_SERVICIO (ID_CONFIG, TAG, VALOR_TAG, ACTIVO)
SELECT ID_CONFIG, 'OFFERID', '150', 'S'
  FROM gpf_omnistack.AD_SERVICIO_PARAMETROS
 WHERE TRIM(CODIGO_ITEM_RMS) IN (
   '100733842','100733843','100733844','100733845','100733846','100733847',
   '100733848','100733849','100733850','100733851','100733852','100733853','100733854'
 );

-- ---- 2. EXTERNALOPERATION: corregir de numeros a strings ----
UPDATE gpf_omnistack.AD_ITEM_SERVICIO
   SET VALOR_TAG = 'RECARGA_DATOS'
 WHERE TAG = 'EXTERNALOPERATION'
   AND ACTIVO = 'S';

-- ---- 3. Eliminar offer_id.* de IN_OMNI_PROVEEDOR_WS_DEFS ---
DELETE FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
 WHERE d.ID_WS IN (
   SELECT w.ID_WS FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
    WHERE w.PROVEEDOR_KEY = 'claro'
 )
   AND d.DEFAULT_CLAVE LIKE 'offer_id.%';

COMMIT;

-- Verificar:
-- SELECT sp.CODIGO_ITEM_RMS, ai.TAG, ai.VALOR_TAG
--   FROM gpf_omnistack.AD_ITEM_SERVICIO ai
--   JOIN gpf_omnistack.AD_SERVICIO_PARAMETROS sp ON sp.ID_CONFIG = ai.ID_CONFIG
--  WHERE ai.TAG IN ('OFFERID','EXTERNALOPERATION')
--  ORDER BY sp.CODIGO_ITEM_RMS, ai.TAG;
--
-- SELECT DEFAULT_CLAVE FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
--   JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w ON w.ID_WS = d.ID_WS
--  WHERE w.PROVEEDOR_KEY = 'claro' AND d.DEFAULT_CLAVE LIKE 'offer_id.%';
-- → debe retornar 0 filas
