-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   Pega3 no tiene un paso de "reserva" separado como Tradicionales
--   (RecuperarNumerosDisponiblesPorCombinacion). CrearTicket vende el
--   ticket por completo — su propia respuesta trae "status":"Purchased".
--   Por eso se elimino CREATE_TICKET para Pega3 (LoteriaPega3CreateTicketStrategy)
--   y CrearTicket ahora se llama desde EXECUTE (LoteriaPega3ExecuteStrategy).
--
--   El WS_KEY 'EXECUTE.CASHIN' de pega3 apuntaba a PagarTicket — endpoint
--   equivocado para esta operacion (PagarTicket es para COBRAR el premio de
--   un ticket YA vendido y ganador, se usa en CASH_OUT). Se reapunta a
--   CrearTicket, la misma URL que ya usaba CREATE_TICKET.CASHIN.
--
--   CREATE_TICKET.CASHIN se DESHABILITA (ENABLED='N', no se borra) en vez de
--   dejarla activa e inerte: las capabilities que expone /business-lines se
--   derivan directo de IN_OMNI_PROVEEDOR_WS (prefijo de WS_KEY antes del
--   punto, filtrado por ENABLED='S' — ver capabilities.sql y
--   ad-capabilities.sql). Si se deja ENABLED='S', /business-lines seguiria
--   anunciando CREATE_TICKET como capability disponible para estos 3 items
--   de Pega3 aunque el codigo ya no lo soporte, y el front intentaria llamar
--   /v1/createTicket para nada.
-- ============================================================

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
   SET URL = 'https://www8.loteria.com.ec/APIVentasLoteria/api/Ventas/CrearTicket',
       NOMBRE_OPERACION = 'CREAR_TICKET_PEGA3_EXECUTE'
 WHERE PROVEEDOR_KEY = 'pega3'
   AND WS_KEY = 'EXECUTE.CASHIN';

UPDATE TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
   SET ENABLED = 'N'
 WHERE PROVEEDOR_KEY = 'pega3'
   AND WS_KEY = 'CREATE_TICKET.CASHIN';

COMMIT;

-- ============================================================
-- VERIFICACION
-- ============================================================
SELECT PROVEEDOR_KEY, WS_KEY, ENABLED, URL, NOMBRE_OPERACION
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
 WHERE PROVEEDOR_KEY = 'pega3' AND WS_KEY IN ('EXECUTE.CASHIN', 'CREATE_TICKET.CASHIN')
 ORDER BY WS_KEY;
-- Esperado:
--   CREATE_TICKET.CASHIN -> ENABLED = N
--   EXECUTE.CASHIN        -> ENABLED = S, URL = .../CrearTicket, NOMBRE_OPERACION = CREAR_TICKET_PEGA3_EXECUTE

-- Confirmar que CREATE_TICKET ya no aparece como capability para los items de Pega3
SELECT DISTINCT d.DEFAULT_VALOR_TEXT AS rms_item_code, REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+') AS capability_code
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
  JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS ws ON ws.ID_WS = d.ID_WS AND ws.ENABLED = 'S'
 WHERE ws.PROVEEDOR_KEY = 'pega3'
   AND d.TIPO_DEF = 'CONFIG'
   AND (d.DEFAULT_CLAVE = 'item' OR d.DEFAULT_CLAVE LIKE 'item.%')
 ORDER BY rms_item_code, capability_code;
-- Esperado: por cada rms_item_code (100708852/100713848/100713850) solo deben aparecer
-- PRECHECK, EXECUTE, VERIFY, REVERSE — sin CREATE_TICKET
