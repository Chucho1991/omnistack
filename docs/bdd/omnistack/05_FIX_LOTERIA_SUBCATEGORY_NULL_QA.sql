-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Fix: subcategory_code de loteria debe ser NULL (wildcard)
-- para que ProviderTokenService.matchesContext() acepte
-- tanto subcategoryCode=1120 (BET593 CI) como 1121 (BET593 CO).
--
-- Cuando providerSubcategoryCode == null → subcategoryMatches = true (wildcard)
-- Cuando providerSubcategoryCode == "1" → subcategoryMatches = ("1" == "1120") = false ← ERROR
--
-- El script 04_UPDATE_PROVEEDOR_CONFIG_QA.sql intento poner '1120'
-- pero eso solo cubre CI. NULL es el valor correcto para cubrir
-- todos los subcategory_codes de BET593.
-- ============================================================

UPDATE IN_OMNI_PROVEEDOR_CONFIG
   SET CONFIG_VALOR = NULL
 WHERE PROVEEDOR_KEY = 'loteria'
   AND CONFIG_KEY    = 'subcategory_code';

COMMIT;

-- Verificar:
-- SELECT PROVEEDOR_KEY, CONFIG_KEY, CONFIG_VALOR
--   FROM IN_OMNI_PROVEEDOR_CONFIG
--  WHERE PROVEEDOR_KEY = 'loteria'
--  ORDER BY CONFIG_KEY;
