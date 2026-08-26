-- ============================================================
-- Ejecutar como: TRX3 (ambiente APPTEST)
--
-- MOTIVO:
--   El script 44 agrego 'amount' (DOUBLE) en EXECUTE para Pega3/4/2 pero
--   dejo FIELD_LENGTH en NULL -- se paso por alto la convencion ya
--   establecida en el script 36: para FIELD_TYPE='DOUBLE', FIELD_LENGTH
--   representa CANTIDAD DE DECIMALES (no caracteres), y BET593 ya usa
--   FIELD_LENGTH=2 para su propio 'amount' (fuente: spec Loteria,
--   "valor... con dos decimales... ej 1000.00, 1250.51"). Es la misma
--   regla generica de CLAUDE.md ("Todos los amounts: BigDecimal con 2
--   decimales, separador punto") -- no requiere confirmacion de negocio,
--   solo homologar lo que ya existe.
-- ============================================================

UPDATE TRX3.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 2,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100708852', '100713848', '100713850')
   AND FIELD_ID = 'amount'
   AND FIELD_TYPE = 'DOUBLE';

COMMIT;

-- ============================================================
-- VERIFICACION -- esperado: 3 filas con FIELD_LENGTH=2
-- ============================================================
SELECT RMS_ITEM_CODE, FIELD_ID, FIELD_TYPE, CAPABILITY, FIELD_LENGTH, REGEX
  FROM TRX3.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100708852', '100713848', '100713850')
   AND FIELD_ID = 'amount'
 ORDER BY RMS_ITEM_CODE;
