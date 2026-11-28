-- Kiểm tra 6.A. Chạy trong SQL Editor SAU KHI chạy migration_6a_auto_assign.sql.
-- Cần có sẵn: 1 sinh viên, 1 kỹ thuật viên, 1 khu vực, 1 loại lỗi. Toàn bộ chạy trong giao dịch rồi ROLLBACK,
-- nên không để lại dữ liệu. Kết quả đọc ở tab Messages/Notices: mỗi test in PASS hoặc ném lỗi FAIL.

begin;

do $$
declare
  s uuid; t uuid; a int; c int;
  v1 uuid; v2 uuid;
  r public.tickets%rowtype;
  n int;
  last_note text;
begin
  select id into s from public.profiles where role = 'student' limit 1;
  select id into t from public.profiles where role = 'technician' limit 1;
  select min(id) into a from public.areas;
  select min(id) into c from public.categories;
  if s is null or t is null or a is null or c is null then
    raise exception 'Thiếu dữ liệu mẫu (cần sinh viên, kỹ thuật viên, khu vực, loại lỗi).';
  end if;

  -- Chỉ kỹ thuật viên t sẵn sàng; tự động phân công BẬT, ETA 2 ngày.
  update public.auto_assign_settings set enabled = true, eta_days = 2, fallback_any = true where id;
  delete from public.technician_settings;
  insert into public.technician_settings (technician_id, is_available) values (t, true);

  -- T1: sinh viên tạo phiếu -> tự giao cho t
  perform set_config('request.jwt.claims', json_build_object('sub', s, 'role', 'authenticated')::text, true);
  execute 'set local role authenticated';
  insert into public.tickets (created_by, area_id, category_id, title) values (s, a, c, 'TEST tu dong phan cong 1') returning id into v1;
  execute 'reset role';
  select * into r from public.tickets where id = v1;
  if r.status = 'assigned' and r.assigned_to = t and r.eta is not null then
    raise notice 'PASS T1: phiếu mới được giao tự động (assigned, đúng KTV, có ETA).';
  else
    raise exception 'FAIL T1: status=%, assigned_to=%, eta=%', r.status, r.assigned_to, r.eta;
  end if;

  -- T2: timeline có 2 sự kiện đúng thứ tự open -> assigned, sự kiện assigned có note và actor null
  select count(*) into n from public.ticket_events where ticket_id = v1;
  select e.note into last_note from public.ticket_events e where e.ticket_id = v1 order by e.created_at desc limit 1;
  if n = 2 and last_note = 'Tự động phân công'
     and (select actor_id from public.ticket_events where ticket_id = v1 order by created_at desc limit 1) is null then
    raise notice 'PASS T2: timeline đúng (2 sự kiện, assigned sau open, actor null, có ghi chú).';
  else
    raise exception 'FAIL T2: events=%, note=%', n, last_note;
  end if;

  -- T3: tắt tự động -> phiếu mới giữ Open
  update public.auto_assign_settings set enabled = false where id;
  perform set_config('request.jwt.claims', json_build_object('sub', s, 'role', 'authenticated')::text, true);
  execute 'set local role authenticated';
  insert into public.tickets (created_by, area_id, category_id, title) values (s, a, c, 'TEST tu dong phan cong 2') returning id into v2;
  execute 'reset role';
  select * into r from public.tickets where id = v2;
  if r.status = 'open' and r.assigned_to is null then
    raise notice 'PASS T3: tắt tự động thì phiếu giữ Open.';
  else
    raise exception 'FAIL T3: status=%', r.status;
  end if;

  -- T4: bật lại nhưng KTV nghỉ -> giữ Open; KTV bật sẵn sàng -> phiếu chờ được giao
  update public.auto_assign_settings set enabled = true where id;
  update public.technician_settings set is_available = false where technician_id = t;
  perform public.auto_assign_one(v2);
  select * into r from public.tickets where id = v2;
  if r.status <> 'open' then
    raise exception 'FAIL T4a: không có ai sẵn sàng mà phiếu vẫn bị giao (status=%)', r.status;
  end if;
  perform set_config('request.jwt.claims', json_build_object('sub', t, 'role', 'authenticated')::text, true);
  execute 'set local role authenticated';
  perform public.set_my_availability(true);
  execute 'reset role';
  select * into r from public.tickets where id = v2;
  if r.status = 'assigned' and r.assigned_to = t then
    raise notice 'PASS T4: KTV bật Sẵn sàng thì phiếu đang chờ được giao.';
  else
    raise exception 'FAIL T4b: status=%', r.status;
  end if;

  -- T5: sinh viên không được đổi cài đặt
  perform set_config('request.jwt.claims', json_build_object('sub', s, 'role', 'authenticated')::text, true);
  execute 'set local role authenticated';
  begin
    perform public.set_auto_assign(false, null, true);
    execute 'reset role';
    raise exception 'FAIL T5: sinh viên đổi được cài đặt.';
  exception when insufficient_privilege then
    execute 'reset role';
    raise notice 'PASS T5: sinh viên bị chặn đổi cài đặt.';
  end;
end $$;

rollback;
