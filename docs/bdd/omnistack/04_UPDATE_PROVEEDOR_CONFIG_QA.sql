-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Actualiza IN_OMNI_PROVEEDOR_CONFIG con los códigos reales de QA.
-- Los valores de service_provider_code, category_code y subcategory_code
-- deben coincidir con los datos de AD_SERVICIO_PARAMETROS (TERCERO, CLASS, SUBCLASS)
-- del schema gpf_omnistack en QA.
--
-- Valores QA confirmados (2026-06-12):
--   ECUABET  TERCERO=12661912  CLASS=983  SUBCLASS=1118(CI)/1119(CO)
--   LOTERIA  TERCERO=408403    CLASS=983  SUBCLASS=1120(CI)/1121(CO)
--
-- NOTA: subcategory_code no es validado por las strategies de BET593
-- (el routing por 4 campos en el catálogo ya lo garantiza).
-- ============================================================

-- ECUABET
UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = '12661912'
 WHERE PROVEEDOR_KEY = 'ecuabet'
   AND CONFIG_KEY    = 'service_provider_code';

UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = '983'
 WHERE PROVEEDOR_KEY = 'ecuabet'
   AND CONFIG_KEY    = 'category_code';

-- LOTERIA (BET593, Pega3, Tradicionales comparten PROVEEDOR_KEY='loteria'
--          para login de sesión, pero BET593 usa su propio service_provider_code)
UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = '408403'
 WHERE PROVEEDOR_KEY = 'loteria'
   AND CONFIG_KEY    = 'service_provider_code';

UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = '983'
 WHERE PROVEEDOR_KEY = 'loteria'
   AND CONFIG_KEY    = 'category_code';

UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = '1120'
 WHERE PROVEEDOR_KEY = 'loteria'
   AND CONFIG_KEY    = 'subcategory_code';

COMMIT;

-- Verificar resultado:
-- SELECT PROVEEDOR_KEY, CONFIG_KEY, CONFIG_VALOR
--   FROM IN_OMNI_PROVEEDOR_CONFIG
--  WHERE CONFIG_KEY IN ('service_provider_code','category_code','subcategory_code')
--  ORDER BY PROVEEDOR_KEY, CONFIG_KEY;
