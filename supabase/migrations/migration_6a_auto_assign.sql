-- 6.A: Tự động phân công kỹ thuật viên (chạy ở phía cơ sở dữ liệu nên hoạt động 24/7, không cần Admin online).
-- Quy tắc: phiếu mới (Open, chưa có người) -> chọn kỹ thuật viên ĐANG SẴN SÀNG,
--   ưu tiên người có chuyên môn đúng loại sự cố, rồi người có ít phiếu chưa xong nhất, rồi người được giao lâu nhất.
-- Phiếu chuyển Open -> Assigned, ghi ticket_events (actor_id = null, note 'Tự động phân công').
-- Không có ai sẵn sàng -> phiếu giữ nguyên Open cho Admin; khi có người bật Sẵn sàng thì phiếu chờ được giao tiếp.
-- Chạy trong Supabase SQL Editor. Idempotent. Mặc định TẮT cho tới khi Admin bật.

begin;

-- ---------- Bảng ----------
create table if not exists public.auto_assign_settings (
  id           boolean primary key default true check (id),      -- chỉ có đúng một dòng
  enabled      boolean not null default false,
  eta_days     int check (eta_days in (1, 2, 3, 7)),             -- null = không đặt ETA
  fallback_any boolean not null default true,                    -- không có người đúng chuyên môn thì giao người khác
  updated_at   timestamptz not null default now(),
  updated_by   uuid
);
insert into public.auto_assign_settings (id) values (true) on conflict (id) do nothing;

create table if not exists public.technician_settings (
  technician_id uuid primary key references public.profiles(id) on delete cascade,
  is_available  boolean not null default false,                  -- chưa có dòng = chưa sẵn sàng
  updated_at    timestamptz not null default now()
);

create table if not exists public.technician_categories (
  technician_id uuid not null references public.profiles(id) on delete cascade,
  category_id   int  not null references public.categories(id) on delete cascade,
  primary key (technician_id, category_id)
);

alter table public.auto_assign_settings   enable row level security;
alter table public.technician_settings    enable row level security;
alter table public.technician_categories  enable row level security;

drop policy if exists auto_assign_settings_select on public.auto_assign_settings;
create policy auto_assign_settings_select on public.auto_assign_settings for select to authenticated
  using (public.current_role() = 'admin');

drop policy if exists technician_settings_select on public.technician_settings;
create policy technician_settings_select on public.technician_settings for select to authenticated
  using (public.current_role() = 'admin' or technician_id = auth.uid());

drop policy if exists technician_categories_select on public.technician_categories;
create policy technician_categories_select on public.technician_categories for select to authenticated
  using (public.current_role() in ('admin', 'technician'));
drop policy if exists technician_categories_insert on public.technician_categories;
create policy technician_categories_insert on public.technician_categories for insert to authenticated
  with check (public.current_role() = 'admin'
              and exists (select 1 from public.profiles p where p.id = technician_id and p.role = 'technician'));
drop policy if exists technician_categories_delete on public.technician_categories;
create policy technician_categories_delete on public.technician_categories for delete to authenticated
  using (public.current_role() = 'admin');

grant select on public.auto_assign_settings, public.technician_settings to authenticated;
grant select, insert, delete on public.technician_categories to authenticated;
-- Ghi cài đặt chỉ qua các hàm RPC bên dưới (không cấp insert/update trực tiếp).

-- ---------- Chọn người ----------
create or replace function public.pick_technician(p_category_id int, p_fallback boolean)
returns uuid
language sql
stable
security definer
set search_path = public
as $$
  select p.id
  from public.profiles p
  join public.technician_settings s on s.technician_id = p.id and s.is_available
  where p.role = 'technician'
    and (p_fallback or exists (select 1 from public.technician_categories tc
                               where tc.technician_id = p.id and tc.category_id = p_category_id))
  order by
    exists (select 1 from public.technician_categories tc
            where tc.technician_id = p.id and tc.category_id = p_category_id) desc,
    (select count(*) from public.tickets t
      where t.assigned_to = p.id and t.status in ('assigned', 'in_progress')) asc,
    (select max(e.created_at) from public.ticket_events e
       join public.tickets t on t.id = e.ticket_id
      where t.assigned_to = p.id and e.status = 'assigned') asc nulls first,
    p.id
  limit 1
$$;

-- ---------- Giao một phiếu ----------
-- Chạy với danh tính "hệ thống": tạm xóa claims đăng nhập để các trigger bảo vệ (student/tech guard)
-- coi đây là lệnh không có phiên, đồng thời ticket_events.actor_id = null. Khôi phục claims ngay sau đó.
create or replace function public.auto_assign_one(p_ticket_id uuid)
returns boolean
language plpgsql
security definer
set search_path = public
as $$
declare
  v_set    public.auto_assign_settings%rowtype;
  v_ticket public.tickets%rowtype;
  v_tech   uuid;
  v_claims text;
  v_sub    text;
begin
  select * into v_set from public.auto_assign_settings where id;
  if not found or not v_set.enabled then
    return false;
  end if;

  select * into v_ticket from public.tickets
   where id = p_ticket_id and status = 'open' and assigned_to is null
   for update;
  if not found then
    return false;
  end if;

  v_tech := public.pick_technician(v_ticket.category_id, v_set.fallback_any);
  if v_tech is null then
    return false;
  end if;

  v_claims := current_setting('request.jwt.claims', true);
  v_sub    := current_setting('request.jwt.claim.sub', true);
  perform set_config('request.jwt.claims', '', true);
  perform set_config('request.jwt.claim.sub', '', true);

  update public.tickets
     set assigned_to = v_tech,
         status      = 'assigned',
         eta         = case when v_set.eta_days is null then eta
                            else now() + make_interval(days => v_set.eta_days) end
   where id = p_ticket_id;

  -- Gắn ghi chú và đẩy mốc thời gian sau sự kiện 'open' (cùng giao dịch nên now() trùng nhau).
  update public.ticket_events
     set note = 'Tự động phân công', created_at = clock_timestamp()
   where id = (select e.id from public.ticket_events e
                where e.ticket_id = p_ticket_id and e.status = 'assigned'
                order by e.created_at desc limit 1);

  perform set_config('request.jwt.claims', coalesce(v_claims, ''), true);
  perform set_config('request.jwt.claim.sub', coalesce(v_sub, ''), true);
  return true;
end;
$$;

-- Giao tất cả phiếu Open chưa có người (cũ nhất trước). Trả về số phiếu đã giao.
create or replace function public.auto_assign_pending()
returns int
language plpgsql
security definer
set search_path = public
as $$
declare
  r record;
  n int := 0;
begin
  for r in select id from public.tickets where status = 'open' and assigned_to is null order by created_at loop
    if public.auto_assign_one(r.id) then
      n := n + 1;
    end if;
  end loop;
  return n;
end;
$$;

-- ---------- Trigger khi có phiếu mới ----------
create or replace function public.tickets_auto_assign()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.auto_assign_one(new.id);
  return new;
end;
$$;

-- Tên bắt đầu bằng 'z' để chạy SAU trigger ghi ticket_events (trg_tickets_log_status).
drop trigger if exists trg_tickets_z_auto_assign on public.tickets;
create trigger trg_tickets_z_auto_assign
  after insert on public.tickets
  for each row
  when (new.status = 'open' and new.assigned_to is null)
  execute function public.tickets_auto_assign();

-- ---------- RPC cho web ----------
create or replace function public.set_auto_assign(p_enabled boolean, p_eta_days int, p_fallback boolean)
returns int
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_role() is distinct from 'admin' then
    raise exception 'Chỉ Quản trị viên được đổi cài đặt tự động phân công.' using errcode = '42501';
  end if;
  if p_eta_days is not null and p_eta_days not in (1, 2, 3, 7) then
    raise exception 'ETA mặc định chỉ nhận 1, 2, 3 hoặc 7 ngày.' using errcode = '22023';
  end if;

  update public.auto_assign_settings
     set enabled = p_enabled, eta_days = p_eta_days, fallback_any = p_fallback,
         updated_at = now(), updated_by = auth.uid()
   where id;

  if p_enabled then
    return public.auto_assign_pending();
  end if;
  return 0;
end;
$$;

create or replace function public.run_auto_assign_now()
returns int
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_role() is distinct from 'admin' then
    raise exception 'Chỉ Quản trị viên được chạy phân công.' using errcode = '42501';
  end if;
  if not (select enabled from public.auto_assign_settings where id) then
    raise exception 'Tự động phân công đang tắt. Hãy bật trước khi chạy.' using errcode = '22023';
  end if;
  return public.auto_assign_pending();
end;
$$;

create or replace function public.set_my_availability(p_available boolean)
returns int
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_role() is distinct from 'technician' then
    raise exception 'Chỉ Kỹ thuật viên được đổi trạng thái sẵn sàng của mình.' using errcode = '42501';
  end if;
  insert into public.technician_settings (technician_id, is_available, updated_at)
  values (auth.uid(), p_available, now())
  on conflict (technician_id) do update set is_available = excluded.is_available, updated_at = now();

  if p_available then
    return public.auto_assign_pending();
  end if;
  return 0;
end;
$$;

create or replace function public.set_technician_availability(p_user_id uuid, p_available boolean)
returns int
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_role() is distinct from 'admin' then
    raise exception 'Chỉ Quản trị viên được đổi trạng thái của kỹ thuật viên.' using errcode = '42501';
  end if;
  if not exists (select 1 from public.profiles where id = p_user_id and role = 'technician') then
    raise exception 'Người này không phải Kỹ thuật viên.' using errcode = '22023';
  end if;
  insert into public.technician_settings (technician_id, is_available, updated_at)
  values (p_user_id, p_available, now())
  on conflict (technician_id) do update set is_available = excluded.is_available, updated_at = now();

  if p_available then
    return public.auto_assign_pending();
  end if;
  return 0;
end;
$$;

-- ---------- Quyền gọi hàm ----------
revoke all on function public.pick_technician(int, boolean)      from public, anon, authenticated;
revoke all on function public.auto_assign_one(uuid)              from public, anon, authenticated;
revoke all on function public.auto_assign_pending()              from public, anon, authenticated;
revoke all on function public.tickets_auto_assign()              from public, anon, authenticated;
revoke all on function public.set_auto_assign(boolean, int, boolean)        from public, anon;
revoke all on function public.run_auto_assign_now()                          from public, anon;
revoke all on function public.set_my_availability(boolean)                   from public, anon;
revoke all on function public.set_technician_availability(uuid, boolean)     from public, anon;
grant execute on function public.set_auto_assign(boolean, int, boolean)      to authenticated;
grant execute on function public.run_auto_assign_now()                       to authenticated;
grant execute on function public.set_my_availability(boolean)                to authenticated;
grant execute on function public.set_technician_availability(uuid, boolean)  to authenticated;

commit;

notify pgrst, 'reload schema';
