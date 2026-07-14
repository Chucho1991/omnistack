-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- FIX: Agrega la entrada 'item' faltante en IN_OMNI_PROVEEDOR_WS_DEFS
-- para la operacion PRECHECK.CASHOUT del proveedor 'loteria' (BET593).
--
-- PROBLEMA:
--   El precheck de CASH_OUT para rms_item_code=100708848 (BET593 PREMIO)
--   devuelve "El producto solicitado no esta completamente configurado"
--   porque hasConfiguredOperation() no encuentra item en WS_DEFS para
--   WS_KEY='PRECHECK.CASHOUT'.
--
--   El script 02_DML_PROVEEDORES.sql inserto item para EXECUTE.CASHOUT,
--   VERIFY.CASHOUT y REVERSE.CASHOUT, pero omitio PRECHECK.CASHOUT.
--
-- SOLUCION:
--   Insertar item.100708848 (formato multi-item) para PRECHECK.CASHOUT
--   del proveedor 'loteria'. Esto es consistente con el patron usado
--   en scripts 27 y 28 para pega3 y tradicional.
-- ============================================================

-- Insertar item en formato multi-item (item.{rmsItemCode})
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS
  (ID_DEFAULT, ID_WS, TIPO_DEF, DEFAULT_CLAVE, DEFAULT_VALOR_TEXT)
SELECT SEQ_IN_OMNI_PROVEEDOR_WS_DEFS.NEXTVAL,
       w.ID_WS,
       'CONFIG',
       'item.100708848',
       '100708848'
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
 WHERE w.PROVEEDOR_KEY = 'loteria'
   AND w.WS_KEY        = 'PRECHECK.CASHOUT'
   AND NOT EXISTS (
       SELECT 1
         FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d
        WHERE d.ID_WS         = w.ID_WS
          AND d.DEFAULT_CLAVE = 'item.100708848'
   );

COMMIT;

-- ============================================================
-- VERIFICACION: debe retornar 1 fila con valor '100708848'
-- ============================================================
SELECT w.PROVEEDOR_KEY, w.WS_KEY, d.DEFAULT_CLAVE, d.DEFAULT_VALOR_TEXT
  FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS w
  JOIN TUKUNAFUNC.IN_OMNI_PROVEEDOR_WS_DEFS d ON d.ID_WS = w.ID_WS
 WHERE w.PROVEEDOR_KEY = 'loteria'
   AND w.WS_KEY        = 'PRECHECK.CASHOUT'
   AND d.DEFAULT_CLAVE LIKE 'item%';
