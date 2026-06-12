-- Campos de entrada del formulario POS: sin tabla fuente en esquema DBA — pendiente definicion
SELECT TO_CHAR(NULL) AS category_code,
       TO_CHAR(NULL) AS subcategory_code,
       TO_CHAR(NULL) AS service_provider_code,
       TO_CHAR(NULL) AS rms_item_code,
       TO_CHAR(NULL) AS input_field_id,
       TO_CHAR(NULL) AS label,
       TO_CHAR(NULL) AS field_type,
       TO_CHAR(NULL) AS capability_code,
       0             AS is_required,
       TO_CHAR(NULL) AS field_group,
       TO_CHAR(NULL) AS conditional_operator
FROM DUAL
WHERE 1 = 0
