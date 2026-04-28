select category_code, subcategory_code, service_provider_code, rms_item_code, input_field_id, label, field_type, capability_code, is_required, field_group, conditional_operator
from (
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 0 as is_required, 'IDENTIFICATION' as field_group, 'OR' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 'userid' as input_field_id, 'ID Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 0 as is_required, 'IDENTIFICATION' as field_group, 'OR' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 'phone' as input_field_id, 'Celular Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 0 as is_required, 'IDENTIFICATION' as field_group, 'OR' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565826' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'withdrawId' as input_field_id, 'Número asignado a retiro' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'password' as input_field_id, 'Clave de retiro' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'password' as input_field_id, 'Clave de retiro' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'amount' as input_field_id, 'Monto reverso' as label, 'DOUBLE' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '1' as service_provider_code, '10001565827' as rms_item_code, 'motivo' as input_field_id, 'Motivo del reverso' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'DETAIL' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'EXECUTE' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'EXECUTE' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'EXECUTE' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'withdrawId' as input_field_id, 'Número asignado a retiro' as label, 'STRING' as field_type, 'EXECUTE' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'VERIFY' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'withdrawId' as input_field_id, 'Numero asignado a retiro' as label, 'STRING' as field_type, 'VERIFY' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 'document' as input_field_id, 'Cuenta web' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565828' as rms_item_code, 'motivo' as input_field_id, 'Motivo del reverso' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'DETAIL' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'authorization' as input_field_id, 'Numero de transaccion original' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'ID' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '1' as subcategory_code, '2' as service_provider_code, '10001565829' as rms_item_code, 'motivo' as input_field_id, 'Motivo del reverso' as label, 'STRING' as field_type, 'REVERSE' as capability_code, 1 as is_required, 'DETAIL' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '1' as category_code, '2' as subcategory_code, '2' as service_provider_code, '20001565830' as rms_item_code, 'rms_item_code' as input_field_id, 'CODIGO_RMS_ITEM' as label, 'STRING' as field_type, 'EXECUTE' as capability_code, 1 as is_required, 'ITEM_SKU' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12004565834' as rms_item_code, 'phone' as input_field_id, 'Celular Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'PHONE' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '5' as category_code, '9' as subcategory_code, '7' as service_provider_code, '12001565829' as rms_item_code, 'phone' as input_field_id, 'Celular Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'PHONE' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '3' as subcategory_code, '4' as service_provider_code, '10001565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '3' as subcategory_code, '4' as service_provider_code, '10001565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '4' as subcategory_code, '4' as service_provider_code, '10004565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '4' as subcategory_code, '4' as service_provider_code, '10004565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '5' as subcategory_code, '4' as service_provider_code, '10005565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '2' as category_code, '5' as subcategory_code, '4' as service_provider_code, '10005565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '6' as subcategory_code, '6' as service_provider_code, '10014565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '6' as subcategory_code, '6' as service_provider_code, '10014565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '7' as subcategory_code, '5' as service_provider_code, '10024565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '3' as category_code, '7' as subcategory_code, '5' as service_provider_code, '10024565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10204565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10204565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10201565829' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '5' as service_provider_code, '10201565829' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12004565834' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12004565834' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12001565829' as rms_item_code, 'document' as input_field_id, 'Documento Usuario' as label, 'STRING' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'IDENTIFICATION' as field_group, '' as conditional_operator from dual
union all
select '1' as chain, '148' as store, 'FYBECA AMAZONAS' as store_name, '1' as pos, 'POS' as channel_pos, '4' as category_code, '8' as subcategory_code, '6' as service_provider_code, '12001565829' as rms_item_code, 'amount' as input_field_id, 'Monto Recarga' as label, 'DOUBLE' as field_type, 'PRECHECK' as capability_code, 1 as is_required, 'AMOUNT' as field_group, '' as conditional_operator from dual
) catalog
where chain = :chain
  and store = :store
  and store_name = :store_name
  and pos = :pos
  and channel_pos = :channel_pos
order by to_number(category_code), to_number(subcategory_code), to_number(service_provider_code), rms_item_code, input_field_id
