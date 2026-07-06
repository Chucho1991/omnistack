SELECT
    TRIM(im.ITEM)        AS rms_item_code,
    TO_CHAR(c.CLASS)     AS category_code,
    c.CLASS_NAME         AS category_name,
    TO_CHAR(sc.SUBCLASS) AS subcategory_code,
    sc.SUB_NAME          AS subcategory_name,
    im.ITEM_DESC         AS description
FROM ITEM_MASTER im
JOIN CLASS c
    ON c.CLASS = im.CLASS
JOIN SUBCLASS sc
    ON sc.CLASS    = im.CLASS
   AND sc.SUBCLASS = im.SUBCLASS
WHERE TRIM(im.ITEM) IN (:rms_item_codes)
ORDER BY c.CLASS, sc.SUBCLASS, TRIM(im.ITEM)
