SELECT
    f.CATEGORY_CODE         AS category_code,
    f.SUBCATEGORY_CODE      AS subcategory_code,
    f.SERVICE_PROVIDER_CODE AS service_provider_code,
    f.RMS_ITEM_CODE         AS rms_item_code,
    f.FIELD_ID              AS input_field_id,
    f.LABEL                 AS label,
    f.FIELD_TYPE            AS field_type,
    f.CAPABILITY            AS capability_code,
    f.IS_REQUIRED           AS is_required,
    f.FIELD_GROUP           AS field_group,
    f.CONDITIONAL_OPERATOR  AS conditional_operator,
    f.FIELD_LENGTH          AS field_length,
    f.REGEX                 AS regex,
    f.GROUP_LENGTH          AS group_length
FROM IN_OMNI_INPUT_FIELDS f
WHERE f.RMS_ITEM_CODE IN (:rms_item_codes)
  AND f.ENABLED = 'S'
ORDER BY f.RMS_ITEM_CODE, f.CAPABILITY, f.FIELD_ORDER
