-- ============================================================
-- Ejecutar como: TRX3 (ambiente APPTEST, 10.100.3.35:1521/APPTEST)
-- Requiere: privilegio ALTER SEQUENCE sobre TRX3.SEQ_IN_OMNI_INPUT_FIELDS
--           (falla con ORA-01031 si el usuario conectado no es el dueño
--           del objeto ni tiene el privilegio otorgado explicitamente).
--
-- MOTIVO:
--   Detectado el 2026-08-24 al intentar insertar en IN_OMNI_INPUT_FIELDS
--   usando SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL (ver script 44): la secuencia
--   quedo en LAST_NUMBER=2 mientras la tabla migrada desde TUKUNAFUNC@PRS6
--   ya tiene datos hasta ID_FIELD=60030. La migracion que copio los datos
--   de IN_OMNI_INPUT_FIELDS a este esquema nunca reajusto la secuencia
--   (mismo tipo de riesgo ya documentado en CONTEXTO_SESION_2026-07.md
--   seccion 10.3 para las secuencias de GPF_OMNISTACK). Cualquier INSERT
--   que use NEXTVAL revienta con ORA-00001 (restriccion unica violada)
--   hasta que se corrija.
--
--   El script 44 esquivo el problema insertando con ID_FIELD explicito
--   (60031-60033) para no bloquear esa entrega, pero la secuencia sigue
--   rota para cualquier INSERT futuro (app en runtime u otros scripts)
--   hasta correr este fix.
--
-- TECNICA: ALTER SEQUENCE no permite fijar LAST_NUMBER directo. Se hace
--   el salto subiendo INCREMENT_BY temporalmente, consumiendo un solo
--   NEXTVAL para saltar de una vez, y devolviendo INCREMENT_BY a 1.
-- ============================================================

-- PASO 1: Verificar estado actual ANTES del fix (comparar contra el paso 4)
SELECT sq.SEQUENCE_NAME, sq.LAST_NUMBER, sq.INCREMENT_BY,
       (SELECT MAX(ID_FIELD) FROM TRX3.IN_OMNI_INPUT_FIELDS) AS max_id_field
  FROM USER_SEQUENCES sq
 WHERE sq.SEQUENCE_NAME = 'SEQ_IN_OMNI_INPUT_FIELDS';

-- PASO 2: Saltar la secuencia por encima del MAX(ID_FIELD) real.
-- 70000 da margen holgado sobre el MAX_ID_FIELD=60030 visto el 2026-08-24
-- (incluye los 3 registros insertados por el script 44 con ID explicito
-- 60031-60033) -- si al momento de correr esto MAX(ID_FIELD) ya es mayor,
-- ajustar el INCREMENT BY de este paso para que LAST_NUMBER final quede
-- por encima del nuevo MAX antes de continuar.
ALTER SEQUENCE TRX3.SEQ_IN_OMNI_INPUT_FIELDS INCREMENT BY 70000;

-- PASO 3: Consumir un NEXTVAL para materializar el salto, y devolver el
-- incremento a 1 para operacion normal.
SELECT TRX3.SEQ_IN_OMNI_INPUT_FIELDS.NEXTVAL FROM DUAL;

ALTER SEQUENCE TRX3.SEQ_IN_OMNI_INPUT_FIELDS INCREMENT BY 1;

-- ============================================================
-- PASO 4: VERIFICACION -- LAST_NUMBER debe quedar por encima de
-- max_id_field del paso 1, e INCREMENT_BY debe volver a 1.
-- ============================================================
SELECT sq.SEQUENCE_NAME, sq.LAST_NUMBER, sq.INCREMENT_BY,
       (SELECT MAX(ID_FIELD) FROM TRX3.IN_OMNI_INPUT_FIELDS) AS max_id_field
  FROM USER_SEQUENCES sq
 WHERE sq.SEQUENCE_NAME = 'SEQ_IN_OMNI_INPUT_FIELDS';

-- ============================================================
-- NOTA: este mismo problema puede existir en otras secuencias de TRX3
-- si la migracion las trato igual. Antes de asumir que estan bien,
-- correr por cada tabla/secuencia relevante:
--   SELECT sq.SEQUENCE_NAME, sq.LAST_NUMBER, tab.max_pk
--     FROM USER_SEQUENCES sq, (SELECT MAX(<PK_COLUMN>) AS max_pk FROM <TABLA>) tab
--    WHERE sq.SEQUENCE_NAME = '<SEQ_NAME>';
-- Ver docs/bdd/omnistack/ALL_SEQUENCES.sql para el listado completo de
-- secuencias del esquema TUKUNAFUNC/TRX3.
-- ============================================================
