-- 6.2: Chặn kỹ thuật viên sửa phiếu ngoài phạm vi công việc của mình.
-- Lý do: policy tickets_update_tech cho phép kỹ thuật viên UPDATE MỌI cột của phiếu được giao cho mình
-- (đổi priority, eta, rating, feedback, hoặc nhảy thẳng sang 'closed'). Android chỉ gọi đổi status nên
-- không bị ảnh hưởng, nhưng ai gọi API trực tiếp vẫn làm được. Trigger này đóng lỗ hổng đó.
-- Chạy trong Supabase SQL Editor. Idempotent. Chỉ tác động khi người gọi có vai trò 'technician'.
-- Admin, sinh viên (kể cả RPC reopen_ticket), và lệnh chạy không có phiên đăng nhập không bị ảnh hưởng.

begin;

create or replace function public.tickets_tech_guard()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_role() is distinct from 'technician' then
    return new;
  end if;

  -- Các cột do kỹ thuật viên không được đổi. (updated_at, resolved_at, closed_at do trigger khác đặt nên bỏ qua.)
  if new.title        is distinct from old.title
     or new.description is distinct from old.description
     or new.priority    is distinct from old.priority
     or new.eta         is distinct from old.eta
     or new.is_public   is distinct from old.is_public
     or new.rating      is distinct from old.rating
     or new.feedback    is distinct from old.feedback
     or new.created_by  is distinct from old.created_by
     or new.assigned_to is distinct from old.assigned_to
     or new.area_id     is distinct from old.area_id
     or new.category_id is distinct from old.category_id
     or new.created_at  is distinct from old.created_at then
    raise exception 'Kỹ thuật viên chỉ được đổi trạng thái phiếu.' using errcode = '42501';
  end if;

  -- Chỉ hai bước: Đã phân công -> Đang xử lý, Đang xử lý -> Đã khắc phục.
  if new.status is distinct from old.status
     and not ((old.status = 'assigned' and new.status = 'in_progress')
           or (old.status = 'in_progress' and new.status = 'resolved')) then
    raise exception 'Kỹ thuật viên chỉ được chuyển Đã phân công -> Đang xử lý -> Đã khắc phục.' using errcode = '42501';
  end if;

  return new;
end;
$$;

drop trigger if exists trg_tickets_tech_guard on public.tickets;
create trigger trg_tickets_tech_guard
  before update on public.tickets
  for each row execute function public.tickets_tech_guard();

commit;
