-- ============================================================
-- Ejecutar como: TUKUNAFUNC (despues de 40/41)
--
-- MOTIVO:
--   Los scripts 40/41 agregaron las columnas REGEX y GROUP_LENGTH,
--   pero ningun input_field existente (ECUABET, BET593, PEGA3, TRAD,
--   CLARO) quedo con esos campos poblados. Este script cierra ese hueco
--   para TODOS los input_fields ya creados en scripts anteriores.
--
--   ESTOS VALORES SON UNA ESTIMACION, NO UNA SPEC CONFIRMADA por cada
--   proveedor (mismo criterio que scripts 38/39). Se basan en formato
--   estandar de Ecuador (cedula=10 digitos, celular=09+8 digitos) y en
--   los ejemplos reales de docs/OmniStack_mock_por_proveedor_v1.json.
--   Pendiente reemplazar por el valor real cuando cada proveedor lo
--   confirme.
--
--   El regex aplica sobre el texto que escribe el cajero, ANTES de
--   convertir segun FIELD_TYPE — por eso aplica igual a STRING/INTEGER/
--   DOUBLE (ver [[project-omnistack-input-field-length-regex]]).
-- ============================================================

-- ---- document (cedula, formato Ecuador: 10 digitos) — todos los providers ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d{10}$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'document';

-- ---- phone (celular Ecuador: 09 + 8 digitos) — todos los providers ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^09\d{8}$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'phone';

-- ---- amount (DOUBLE, hasta 2 decimales) — todos los providers ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d+(\.\d{1,2})?$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'amount';

-- ---- userid (Ecuabet, ejemplo real "997561") ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'userid';

-- ---- password (Ecuabet retiro, sin spec formal -- alfanumerico generico) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^[A-Za-z0-9]+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'password';

-- ---- withdrawId (numerico, ejemplos reales "7583" / "20240430800100007") ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'withdrawId';

-- ---- username (nombre comprador: letras, espacios y tildes) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^[A-Za-zÁÉÍÓÚÑáéíóúñ ]+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'username';

-- ---- draw_id (id numerico de sorteo, CREATE_TICKET Tradicionales) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'draw_id';

-- ---- cantidad_fracciones (INTEGER, solo Loteria) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'cantidad_fracciones';

-- ---- figura_id (codigo de mascota/fruta, 2 digitos, ejemplo real "01") ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^\d{2}$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE FIELD_ID = 'figura_id';

-- ---- authorization Premios Pega (Numero de ticket) -- spec real confirmada:
--      27 caracteres (24 + 3 verificador, ver script 36) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^[A-Za-z0-9]{27}$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100708858', '100708860', '100708862')
   AND FIELD_ID = 'authorization'
   AND CAPABILITY = 'PRECHECK';

-- ---- authorization Premios Tradicionales (Clave del boleto electronico) --
--      sin spec confirmada -- alfanumerico generico ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET REGEX = '^[A-Za-z0-9]+$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100708854', '100708855', '100708856')
   AND FIELD_ID = 'authorization'
   AND CAPABILITY = 'PRECHECK';

-- ---- motivo: SIN regex a proposito -- es texto libre donde el cajero
--      explica el reverso, cualquier patron seria mas restrictivo de lo
--      que el negocio necesita. Se deja NULL deliberadamente. ----

-- ============================================================
-- combinacion (Tradicionales CREATE_TICKET) -- retrofit a GROUP_LENGTH
--
-- El script 39 le puso FIELD_LENGTH=10 bajo la semantica ANTERIOR (campo
-- simple, 10 caracteres totales). Ahora que existe GROUP_LENGTH, pasa a
-- representar el caso de uso real que motivo esta columna: varios
-- numeros de 2 digitos separados por espacio. Se estima 5 grupos maximo
-- (mismo criterio que cantidadDigitosCombinacionPrincipal..Quinta de
-- PrecheckResponse.TradicionalDraw -- hasta 5 combinaciones por sorteo).
-- FIELD_LENGTH cambia de significado: de "10 caracteres" a "5 grupos".
-- ============================================================
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 5,
       GROUP_LENGTH = 2,
       REGEX = '^\d{2}(\s\d{2}){0,4}$',
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_TYPE, FIELD_LENGTH, GROUP_LENGTH, REGEX
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE ENABLED = 'S'
 ORDER BY RMS_ITEM_CODE, CAPABILITY, FIELD_ORDER;
-- Esperado: REGEX poblado en todos los campos salvo 'motivo' (queda NULL
-- a proposito). 'combinacion' es el unico con GROUP_LENGTH no-null.
