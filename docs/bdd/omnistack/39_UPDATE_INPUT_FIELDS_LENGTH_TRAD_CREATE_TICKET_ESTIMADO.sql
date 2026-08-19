-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   El script 37 agrego los input_fields de CREATE_TICKET para
--   Tradicionales (draw_id, combinacion, figura_id, cantidad_fracciones)
--   sin FIELD_LENGTH, por no tener spec formal del proveedor. Igual
--   criterio que el script 38 (ECUABET): se puebla un ESTIMADO en vez
--   de dejarlo sin restriccion, para que el POS pueda limitar el input
--   del cajero mientras se confirma con Loteria Nacional.
--
--   ESTOS VALORES SON UNA ESTIMACION, NO UNA SPEC CONFIRMADA. Fuente:
--     - draw_id: ejemplo real en CreateTicketRequest.java Schema
--       (example = "7151", 4 digitos) -- se estima con margen.
--     - combinacion: PrecheckResponse.TradicionalDraw expone hasta
--       cantidadDigitosCombinacionPrincipal/Secundaria/Tercera/Cuarta/
--       Quinta -- combinaciones de varios numeros concatenados, se
--       estima con margen sobre 5 combinaciones de hasta 2 digitos c/u.
--     - figura_id: ejemplo real en CreateTicketRequest.java Schema
--       (example = "01", codigo de mascota/fruta de 2 digitos).
--     - cantidad_fracciones (INTEGER -> cantidad de digitos): las
--       fracciones se compran de a pocas unidades, 2 digitos alcanza
--       holgadamente.
--
--   Pendiente: reemplazar por el valor real apenas Loteria Nacional o
--   negocio confirmen el formato oficial de cada campo.
--
-- ITEMS: 100713842 (Loteria), 100713844 (Lotto), 100713846 (Pozo)
-- ============================================================

-- ---- draw_id (los 3 items) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 10,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'draw_id';

-- ---- combinacion (los 3 items) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 10,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'combinacion';

-- ---- figura_id (solo Pozo Millonario) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 2,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100713846'
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'figura_id';

-- ---- cantidad_fracciones (solo Loteria) ----
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 2,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100713842'
   AND CAPABILITY = 'CREATE_TICKET'
   AND FIELD_ID = 'cantidad_fracciones';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_TYPE, FIELD_LENGTH
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
 ORDER BY RMS_ITEM_CODE, FIELD_ORDER;
-- Esperado: draw_id=10 y combinacion=10 en los 3 items; ademas
-- cantidad_fracciones=2 en 100713842 y figura_id=2 en 100713846.
