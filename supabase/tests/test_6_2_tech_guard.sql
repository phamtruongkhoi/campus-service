-- Kiểm tra trigger tickets_tech_guard (6.2). Chạy TỪNG khối (BEGIN ... ROLLBACK), không để lại dữ liệu.
-- Thay: <TECH_UUID> = id kỹ thuật viên; <TICKET_ASSIGNED> = id phiếu đang 'assigned' cho kỹ thuật viên đó;
--       <TICKET_INPROGRESS> = id phiếu 'in_progress' của chính kỹ thuật viên đó; <ADMIN_UUID> = id admin.

-- A. KT đổi priority -> KỲ VỌNG: lỗi "Kỹ thuật viên chỉ được đổi trạng thái phiếu."
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<TECH_UUID>","role":"authenticated"}', true);
update public.tickets set priority = 'high' where id = '<TICKET_ASSIGNED>';
rollback;

-- B. KT chuyển assigned -> in_progress -> KỲ VỌNG: 1 dòng cập nhật, không lỗi
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<TECH_UUID>","role":"authenticated"}', true);
update public.tickets set status = 'in_progress' where id = '<TICKET_ASSIGNED>' returning id, status;
rollback;

-- C. KT nhảy assigned -> closed -> KỲ VỌNG: lỗi "chỉ được chuyển Đã phân công -> Đang xử lý -> Đã khắc phục"
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<TECH_UUID>","role":"authenticated"}', true);
update public.tickets set status = 'closed' where id = '<TICKET_ASSIGNED>';
rollback;

-- D. KT tự đặt rating -> KỲ VỌNG: lỗi "Kỹ thuật viên chỉ được đổi trạng thái phiếu."
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<TECH_UUID>","role":"authenticated"}', true);
update public.tickets set rating = 5 where id = '<TICKET_INPROGRESS>';
rollback;

-- E. KT chuyển in_progress -> resolved -> KỲ VỌNG: thành công, resolved_at được đặt
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<TECH_UUID>","role":"authenticated"}', true);
update public.tickets set status = 'resolved' where id = '<TICKET_INPROGRESS>' returning id, status, resolved_at;
rollback;

-- F. Admin đổi priority -> KỲ VỌNG: thành công (trigger không chặn Admin)
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<ADMIN_UUID>","role":"authenticated"}', true);
update public.tickets set priority = 'high' where id = '<TICKET_ASSIGNED>' returning id, priority;
rollback;
