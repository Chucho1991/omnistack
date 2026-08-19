-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   Poblar FIELD_LENGTH (agregada en script 34) solo para los campos
--   donde la documentacion del proveedor (Loteria Nacional / Claro)
--   respalda explicitamente un valor. No se asignan valores a campos
--   sin respaldo documental (document/cedula, username, password,
--   withdrawId, motivo, authorization de Premios Tradicionales) —
--   quedan pendientes de confirmar con el proveedor o con negocio.
--
--   FIELD_TYPE define el significado de FIELD_LENGTH:
--     STRING  -> cantidad maxima de caracteres
--     DOUBLE  -> cantidad de decimales
-- ============================================================

-- ---- 1. BET593 "amount" (DOUBLE) -> 2 decimales ----
-- Fuente: spec Loteria, endpoint RecargarBet593: "valor... con dos
-- decimales (separador de decimales punto)... por ejemplo 1000.00, 1250.51"
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 2,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE SERVICE_PROVIDER_CODE = '408403'
   AND FIELD_ID = 'amount'
   AND FIELD_TYPE = 'DOUBLE';

-- ---- 2. Claro "phone" (STRING) -> 10 caracteres ----
-- Fuente: spec Claro, SUBSCRIBERID = 12 digitos con codigo de pais
-- ("593993154323"), pero ClaroXmlAdapter.resolveSubscriberId() le
-- quita el "0" inicial y antepone "593" al numero que digita el
-- cajero -> el cajero escribe el formato local de 10 digitos.
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 10,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE SERVICE_PROVIDER_CODE = '407925'
   AND FIELD_ID = 'phone'
   AND FIELD_TYPE = 'STRING';

-- ---- 3. Premios Pega "authorization"/Numero de ticket (STRING) -> 27 caracteres ----
-- Fuente: spec Pega, endpoint ConsultarTicket/CancelarTicket: "ticketNumber...
-- de longitud 24 caracteres sin considerar los 3 ultimos digitos
-- (codigo verificador)" -> 24 + 3 = 27
UPDATE TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
   SET FIELD_LENGTH = 27,
       USR_MODIFICACION = USER,
       FEC_MODIFICACION = SYSDATE
 WHERE RMS_ITEM_CODE IN ('100708858', '100708860', '100708862')
   AND FIELD_ID = 'authorization'
   AND CAPABILITY = 'PRECHECK';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT RMS_ITEM_CODE, SERVICE_PROVIDER_CODE, FIELD_ID, FIELD_TYPE, CAPABILITY, FIELD_LENGTH
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE (SERVICE_PROVIDER_CODE = '408403' AND FIELD_ID = 'amount' AND FIELD_TYPE = 'DOUBLE')
    OR (SERVICE_PROVIDER_CODE = '407925' AND FIELD_ID = 'phone' AND FIELD_TYPE = 'STRING')
    OR (RMS_ITEM_CODE IN ('100708858', '100708860', '100708862') AND FIELD_ID = 'authorization' AND CAPABILITY = 'PRECHECK')
 ORDER BY SERVICE_PROVIDER_CODE, RMS_ITEM_CODE, FIELD_ID;
-- Esperado: BET593 amount con FIELD_LENGTH=2, Claro phone con FIELD_LENGTH=10,
-- Premios Pega authorization con FIELD_LENGTH=27; el resto de filas de la tabla
-- (no cubiertas por este script) deben seguir con FIELD_LENGTH null.
