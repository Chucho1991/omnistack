-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   TradicionalCreateTicketStrategy (agregada en los scripts 22/23, despues
--   del script 09 que poblo input_fields por primera vez) exige "draw_id"
--   (lanza IntegrationException si viene vacio) y acepta "combinacion",
--   "figura_id" y "cantidad_fracciones" — campos planos que dependen de una
--   decision real del cajero/cliente (que sorteo jugar, que numeros, que
--   mascota, cuantas fracciones). Sin embargo CREATE_TICKET nunca tuvo filas
--   en IN_OMNI_INPUT_FIELDS: el front no sabe que debe pedirlos.
--
--   "sugerir" y "registros" NO se incluyen como input_fields porque no
--   dependen del cajero/cliente para que el paso funcione — la strategy ya
--   los resuelve con un default en codigo. Este script los saca del codigo
--   y los deja parametrizados en IN_OMNI_PROVEEDOR_WS_DEFS (mismo patron que
--   medio_id/cliente_id), en vez de un default fijo en Java:
--     sugerir  = 'true'  (siempre pedir sugerencias si no hay match exacto)
--     registros = '10'   (cantidad de resultados que retorna el proveedor)
--
-- rms_item_codes (valores YA corregidos de catalogo QA, ver script 30):
--   100713842 (Loteria) — category=984, subcategory=1122, service_provider=3445
--   100713844 (Lotto)   — category=984, subcategory=1123, service_provider=3445
--   100713846 (Pozo)    — category=984, subcategory=1124, service_provider=3445
--
-- CAMPOS POR ITEM (fuente: CreateTicketRequest.java + TradicionalCreateTicketStrategy):
--   Loteria (100713842): draw_id (req), combinacion (opt), cantidad_fracciones (opt)
--     — cantidad_fracciones es exclusivo de Loteria (no aplica a Lotto/Pozo, ver
--       Javadoc de CreateTicketRequest.cantidadFracciones).
--   Lotto   (100713844): draw_id (req), combinacion (opt)
--   Pozo    (100713846): draw_id (req), combinacion (opt), figura_id (opt)
--     — figura_id (mascota) es exclusivo de Pozo Millonario (RN-05/RF-06).
-- ============================================================

-- ============================================================
-- PASO 1: Parametrizar sugerir/registros en IN_OMNI_PROVEEDOR_WS_DEFS
--   (constantes internas del proveedor, no dependen del cajero/cliente)
-- ============================================================
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'sugerir', 'true'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY = 'CREATE_TICKET.CASHIN';

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'registros', '10'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY = 'CREATE_TICKET.CASHIN';

-- ============================================================
-- PASO 2: input_fields de CREATE_TICKET — Loteria (100713842)
-- ============================================================
INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1122', '3445', '100713842',
    'draw_id', 'Sorteo', 'STRING', 'CREATE_TICKET', 1, 'DETAIL', NULL, 1);

INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1122', '3445', '100713842',
    'combinacion', 'Combinacion de numeros', 'STRING', 'CREATE_TICKET', 0, 'DETAIL', NULL, 2);

INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1122', '3445', '100713842',
    'cantidad_fracciones', 'Cantidad de fracciones', 'INTEGER', 'CREATE_TICKET', 0, 'DETAIL', NULL, 3);

-- ============================================================
-- PASO 3: input_fields de CREATE_TICKET — Lotto (100713844)
-- ============================================================
INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1123', '3445', '100713844',
    'draw_id', 'Sorteo', 'STRING', 'CREATE_TICKET', 1, 'DETAIL', NULL, 1);

INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1123', '3445', '100713844',
    'combinacion', 'Combinacion de numeros', 'STRING', 'CREATE_TICKET', 0, 'DETAIL', NULL, 2);

-- ============================================================
-- PASO 4: input_fields de CREATE_TICKET — Pozo Millonario (100713846)
-- ============================================================
INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1124', '3445', '100713846',
    'draw_id', 'Sorteo', 'STRING', 'CREATE_TICKET', 1, 'DETAIL', NULL, 1);

INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1124', '3445', '100713846',
    'combinacion', 'Combinacion de numeros', 'STRING', 'CREATE_TICKET', 0, 'DETAIL', NULL, 2);

INSERT INTO TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER)
VALUES (SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL,
    '984', '1124', '3445', '100713846',
    'figura_id', 'Mascota/fruta', 'STRING', 'CREATE_TICKET', 0, 'DETAIL', NULL, 3);

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================

-- 1. WS_DEFS: sugerir/registros parametrizados para CREATE_TICKET.CASHIN
SELECT w.PROVEEDOR_KEY, w.WS_KEY, d.DEFAULT_CLAVE, d.DEFAULT_VALOR_TEXT
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
  JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w ON w.ID_WS = d.ID_WS
 WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY = 'CREATE_TICKET.CASHIN'
   AND d.DEFAULT_CLAVE IN ('sugerir', 'registros');
-- Esperado: 2 filas (sugerir=true, registros=10)

-- 2. input_fields de CREATE_TICKET (esperado: 8 filas — 3+2+3)
SELECT RMS_ITEM_CODE, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE,
       CAPABILITY, FIELD_ORDER, FIELD_ID, LABEL, IS_REQUIRED, FIELD_GROUP
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100713842', '100713844', '100713846')
   AND CAPABILITY = 'CREATE_TICKET'
 ORDER BY RMS_ITEM_CODE, FIELD_ORDER;
