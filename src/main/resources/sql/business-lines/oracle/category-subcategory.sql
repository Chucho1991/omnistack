select category_code, category_name, subcategory_code, subcategory_name, is_active
from (
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, 'ENTRETENIMIENTO' as category_name, '1' as subcategory_code, 'APUESTAS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, 'ENTRETENIMIENTO' as category_name, '2' as subcategory_code, 'BOLETOS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, 'TELEFONÍA' as category_name, '9' as subcategory_code, 'RECARGAS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, 'CORRESPONSAL BANCARIO' as category_name, '3' as subcategory_code, 'SERVICIOS BASICOS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, 'CORRESPONSAL BANCARIO' as category_name, '4' as subcategory_code, 'IMPUESTOS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, 'CORRESPONSAL BANCARIO' as category_name, '5' as subcategory_code, 'VEHICULOS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, 'SEGUROS' as category_name, '6' as subcategory_code, 'SEGURO DE VIDA' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, 'SEGUROS' as category_name, '7' as subcategory_code, 'MASCOTAS' as subcategory_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, 'RECAUDOS' as category_name, '8' as subcategory_code, 'REMESAS' as subcategory_name, 1 as is_active from dual
) catalog
where chain = :chain
  and store = :store
  and store_name = :store_name
  and pos = :pos
  and channel_pos = :channel_pos
order by to_number(category_code), to_number(subcategory_code)
