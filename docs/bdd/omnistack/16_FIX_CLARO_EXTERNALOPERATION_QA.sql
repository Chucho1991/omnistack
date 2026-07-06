-- ============================================================
-- Ejecutar como: usuario con acceso a GPF_OMNISTACK (rms datasource)
--
-- Los registros de AD_ITEM_SERVICIO para TAG='EXTERNALOPERATION'
-- contenían valores numéricos (132, 136, 150) que corresponden a
-- OFFERIDs, no al tipo de operación que CLARO espera.
--
-- CLARO requiere el string 'RECARGA_DATOS' para todos los
-- productos actuales (paquetes de datos + recarga monto libre).
-- Si en el futuro se agregan pines, usar 'RECARGA_PINES' para esos items.
-- ============================================================

UPDATE gpf_omnistack.AD_ITEM_SERVICIO
   SET VALOR_TAG = 'RECARGA_DATOS'
 WHERE TAG = 'EXTERNALOPERATION'
   AND ACTIVO = 'S';

COMMIT;

-- Verificar:
-- SELECT sp.CODIGO_ITEM_RMS, ai.TAG, ai.VALOR_TAG
--   FROM gpf_omnistack.AD_ITEM_SERVICIO ai
--   JOIN gpf_omnistack.AD_SERVICIO_PARAMETROS sp ON sp.ID_CONFIG = ai.ID_CONFIG
--  WHERE ai.TAG = 'EXTERNALOPERATION'
--  ORDER BY sp.CODIGO_ITEM_RMS;
