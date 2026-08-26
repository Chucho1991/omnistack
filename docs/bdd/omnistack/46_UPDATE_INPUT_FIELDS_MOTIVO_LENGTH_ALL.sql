-- ============================================================
-- Ejecutar como: TRX3 (ambiente APPTEST)
--
-- MOTIVO:
--   Confirmado con el usuario (2026-08-25): todos los input_fields
--   'motivo' (REVERSE, todos los proveedores) deben tener un
--   FIELD_LENGTH (tope de caracteres) para que el POS pueda limitar el
--   textarea, aunque el campo siga siendo texto libre.
--
--   REGEX se deja intacto (NULL) a proposito -- decision ya documentada
--   en el script 42: 'motivo' es texto libre donde el cajero explica el
--   reverso, un patron seria mas restrictivo de lo que el negocio
--   necesita. Este script NO revierte esa decision, solo agrega el
--   largo maximo.
--
--   Valor elegido (200): unico precedente real ya en la tabla
--   (100708846, Ecuabet Premio, ya tenia FIELD_LENGTH=200 desde antes de
--   este script) y sin ninguna restriccion de tamaño documentada del
--   lado proveedor/app (ReverseRequest.motivo solo tiene @NotBlank, sin
--   @Size) que sugiera otro valor. Se homologa el mismo criterio a
--   TODOS los 'motivo' existentes, sin importar el proveedor.
-- ============================================================

UPDATE TRX3.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 200,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'motivo'
   AND (FIELD_LENGTH IS NULL OR FIELD_LENGTH <> 200);

COMMIT;

-- ============================================================
-- VERIFICACION -- esperado: todas las filas con FIELD_LENGTH=200,
-- REGEX y GROUP_LENGTH sin cambios (deben seguir en NULL).
-- ============================================================
SELECT RMS_ITEM_CODE, FIELD_ID, FIELD_LENGTH, REGEX, GROUP_LENGTH
  FROM TRX3.IN_OMNI_INPUT_FIELDS
 WHERE FIELD_ID = 'motivo'
 ORDER BY RMS_ITEM_CODE;
