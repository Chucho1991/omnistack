select category_code, subcategory_code, service_provider_code, rms_item_code, service_payment_method_id, payment_method_code, is_active
from (
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565830' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565830' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '20001565830' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '20001565830' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565831' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565832' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565832' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '10001565833' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12004565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12004565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12001565829' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12001565829' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '3' as subcategory_code, '4' as service_provider_code, '10001565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '3' as subcategory_code, '4' as service_provider_code, '10001565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '4' as subcategory_code, '4' as service_provider_code, '10004565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '4' as subcategory_code, '4' as service_provider_code, '10004565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '5' as subcategory_code, '4' as service_provider_code, '10005565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '5' as subcategory_code, '4' as service_provider_code, '10005565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '6' as subcategory_code, '6' as service_provider_code, '10014565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '6' as subcategory_code, '6' as service_provider_code, '10014565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '7' as subcategory_code, '5' as service_provider_code, '10024565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '7' as subcategory_code, '5' as service_provider_code, '10024565834' as rms_item_code, 2 as service_payment_method_id, 'TARJETA CREDITO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10204565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10201565829' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12004565834' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12001565829' as rms_item_code, 1 as service_payment_method_id, 'EFECTIVO' as payment_method_code, 1 as is_active from dual
) catalog
where channel_pos = :channel_pos
order by to_number(category_code), to_number(subcategory_code), to_number(service_provider_code), rms_item_code, service_payment_method_id
