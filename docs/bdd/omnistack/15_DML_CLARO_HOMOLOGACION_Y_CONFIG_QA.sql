-- ============================================================
-- Ejecutar como: TUKUNAFUNC
-- Requiere: script 14 ejecutado previamente.
--
-- HOMOLOGACION: company_id por cadena
--   VALOR_ORIGEN = valor que envía el POS (chain)
--   CONFIG_VALOR = COMPANYID que CLARO espera según el contrato
--
-- CONFIG FIJA: media_id — valor constante para todos los items CLARO.
--   Se almacena aquí (no en AD_ITEM_SERVICIO) porque no varía por rms_item_code.
--
-- NOTA: cod_caja, cod_site van en GPF_OMNISTACK.AD_ITEM_SERVICIO (script 16)
--       porque pueden variar por item en el futuro.
-- ============================================================

-- ---- HOMOLOGACION company_id ------------------------------------
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
  (ID_CONFIG, PROVEEDOR_KEY, CONFIG_KEY, TIPO_CONFIG, CONFIG_VALOR, VALOR_ORIGEN)
VALUES (SEQ_IN_OMNI_PROVEEDOR_CONFIG.NEXTVAL,
  'claro', 'company_id', 'MAPEO', '2', '1');
-- chain=1 (FYBECA) → CLARO COMPANYID=2

INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
  (ID_CONFIG, PROVEEDOR_KEY, CONFIG_KEY, TIPO_CONFIG, CONFIG_VALOR, VALOR_ORIGEN)
VALUES (SEQ_IN_OMNI_PROVEEDOR_CONFIG.NEXTVAL,
  'claro', 'company_id', 'MAPEO', '7', '8');
-- chain=8 (SANA SANA) → CLARO COMPANYID=7

-- ⚠ Agregar más cadenas según acuerdo con CLARO antes de go-live

-- ---- CONFIG FIJA media_id ---------------------------------------
INSERT INTO TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
  (ID_CONFIG, PROVEEDOR_KEY, CONFIG_KEY, TIPO_CONFIG, CONFIG_VALOR, VALOR_ORIGEN)
VALUES (SEQ_IN_OMNI_PROVEEDOR_CONFIG.NEXTVAL,
  'claro', 'media_id', 'FIJO', 'RETA', NULL);
-- Red de Establecimientos Retail — constante para todos los items CLARO

COMMIT;

-- Verificar:
-- SELECT CONFIG_KEY, CONFIG_VALOR, VALOR_ORIGEN
--   FROM TUKUNAFUNC.IN_OMNI_PROVEEDOR_CONFIG
--  WHERE PROVEEDOR_KEY = 'claro'
--  ORDER BY CONFIG_KEY, VALOR_ORIGEN;
