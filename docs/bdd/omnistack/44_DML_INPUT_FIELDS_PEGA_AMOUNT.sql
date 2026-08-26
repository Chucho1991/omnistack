-- ============================================================
-- Ejecutar como: TRX3 (ambiente APPTEST, ver 04_UPDATE_PROVEEDOR_CONFIG_QA.sql
-- actualizado en el commit 4c592c0 -- reemplaza a TUKUNAFUNC@PRS6)
--
-- MOTIVO:
--   Pega3/Pega4/Pega2 (CASH_IN) solo tenian input_fields para REVERSE
--   ('motivo', ver script 09). El resto de capabilities (PRECHECK/EXECUTE/
--   VERIFY) se dejaron sin campos a proposito en su momento, bajo la
--   asuncion de que el POS arma ticket_data completo sin necesitar
--   metadata de OmniStack.
--
--   Confirmado con el usuario (2026-08-24) que el POS SI necesita ver
--   'amount' como campo esperado en EXECUTE. A diferencia de BET593/
--   Ecuabet (amount en PRECHECK), Pega3 no valida amount en PRECHECK
--   (ese paso solo devuelve catalogo de juego + sorteo activo, sin
--   input del cajero) -- amount se envia junto con ticket_data recien
--   en EXECUTE (ver ExecuteRequest.java:32, campo top-level, validado
--   por TransactionAmountValidationService y por
--   LoteriaPega3ExecuteStrategy segun docs/CONTEXTO_SESION_2026-07.md
--   seccion 9.4/11.5).
--
--   NOTA -- NO se agrega 'draw_number': ese campo viaja dentro de
--   ticket_data y lo reenvia el POS desde el active_draw que devolvio
--   el PRECHECK (ObtieneSorteosActivo) -- no lo digita el cajero. Mismo
--   patron que 'authorization'/'reserva_id', explicitamente excluidos
--   de input_fields por la regla del script 09. Si en el futuro se
--   necesita exponer la jugada (numeros/tipo de jugada dentro de
--   panels[]), eso va en un script aparte una vez se defina el mapeo
--   exacto campo-a-campo con negocio.
--
-- rms_item_codes (category=984, service_provider=3445, ver script 20):
--   100708852 (Pega3) — subcategory=1127
--   100713848 (Pega4) — subcategory=1125
--   100713850 (Pega2) — subcategory=1126
--
-- ⚠ SEQ_IN_OMNI_INPUT_FIELDS ROTA EN TRX3@APPTEST (encontrado al ejecutar
--   este script, 2026-08-24): LAST_NUMBER=2 mientras MAX(ID_FIELD) real de
--   la tabla migrada es 60030 -- la secuencia nunca se resincronizo tras
--   copiar los datos de TUKUNAFUNC@PRS6 a este esquema nuevo (mismo tipo de
--   riesgo ya documentado en CONTEXTO_SESION_2026-07.md seccion 10.3, pero
--   ahi aplicaba a GPF_OMNISTACK). SEQ.NEXTVAL revienta con ORA-00001 hasta
--   que alguien con acceso DBA corra:
--     ALTER SEQUENCE TRX3.SEQ_IN_OMNI_INPUT_FIELDS INCREMENT BY 60050;
--     SELECT TRX3.SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL FROM DUAL;
--     ALTER SEQUENCE TRX3.SEQ_IN_OMNI_INPUT_FIELDS INCREMENT BY 1;
--   Mientras tanto, este script usa ID_FIELD explicito (60031-60033, por
--   encima del MAX real) en vez de NEXTVAL -- excepcion puntual al
--   patron de secuencias del proyecto, justificada por la secuencia rota,
--   NO replicar este patron en scripts futuros una vez se corrija.
-- ============================================================

-- Pega3 (100708852) — amount en EXECUTE
INSERT INTO TRX3.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER,
     FIELD_LENGTH, REGEX)
VALUES (60031,
    '984', '1127', '3445', '100708852',
    'amount', 'Monto jugada', 'DOUBLE', 'EXECUTE', 1, 'AMOUNT', NULL, 1,
    NULL, '^\d+(\.\d{1,2})?$');

-- Pega4 (100713848) — amount en EXECUTE
INSERT INTO TRX3.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER,
     FIELD_LENGTH, REGEX)
VALUES (60032,
    '984', '1125', '3445', '100713848',
    'amount', 'Monto jugada', 'DOUBLE', 'EXECUTE', 1, 'AMOUNT', NULL, 1,
    NULL, '^\d+(\.\d{1,2})?$');

-- Pega2 (100713850) — amount en EXECUTE
INSERT INTO TRX3.IN_OMNI_INPUT_FIELDS
    (ID_FIELD, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE, RMS_ITEM_CODE,
     FIELD_ID, LABEL, FIELD_TYPE, CAPABILITY, IS_REQUIRED, FIELD_GROUP, CONDITIONAL_OPERATOR, FIELD_ORDER,
     FIELD_LENGTH, REGEX)
VALUES (60033,
    '984', '1126', '3445', '100713850',
    'amount', 'Monto jugada', 'DOUBLE', 'EXECUTE', 1, 'AMOUNT', NULL, 1,
    NULL, '^\d+(\.\d{1,2})?$');

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, CATEGORY_CODE, SUBCATEGORY_CODE, SERVICE_PROVIDER_CODE,
       CAPABILITY, FIELD_ORDER, FIELD_ID, LABEL, IS_REQUIRED, FIELD_GROUP, REGEX
  FROM TRX3.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100708852','100713848','100713850')
 ORDER BY RMS_ITEM_CODE, CAPABILITY, FIELD_ORDER;
-- Esperado: 6 filas por item... 3 items x 2 filas c/u (amount EXECUTE + motivo REVERSE)
