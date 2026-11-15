-- =====================================================================
-- Migration 2.4: Bảng tin sự cố công khai (public_incidents)
-- Chạy trong Supabase Dashboard > SQL Editor SAU khi đã chạy schema.sql.
-- Mục đích:
--   1) Sinh viên chỉ thấy thông tin tối thiểu của sự cố công khai
--      (vị trí, loại lỗi, trạng thái, ETA), KHÔNG đọc được tiêu đề/mô tả/ảnh
--      của phiếu người khác.
--   2) Vẫn có Realtime (bảng thật thì postgres_changes hoạt động, view thì không).
-- =====================================================================

-- ---------- 1. Bảng bảng tin ----------
create table if not exists public_incidents (
  ticket_id     uuid primary key references tickets(id) on delete cascade,
  area_name     text not null,
  building      text,
  category_name text not null,
  status        ticket_status not null,
  eta           timestamptz,
  updated_at    timestamptz not null default now()
);

alter table public_incidents enable row level security;

create policy public_incidents_select on public_incidents
  for select to authenticated using (true);

-- ---------- 2. Trigger đồng bộ từ tickets ----------
create or replace function public.sync_public_incident()
returns trigger
language plpgsql security definer set search_path = public
as $$
declare
  a_name     text;
  a_building text;
  c_name     text;
begin
  if tg_op = 'DELETE' then
    delete from public_incidents where ticket_id = old.id;
    return old;
  end if;

  if new.is_public and new.status <> 'closed' then
    select name, building into a_name, a_building from areas where id = new.area_id;
    select name into c_name from categories where id = new.category_id;

    insert into public_incidents (ticket_id, area_name, building, category_name, status, eta, updated_at)
    values (new.id, a_name, a_building, c_name, new.status, new.eta, now())
    on conflict (ticket_id) do update set
      area_name     = excluded.area_name,
      building      = excluded.building,
      category_name = excluded.category_name,
      status        = excluded.status,
      eta           = excluded.eta,
      updated_at    = excluded.updated_at;
  else
    delete from public_incidents where ticket_id = new.id;
  end if;

  return new;
end $$;

drop trigger if exists trg_sync_public_incident on tickets;
create trigger trg_sync_public_incident
  after insert or update or delete on tickets
  for each row execute function public.sync_public_incident();

-- ---------- 3. Realtime ----------
alter publication supabase_realtime add table public_incidents;

-- ---------- 4. Siết quyền đọc bảng tickets ----------
-- Bỏ điều kiện "or is_public": sinh viên chỉ đọc phiếu của mình,
-- bảng tin công khai đọc qua public_incidents.
drop policy if exists tickets_select on tickets;
create policy tickets_select on tickets for select to authenticated
  using (
    created_by = auth.uid()
    or assigned_to = auth.uid()
    or public.current_role() in ('admin', 'technician')
  );

-- ---------- 5. Đồng bộ dữ liệu có sẵn ----------
-- Kích hoạt trigger cho các phiếu đang công khai (nếu có)
update tickets set is_public = is_public where is_public;
