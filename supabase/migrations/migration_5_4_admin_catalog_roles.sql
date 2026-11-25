-- 5.4: Admin quản trị khu vực / loại lỗi và cấp vai trò (Student <-> Technician).
-- Chạy trong Supabase SQL Editor. Idempotent: chạy lại nhiều lần không lỗi.
-- Giả định: public.current_role() trả vai trò của người đang đăng nhập (đã dùng trong các policy hiện có).

begin;

-- 1) Đồng bộ bộ đếm id (phòng khi dữ liệu mẫu được chèn kèm id cố định).
select setval(pg_get_serial_sequence('public.areas', 'id'),
              greatest((select coalesce(max(id), 1) from public.areas), 1));
select setval(pg_get_serial_sequence('public.categories', 'id'),
              greatest((select coalesce(max(id), 1) from public.categories), 1));

-- 2) Chống trùng tên (không phân biệt hoa thường / khoảng trắng thừa).
create unique index if not exists areas_name_building_uniq
  on public.areas (lower(btrim(name)), lower(btrim(coalesce(building, ''))));
create unique index if not exists categories_name_uniq
  on public.categories (lower(btrim(name)));

-- 3) Quyền ghi cho Admin (RLS quyết định ai được ghi).
grant insert, update, delete on public.areas to authenticated;
grant insert, update, delete on public.categories to authenticated;
grant usage, select on sequence public.areas_id_seq to authenticated;
grant usage, select on sequence public.categories_id_seq to authenticated;

drop policy if exists areas_admin_insert on public.areas;
drop policy if exists areas_admin_update on public.areas;
drop policy if exists areas_admin_delete on public.areas;
create policy areas_admin_insert on public.areas for insert to authenticated
  with check (public.current_role() = 'admin');
create policy areas_admin_update on public.areas for update to authenticated
  using (public.current_role() = 'admin') with check (public.current_role() = 'admin');
create policy areas_admin_delete on public.areas for delete to authenticated
  using (public.current_role() = 'admin');

drop policy if exists categories_admin_insert on public.categories;
drop policy if exists categories_admin_update on public.categories;
drop policy if exists categories_admin_delete on public.categories;
create policy categories_admin_insert on public.categories for insert to authenticated
  with check (public.current_role() = 'admin');
create policy categories_admin_update on public.categories for update to authenticated
  using (public.current_role() = 'admin') with check (public.current_role() = 'admin');
create policy categories_admin_delete on public.categories for delete to authenticated
  using (public.current_role() = 'admin');

-- 4) Đổi tên khu vực / loại lỗi thì bảng public_incidents (tên phi chuẩn hóa) phải theo.
create or replace function public.sync_public_incident_names()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if tg_table_name = 'areas' then
    update public.public_incidents p
       set area_name = new.name, building = new.building
      from public.tickets t
     where t.id = p.ticket_id and t.area_id = new.id;
  else
    update public.public_incidents p
       set category_name = new.name
      from public.tickets t
     where t.id = p.ticket_id and t.category_id = new.id;
  end if;
  return new;
end;
$$;

drop trigger if exists trg_areas_sync_public on public.areas;
create trigger trg_areas_sync_public
  after update of name, building on public.areas
  for each row execute function public.sync_public_incident_names();

drop trigger if exists trg_categories_sync_public on public.categories;
create trigger trg_categories_sync_public
  after update of name on public.categories
  for each row execute function public.sync_public_incident_names();

-- 5) Nhật ký đổi vai trò (chỉ Admin đọc; ghi qua RPC).
create table if not exists public.role_changes (
  id          bigint generated always as identity primary key,
  target_id   uuid not null,
  target_email text,
  target_name  text,
  actor_id    uuid not null,
  old_role    public.user_role not null,
  new_role    public.user_role not null,
  note        text not null,
  created_at  timestamptz not null default now()
);

alter table public.role_changes enable row level security;
revoke all on public.role_changes from anon, authenticated;
grant select on public.role_changes to authenticated;

drop policy if exists role_changes_select_admin on public.role_changes;
create policy role_changes_select_admin on public.role_changes for select to authenticated
  using (public.current_role() = 'admin');

-- 6) RPC đổi vai trò.
create or replace function public.set_user_role(p_user_id uuid, p_new_role public.user_role, p_note text)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_actor uuid := auth.uid();
  v_target public.profiles%rowtype;
  v_note text := btrim(coalesce(p_note, ''));
begin
  if v_actor is null then
    raise exception 'Bạn chưa đăng nhập.';
  end if;
  if public.current_role() <> 'admin' then
    raise exception 'Chỉ Quản trị viên mới được đổi vai trò.';
  end if;
  if p_user_id = v_actor then
    raise exception 'Không thể tự đổi vai trò của chính mình.';
  end if;
  if p_new_role not in ('student', 'technician') then
    raise exception 'Web chỉ cấp vai trò Sinh viên hoặc Kỹ thuật viên.';
  end if;
  if char_length(v_note) < 5 or char_length(v_note) > 300 then
    raise exception 'Căn cứ phải dài từ 5 đến 300 ký tự.';
  end if;

  select * into v_target from public.profiles where id = p_user_id for update;
  if not found then
    raise exception 'Không tìm thấy người dùng.';
  end if;
  if v_target.role = 'admin' then
    raise exception 'Không thể đổi vai trò của Quản trị viên.';
  end if;
  if v_target.role = p_new_role then
    raise exception 'Người dùng đã có vai trò này.';
  end if;
  if v_target.role = 'technician' and exists (
       select 1 from public.tickets
        where assigned_to = p_user_id and status in ('assigned', 'in_progress')) then
    raise exception 'Kỹ thuật viên còn phiếu đang xử lý. Hãy phân công lại trước khi thu hồi.';
  end if;

  update public.profiles set role = p_new_role where id = p_user_id;

  insert into public.role_changes (target_id, target_email, target_name, actor_id, old_role, new_role, note)
  values (p_user_id, v_target.email, v_target.full_name, v_actor, v_target.role, p_new_role, v_note);
end;
$$;

revoke all on function public.set_user_role(uuid, public.user_role, text) from public, anon;
grant execute on function public.set_user_role(uuid, public.user_role, text) to authenticated;

commit;
