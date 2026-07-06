-- ============================================================
-- 19_UPDATE_PEGA3_TRAD_CONFIG_QA.sql
-- Actualiza PROVEEDOR_CONFIG de pega3 y tradicional con valores reales de QA.
--
-- QA values (fuente: AD_SERVICIO_PARAMETROS + ITEM_MASTER):
--   Items Pega3    (100708852/100713848/100713850): TERCERO=3445, CLASS=984, SUBCLASS=1127/1125/1126
--   Items Trad.    (100713842/100713844/100713846): TERCERO=3445, CLASS=984, SUBCLASS=1122/1123/1124
--
-- Cada item tiene un SUBCLASS distinto → no se puede fijar un único subcategory_code.
-- subcategory_code=NULL actúa como wildcard; el routing Pega3 vs Tradicional
-- lo resuelve hasConfiguredOperation() por rmsItemCode en IN_OMNI_PROVEEDOR_WS_DEFS.
-- Las strategies fueron actualizadas para no validar subcategory_code (paridad con BET593).
-- ============================================================

-- pega3
UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = '3445'
    WHERE PROVEEDOR_KEY = 'pega3' AND CONFIG_KEY = 'service_provider_code';

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = '984'
    WHERE PROVEEDOR_KEY = 'pega3' AND CONFIG_KEY = 'category_code';

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = NULL
    WHERE PROVEEDOR_KEY = 'pega3' AND CONFIG_KEY = 'subcategory_code';

-- tradicional
UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = '3445'
    WHERE PROVEEDOR_KEY = 'tradicional' AND CONFIG_KEY = 'service_provider_code';

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = '984'
    WHERE PROVEEDOR_KEY = 'tradicional' AND CONFIG_KEY = 'category_code';

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
    SET CONFIG_VALOR = NULL
    WHERE PROVEEDOR_KEY = 'tradicional' AND CONFIG_KEY = 'subcategory_code';

COMMIT;
