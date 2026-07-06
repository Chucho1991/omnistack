-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Corrige CATEGORY_CODE / SUBCATEGORY_CODE / SERVICE_PROVIDER_CODE
-- en IN_OMNI_INPUT_FIELDS para los items de CLARO.
--
-- Problema: los registros tienen los valores "lógicos" del script 09
-- (category='5', subcategory='9', svc_provider='7') que no coinciden
-- con los valores reales de AD_/RMS que usa el adaptador para
-- construir el ServiceKey:
--   TO_CHAR(c.CLASS)    = '8050'
--   TO_CHAR(sc.SUBCLASS_ID) = '7684'
--   TO_CHAR(sp.TERCERO) = '407925'
--
-- El adaptador agrupa input_fields por ServiceKey(cat, subcat, svc, item)
-- y hace lookup con el ServiceKey del ServiceRow. Si no coinciden,
-- el input_field queda huérfano aunque esté en BD con ENABLED='S'.
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
SET CATEGORY_CODE        = '8050',
    SUBCATEGORY_CODE     = '7684',
    SERVICE_PROVIDER_CODE = '407925',
    USR_MODIFICACION     = USER,
    FEC_MODIFICACION     = SYSDATE
WHERE RMS_ITEM_CODE IN (
    '291843',
    '100733842','100733843','100733844','100733845','100733846','100733847',
    '100733848','100733849','100733850','100733851','100733852','100733853','100733854'
)
AND CATEGORY_CODE        = '5'
AND SUBCATEGORY_CODE     = '9'
AND SERVICE_PROVIDER_CODE = '7';

COMMIT;

-- Verificar (esperado: 15 filas con los valores corregidos):
-- SELECT RMS_ITEM_CODE, CATEGORY_CODE, SUBCATEGORY_CODE,
--        SERVICE_PROVIDER_CODE, FIELD_ID, FIELD_ORDER
--   FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
--  WHERE RMS_ITEM_CODE IN (
--        '291843',
--        '100733842','100733843','100733844','100733845','100733846','100733847',
--        '100733848','100733849','100733850','100733851','100733852','100733853','100733854'
--  )
--  ORDER BY RMS_ITEM_CODE, FIELD_ORDER;
