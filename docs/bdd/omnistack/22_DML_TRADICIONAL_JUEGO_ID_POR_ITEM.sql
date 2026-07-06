-- ============================================================
-- Cada rms_item_code de Tradicionales corresponde a UN SOLO juegoId real del
-- proveedor (confirmado en docs/LOTERIA NACIONAL.docx):
--   100713842 (Loteria Nacional) -> juegoId 1
--   100713844 (Lotto)            -> juegoId 2
--   100713846 (Pozzo Millonario) -> juegoId 5
-- Antes de este script, LoteriaTradicionalPrecheckStrategy usaba un
-- DEFAULT_JUEGO_ID="1" fijo en codigo cuando el front no mandaba "game_id",
-- lo que hacia que PRECHECK/EXECUTE de Lotto/Pozzo consultara por error los
-- datos de Loteria Nacional si el campo opcional no llegaba. Con estas filas,
-- el default se deriva del rms_item_code via ProviderWsDefsService (metodo
-- AbstractProviderStrategy.resolveItemDefault) en vez de estar hardcodeado.
-- Se registra bajo PRECHECK.CASHIN (LoteriaTradicionalPrecheckStrategy) y
-- EXECUTE.CASHIN (LoteriaTradicionalExecuteStrategy, por boleto en el
-- carrito) porque ambas estrategias necesitan resolver juegoId de forma
-- independiente.
-- ============================================================

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'juego_id.100713842', '1'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY IN ('PRECHECK.CASHIN', 'EXECUTE.CASHIN');

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'juego_id.100713844', '2'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY IN ('PRECHECK.CASHIN', 'EXECUTE.CASHIN');

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'juego_id.100713846', '5'
FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'tradicional' AND w.WS_KEY IN ('PRECHECK.CASHIN', 'EXECUTE.CASHIN');
