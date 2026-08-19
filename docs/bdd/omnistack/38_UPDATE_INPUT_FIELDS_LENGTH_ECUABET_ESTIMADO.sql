-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   El script 36 dejo FIELD_LENGTH=null para password/withdrawId/motivo
--   de ECUABET por falta de spec formal del proveedor. Negocio confirmo
--   que no hay definicion oficial, pero pidio poblar un ESTIMADO en vez
--   de dejarlo sin restriccion, para que el POS pueda al menos limitar
--   el input del cajero mientras se confirma con Ecuabet.
--
--   ESTOS VALORES SON UNA ESTIMACION, NO UNA SPEC CONFIRMADA. Fuente:
--     - amount: regla global de OmniStack para montos (docs/CLAUDE.md
--       seccion "Key conventions": "todos los amounts... dos decimales"),
--       mismo valor ya usado para BET593 amount en el script 36.
--     - password / withdrawId: ejemplos reales de retiro ECUABET en
--       docs/OmniStack_mock_por_proveedor_v1.json (withdrawId="7583",
--       password="33367", y "20240430800100007" para BET593 CO como
--       referencia de longitud maxima observada). Se estima con margen
--       sobre el mayor valor observado.
--     - motivo: convencion general OmniStack de campos de texto libre
--       string(200) (docs/CLAUDE.md seccion 3, campos error.message/
--       status.message). Es un campo propio de OmniStack -- Ecuabet no
--       recibe motivo en su contrato (ver docs/CLAUDE.md seccion 5.5).
--
--   Pendiente: reemplazar por el valor real apenas Ecuabet o negocio
--   confirmen el formato oficial de password/withdrawId.
--
-- ITEM: ECUABET CASH_OUT (100708846) -- category=983, subcategory=1119,
--   service_provider=11966043 (ver script 30, PASO 1)
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 2,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100708846'
   AND FIELD_ID = 'amount'
   AND FIELD_TYPE = 'DOUBLE';

UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 20,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100708846'
   AND FIELD_ID = 'password'
   AND FIELD_TYPE = 'STRING';

UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 20,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100708846'
   AND FIELD_ID = 'withdrawId'
   AND FIELD_TYPE = 'STRING';

UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 200,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE = '100708846'
   AND FIELD_ID = 'motivo'
   AND FIELD_TYPE = 'STRING';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_TYPE, FIELD_LENGTH
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE = '100708846'
 ORDER BY CAPABILITY, FIELD_ORDER;
-- Esperado: amount=2, password=20, withdrawId=20, motivo=200
