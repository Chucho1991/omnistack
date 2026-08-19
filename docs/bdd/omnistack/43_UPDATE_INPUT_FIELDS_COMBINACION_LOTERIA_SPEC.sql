-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   Correo "RE: Requerimiento - Loteria - Parametrizacion" (Paulo
--   Mendoza, 2026-08-04) corrige la estimacion que el script 42 habia
--   hecho para "combinacion" de CREATE_TICKET Tradicionales -- el
--   script 42 asumio el MISMO formato agrupado (grupos de 2 digitos
--   con espacio) para los 3 juegos. La spec real de Loteria dice que
--   NO es igual para los 3:
--
--   Loteria y Lotto: SIN agrupamiento -- un solo string de digitos
--     concatenados, sin espacios. length = cantidad exacta de digitos.
--     Ejemplo Loteria (length=5): "12345"
--     Ejemplo Lotto   (length=6): "123456"
--
--   Pozo Millonario: SI mantiene agrupamiento -- length=4 grupos de
--     group_length=2 digitos cada uno (4 numeros de la grilla).
--     Ejemplo: "22 22 22 01"
--
--   REGLA NUEVA (no documentada hasta ahora): cuando GROUP_LENGTH no es
--   null, REGEX deja de validar el string completo y pasa a validar
--   CADA GRUPO por separado. Por eso el regex de Pozo
--   "^(?:0[1-9]|1[0-9]|2[0-5])$" es el rango valido de UN numero
--   (01-25, con cero a la izquierda si es de 1 digito), no de los 4
--   numeros juntos. Loteria/Lotto, al no tener grupos, siguen la regla
--   original: REGEX valida el string completo.
-- ============================================================

-- ---- Loteria (100713842): sin agrupamiento, string plano de 5 digitos ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 5,
       GROUP_LENGTH = NULL,
       REGEX = '^[0-9]+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100713842'
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion';

-- ---- Lotto (100713844): sin agrupamiento, string plano de 6 digitos ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 6,
       GROUP_LENGTH = NULL,
       REGEX = '^[0-9]+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100713844'
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion';

-- ---- Pozo (100713846): agrupado, 4 grupos de 2 digitos (01-25 c/u) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 4,
       GROUP_LENGTH = 2,
       REGEX = '^(?:0[1-9]|1[0-9]|2[0-5])$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100713846'
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, FIELD_ID, FIELD_LENGTH, GROUP_LENGTH, REGEX
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion'
 ORDER BY RMS_ITEM_CODE;
-- Esperado: Loteria (5/null/^[0-9]+$), Lotto (6/null/^[0-9]+$),
-- Pozo (4/2/^(?:0[1-9]|1[0-9]|2[0-5])$)
