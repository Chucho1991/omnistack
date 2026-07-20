-- ============================================================
-- Ejecutar como: TUKUNAFUNC
--
-- MOTIVO:
--   El script 30 (PASO 6 y PASO 7) inserta los campos REVERSE de
--   ECUABET (100713841/100708846) y BET593 CASH_OUT (100708848) con
--   INSERT simple, sin guarda "WHERE NOT EXISTS" (a diferencia del
--   PASO 8, que si la tiene para CLARO/RECARGA MEGAS). Si esa seccion
--   del script se ejecuto mas de una vez, cada fila quedo duplicada.
--
--   Confirmado en la respuesta de /business-lines despues de reiniciar
--   el docker: "motivo" REVERSE aparece 2 veces en ECUABET RECARGA (1118)
--   y ECUABET PREMIO (1119); "document"(Cuenta web) y "motivo" REVERSE
--   aparecen 2 veces cada uno en BET593 PREMIO (1121, rms 100708848).
--
-- SOLUCION:
--   Borrar duplicados dejando solo la fila con menor ID_FIELD por cada
--   combinacion (RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_GROUP).
-- ============================================================

DELETE FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS f
 WHERE f.RMS_ITEM_CODE IN ('100713841', '100708846', '100708848')
   AND f.CAPABILITY = 'REVERSE'
   AND f.ID_FIELD > (
        SELECT MIN(f2.ID_FIELD)
          FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS f2
         WHERE f2.RMS_ITEM_CODE = f.RMS_ITEM_CODE
           AND f2.CAPABILITY   = f.CAPABILITY
           AND f2.FIELD_ID     = f.FIELD_ID
           AND f2.FIELD_GROUP  = f.FIELD_GROUP
       );

COMMIT;

-- ============================================================
-- VERIFICACION: cada FIELD_ID debe aparecer 1 sola vez por item/capability
-- ============================================================
SELECT RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_GROUP, COUNT(*) AS repeticiones
  FROM TUKUNAFUNC.IN_OMNI_INPUT_FIELDS
 WHERE RMS_ITEM_CODE IN ('100713841', '100708846', '100708848')
   AND CAPABILITY = 'REVERSE'
 GROUP BY RMS_ITEM_CODE, CAPABILITY, FIELD_ID, FIELD_GROUP
 ORDER BY RMS_ITEM_CODE, FIELD_ID;
-- Esperado: repeticiones = 1 en todas las filas
