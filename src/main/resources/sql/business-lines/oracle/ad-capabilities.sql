-- Capabilities derivadas de IN_OMNI_PROVEEDOR_WS_DEFS (item/item.*) y
-- como fallback de IN_OMNI_PROVEEDOR_WS por PROVEEDOR_KEY (para proveedores
-- que no usan item.* en WS_DEFS, ej: claro).

-- Fuente 1: item/item.* en WS_DEFS → rms_item_code directo
SELECT DISTINCT
    d.DEFAULT_VALOR_TEXT                AS rms_item_code,
    REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+') AS capability_code
FROM IN_OMNI_PROVEEDOR_WS_DEFS d
JOIN IN_OMNI_PROVEEDOR_WS ws
    ON ws.ID_WS   = d.ID_WS
   AND ws.ENABLED = 'S'
WHERE d.TIPO_DEF = 'CONFIG'
  AND (d.DEFAULT_CLAVE = 'item' OR d.DEFAULT_CLAVE LIKE 'item.%')
  AND d.DEFAULT_VALOR_TEXT IS NOT NULL
  AND REGEXP_SUBSTR(ws.WS_KEY, '^[^.]+')
      IN ('PRECHECK', 'CREATE_TICKET', 'EXECUTE', 'VERIFY', 'REVERSE')
ORDER BY rms_item_code, capability_code
