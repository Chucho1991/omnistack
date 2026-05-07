select category_code, subcategory_code, service_provider_code, ruc_provider, provider_name, is_active
from (
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '9999999999001' as ruc_provider, 'ECUABET' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '9999999999001' as ruc_provider, 'LOTERIA NACIONAL' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '9999999999001' as ruc_provider, 'LOTERIA NACIONAL' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '9999999999001' as ruc_provider, 'CLARO' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '3' as subcategory_code, '4' as service_provider_code, '9999999999001' as ruc_provider, 'MI NEGOCIO' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '4' as subcategory_code, '4' as service_provider_code, '9999999999001' as ruc_provider, 'MI NEGOCIO' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '5' as subcategory_code, '4' as service_provider_code, '9999999999001' as ruc_provider, 'MI NEGOCIO' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '6' as subcategory_code, '6' as service_provider_code, '9999999999001' as ruc_provider, 'ASEGURADORA DEL SUR' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '7' as subcategory_code, '5' as service_provider_code, '9999999999001' as ruc_provider, 'OTTOCARE' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '9999999999001' as ruc_provider, 'WESTERN UNION' as provider_name, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '9999999999001' as ruc_provider, 'MONEYGRAM' as provider_name, 1 as is_active from dual
) catalog
where channel_pos = :channel_pos
order by to_number(category_code), to_number(subcategory_code), to_number(service_provider_code)
