-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- CLEANUP: Eliminar category_code y subcategory_code de IN_OMNI_PROVEEDOR_CONFIG
--
-- MOTIVO:
--   Estos valores replicaban datos que ya existen en las tablas RMS
--   (CLASS y SUBCLASS via ITEM_MASTER). El codigo Java ya no los usa
--   para validacion — el ServiceDefinition se resuelve directamente
--   desde el catalogo RMS en DefaultProviderFlowResolver.
--
--   Los campos eliminados son:
--     - category_code:    replicaba CLASS de ITEM_MASTER (RMS)
--     - subcategory_code: replicaba SUBCLASS de ITEM_MASTER (RMS)
--
--   El campo service_provider_code SE MANTIENE porque se usa para:
--     1. Routing en supports() de las strategies (correlaciona PROVEEDOR_KEY con TERCERO de AD_SERVICIO_PARAMETROS)
--     2. Join en capabilities.sql para derivar capabilities por proveedor
--     3. Metadata en ProviderTokenService
--
-- PROVEEDORES AFECTADOS:
--   ecuabet, loteria, pega3, tradicional, claro
-- ============================================================

-- Eliminar category_code de todos los proveedores
DELETE FROM IN_OMNI_PROVEEDOR_CONFIG
 WHERE CONFIG_KEY = 'category_code';

-- Eliminar subcategory_code de todos los proveedores
DELETE FROM IN_OMNI_PROVEEDOR_CONFIG
 WHERE CONFIG_KEY = 'subcategory_code';

COMMIT;

-- ============================================================
-- VERIFICACION: No deben existir registros con estos CONFIG_KEY
-- ============================================================
SELECT PROVEEDOR_KEY, CONFIG_KEY, CONFIG_VALOR
  FROM IN_OMNI_PROVEEDOR_CONFIG
 WHERE CONFIG_KEY IN ('category_code', 'subcategory_code')
 ORDER BY PROVEEDOR_KEY, CONFIG_KEY;
-- Esperado: 0 filas

-- Verificar que service_provider_code sigue presente:
SELECT PROVEEDOR_KEY, CONFIG_KEY, CONFIG_VALOR
  FROM IN_OMNI_PROVEEDOR_CONFIG
 WHERE CONFIG_KEY = 'service_provider_code'
 ORDER BY PROVEEDOR_KEY;
-- Esperado: 1 fila por proveedor (ecuabet, loteria, pega3, tradicional, claro)
