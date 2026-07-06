-- ============================================================
-- Agrega el endpoint GenerarComprobantePega (PDF de comprobante) como
-- operacion VERIFY_COMPROBANTE.CASHIN de Pega3, separada de VERIFY.CASHIN
-- (que sigue apuntando a ConsultarTicket). Ambos endpoints son legitimos
-- y complementarios: ConsultarTicket = estado/premio, GenerarComprobantePega = PDF.
-- Ver docs/LOTERIA NACIONAL.docx, seccion GenerarComprobantePega.
-- ============================================================

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS
  (ID_WS, PROVEEDOR_KEY, WS_KEY, ENABLED, TIPO_CONEXION, METODO_HTTP, TIPO_REQUEST, URL, NOMBRE_OPERACION)
VALUES
  (SEQ_IN_OMNI_PROVEEDOR_WS.NEXTVAL, 'pega3','VERIFY_COMPROBANTE.CASHIN','S','REST','GET','JSON',
   'https://www8.loteria.com.ec/APIVentasLoteria/api/Ventas/GenerarComprobantePega','GENERAR_COMPROBANTE_PEGA3');

-- ---- PEGA3 — VERIFY_COMPROBANTE.CASHIN ----
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'item.100708852', '100708852' FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'pega3' AND w.WS_KEY = 'VERIFY_COMPROBANTE.CASHIN';
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'item.100713848', '100713848' FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'pega3' AND w.WS_KEY = 'VERIFY_COMPROBANTE.CASHIN';
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL, w.ID_WS, 'CONFIG', 'item.100713850', '100713850' FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w WHERE w.PROVEEDOR_KEY = 'pega3' AND w.WS_KEY = 'VERIFY_COMPROBANTE.CASHIN';

-- NOTA: la llamada a GenerarComprobantePega requiere "transaccion" (query param OBLIGATORIO
-- segun el proveedor) que hoy no viene en ninguna respuesta de CrearTicket/ConsultarTicket
-- Pega3 que tengamos capturada. El codigo (LoteriaPega3VerifyStrategy) solo invoca este
-- endpoint si el VerifyRequest trae el campo "transaccion" — mientras no se identifique de
-- donde sale ese valor, esta operacion queda configurada pero inactiva en la practica.
