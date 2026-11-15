-- =====================================================================
-- Migration 3.4: Siết quyền cập nhật phiếu của sinh viên
-- Chạy trong Supabase Dashboard > SQL Editor.
-- Mục đích: policy tickets_update_student cho phép sinh viên sửa phiếu của mình
-- khi Resolved -> Closed, nhưng không giới hạn cột. Trigger này giữ nguyên mọi
-- cột trừ status, rating, feedback khi người cập nhật là sinh viên.
-- (Admin/Kỹ thuật viên và SQL Editor không bị ảnh hưởng.)
-- =====================================================================
create or replace function public.tickets_student_guard()
returns trigger
language plpgsql security definer set search_path = public
as $$
begin
  if public.current_role() = 'student' then
    new.created_by  := old.created_by;
    new.area_id     := old.area_id;
    new.category_id := old.category_id;
    new.title       := old.title;
    new.description := old.description;
    new.priority    := old.priority;
    new.assigned_to := old.assigned_to;
    new.eta         := old.eta;
    new.is_public   := old.is_public;
  end if;
  return new;
end $$;

drop trigger if exists trg_tickets_student_guard on tickets;
create trigger trg_tickets_student_guard
  before update on tickets
  for each row execute function public.tickets_student_guard();
