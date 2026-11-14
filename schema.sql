-- =====================================================================
-- Campus Service & Issue Reporting System - Supabase schema (DEMO)
-- Chạy toàn bộ file này trong Supabase Dashboard > SQL Editor
-- =====================================================================

-- ---------- 1. ENUM ----------
create type user_role       as enum ('student', 'admin', 'technician');
create type ticket_status   as enum ('open', 'assigned', 'in_progress', 'resolved', 'closed');
create type ticket_priority as enum ('low', 'medium', 'high');

-- ---------- 2. BẢNG ----------
create table profiles (
  id          uuid primary key references auth.users(id) on delete cascade,
  email       text,
  full_name   text,
  student_id  text,                       -- MSSV (chỉ sinh viên)
  role        user_role not null default 'student',
  created_at  timestamptz not null default now()
);

create table areas (
  id       serial primary key,
  name     text not null,
  building text
);

create table categories (
  id   serial primary key,
  name text not null
);

create table tickets (
  id           uuid primary key default gen_random_uuid(),
  created_by   uuid not null references profiles(id) on delete cascade,
  area_id      int  not null references areas(id),
  category_id  int  not null references categories(id),
  title        text not null,
  description  text,
  status       ticket_status   not null default 'open',
  priority     ticket_priority not null default 'medium',
  assigned_to  uuid references profiles(id),
  eta          timestamptz,               -- thời gian dự kiến hoàn thành
  is_public    boolean not null default false,  -- hiển thị trên bảng tin công khai
  rating       int check (rating between 1 and 5),
  feedback     text,
  created_at   timestamptz not null default now(),
  updated_at   timestamptz not null default now(),
  resolved_at  timestamptz,
  closed_at    timestamptz
);
create index on tickets (status);
create index on tickets (created_by);
create index on tickets (assigned_to);

create table ticket_images (
  id         uuid primary key default gen_random_uuid(),
  ticket_id  uuid not null references tickets(id) on delete cascade,
  path       text not null,               -- đường dẫn trong bucket ticket-images
  created_at timestamptz not null default now()
);

-- Lịch sử đổi trạng thái (dùng cho timeline)
create table ticket_events (
  id         uuid primary key default gen_random_uuid(),
  ticket_id  uuid not null references tickets(id) on delete cascade,
  status     ticket_status not null,
  actor_id   uuid,
  created_at timestamptz not null default now()
);

-- ---------- 3. HÀM & TRIGGER ----------
-- Lấy vai trò của người dùng hiện tại (security definer để tránh đệ quy RLS)
create or replace function public.current_role()
returns user_role
language sql stable security definer set search_path = public
as $$ select role from profiles where id = auth.uid() $$;

-- Tự tạo profile khi có user mới (role luôn là student; Admin/Kỹ thuật viên set bằng SQL)
create or replace function public.handle_new_user()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  insert into profiles (id, email, full_name, student_id, role)
  values (
    new.id,
    new.email,
    new.raw_user_meta_data->>'full_name',
    new.raw_user_meta_data->>'student_id',
    'student'
  );
  return new;
end $$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Cập nhật updated_at, resolved_at, closed_at và ghi ticket_events khi đổi trạng thái
create or replace function public.tickets_before_update()
returns trigger language plpgsql as $$
begin
  new.updated_at := now();
  if new.status is distinct from old.status then
    if new.status = 'resolved' then new.resolved_at := now(); end if;
    if new.status = 'closed'   then new.closed_at   := now(); end if;
  end if;
  return new;
end $$;

create trigger trg_tickets_before_update
  before update on tickets
  for each row execute function public.tickets_before_update();

create or replace function public.tickets_log_status()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if tg_op = 'INSERT' then
    insert into ticket_events (ticket_id, status, actor_id) values (new.id, new.status, auth.uid());
  elsif new.status is distinct from old.status then
    insert into ticket_events (ticket_id, status, actor_id) values (new.id, new.status, auth.uid());
  end if;
  return new;
end $$;

create trigger trg_tickets_log_status
  after insert or update on tickets
  for each row execute function public.tickets_log_status();

-- ---------- 4. ROW LEVEL SECURITY ----------
alter table profiles      enable row level security;
alter table areas         enable row level security;
alter table categories    enable row level security;
alter table tickets       enable row level security;
alter table ticket_images enable row level security;
alter table ticket_events enable row level security;

-- profiles
create policy profiles_select on profiles for select to authenticated
  using (id = auth.uid() or public.current_role() in ('admin', 'technician'));
create policy profiles_update_own on profiles for update to authenticated
  using (id = auth.uid()) with check (id = auth.uid());
-- Chặn tự đổi role: chỉ cho sửa các cột thông tin cá nhân
revoke update on profiles from authenticated;
grant  update (full_name, student_id) on profiles to authenticated;

-- areas / categories: ai đăng nhập cũng đọc được
create policy areas_select      on areas      for select to authenticated using (true);
create policy categories_select on categories for select to authenticated using (true);

-- tickets
-- Xem: của mình, được giao cho mình, công khai (bảng tin), hoặc Admin/Kỹ thuật viên (xem toàn cảnh)
create policy tickets_select on tickets for select to authenticated
  using (
    created_by = auth.uid()
    or assigned_to = auth.uid()
    or is_public
    or public.current_role() in ('admin', 'technician')
  );
-- Tạo: sinh viên, đúng chủ, trạng thái open
create policy tickets_insert on tickets for insert to authenticated
  with check (
    created_by = auth.uid()
    and status = 'open'
    and public.current_role() = 'student'
  );
-- Sửa: Admin tất cả; Kỹ thuật viên phiếu được giao; Sinh viên phiếu của mình khi đã Resolved (để nghiệm thu)
create policy tickets_update_admin on tickets for update to authenticated
  using (public.current_role() = 'admin')
  with check (public.current_role() = 'admin');
create policy tickets_update_tech on tickets for update to authenticated
  using (public.current_role() = 'technician' and assigned_to = auth.uid())
  with check (public.current_role() = 'technician' and assigned_to = auth.uid());
create policy tickets_update_student on tickets for update to authenticated
  using (created_by = auth.uid() and status = 'resolved')
  with check (created_by = auth.uid() and status = 'closed');

-- ticket_images
create policy images_select on ticket_images for select to authenticated
  using (exists (select 1 from tickets t where t.id = ticket_id));  -- kế thừa RLS của tickets
create policy images_insert on ticket_images for insert to authenticated
  with check (exists (select 1 from tickets t where t.id = ticket_id and t.created_by = auth.uid()));

-- ticket_events: chỉ đọc (trigger ghi)
create policy events_select on ticket_events for select to authenticated
  using (exists (select 1 from tickets t where t.id = ticket_id));

-- ---------- 5. STORAGE ----------
insert into storage.buckets (id, name, public)
values ('ticket-images', 'ticket-images', true)   -- public cho đơn giản khi demo
on conflict (id) do nothing;

create policy "ticket images read" on storage.objects for select
  using (bucket_id = 'ticket-images');
create policy "ticket images upload" on storage.objects for insert to authenticated
  with check (bucket_id = 'ticket-images');

-- ---------- 6. REALTIME ----------
alter publication supabase_realtime add table tickets;
alter publication supabase_realtime add table ticket_events;

-- ---------- 6b. XÓA TÀI KHOẢN (sinh viên tự xóa) ----------
-- Lưu ý: xóa user sẽ xóa luôn profile và các phiếu của user đó (on delete cascade)
create or replace function public.delete_my_account()
returns void
language plpgsql security definer set search_path = public, auth
as $$
begin
  delete from auth.users where id = auth.uid();
end $$;
revoke all on function public.delete_my_account() from public, anon;
grant execute on function public.delete_my_account() to authenticated;

-- ---------- 7. DỮ LIỆU MẪU (TẠM - sẽ chỉnh sau trong HANDOFF.md) ----------
insert into areas (name, building) values
  ('Phòng A101','Tòa A'), ('Phòng A102','Tòa A'), ('Phòng A201','Tòa A'), ('Phòng A202','Tòa A'),
  ('Thang máy Tòa A','Tòa A'), ('Sảnh Tòa A','Tòa A'),
  ('Phòng B101','Tòa B'), ('Phòng B102','Tòa B'), ('Phòng B201','Tòa B'), ('Phòng máy B203','Tòa B'),
  ('Nhà vệ sinh Tầng 1 Tòa B','Tòa B'),
  ('Phòng C101','Tòa C'), ('Phòng C202','Tòa C'), ('Thư viện','Tòa C'),
  ('Căng tin','Khu chung'), ('Nhà xe','Khu chung'), ('Sân trường','Khu chung'), ('Ký túc xá','Khu chung');

insert into categories (name) values
  ('Điện / Chiếu sáng'), ('Mạng / Wi-Fi'), ('Thiết bị phòng học'), ('Điều hòa / Quạt'),
  ('Thang máy'), ('Nước / Vệ sinh'), ('Bàn ghế / Nội thất'), ('Khác');

-- =====================================================================
-- SAU KHI CHẠY FILE NÀY - TẠO TÀI KHOẢN ADMIN / KỸ THUẬT VIÊN:
-- 1) Dashboard > Authentication > Users > Add user (tick "Auto Confirm User")
--    tạo: admin@demo.edu.vn, tech1@demo.edu.vn, tech2@demo.edu.vn, tech3@demo.edu.vn
--    (sinh viên có thể tự đăng ký trong app, hoặc tạo sẵn sv1..sv3@demo.edu.vn)
-- 2) Rồi chạy đoạn sau để gán vai trò:
--
-- update profiles set role = 'admin',      full_name = 'Phòng CSVC'    where email = 'admin@demo.edu.vn';
-- update profiles set role = 'technician', full_name = 'Kỹ thuật viên 1' where email = 'tech1@demo.edu.vn';
-- update profiles set role = 'technician', full_name = 'Kỹ thuật viên 2' where email = 'tech2@demo.edu.vn';
-- update profiles set role = 'technician', full_name = 'Kỹ thuật viên 3' where email = 'tech3@demo.edu.vn';
-- =====================================================================
