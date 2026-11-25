-- Kiểm tra RLS/RPC của 5.4. Chạy từng khối (BEGIN ... ROLLBACK) trong SQL Editor, KHÔNG để lại dữ liệu.
-- Thay <STUDENT_UUID>, <ADMIN_UUID>, <ADMIN2_UUID> bằng id thật (select id, email, role from profiles;).
-- Mỗi khối: kỳ vọng ghi ở dòng chú thích.

-- A. Sinh viên thêm khu vực -> KỲ VỌNG: lỗi "new row violates row-level security policy"
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<STUDENT_UUID>","role":"authenticated"}', true);
insert into public.areas (name, building) values ('Khu thử nghiệm', 'Tòa Z');
rollback;

-- B. Admin thêm khu vực -> KỲ VỌNG: thành công (1 dòng)
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<ADMIN_UUID>","role":"authenticated"}', true);
insert into public.areas (name, building) values ('Khu thử nghiệm', 'Tòa Z') returning id, name;
rollback;

-- C. Admin tự đổi vai trò mình -> KỲ VỌNG: lỗi "Không thể tự đổi vai trò của chính mình."
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<ADMIN_UUID>","role":"authenticated"}', true);
select public.set_user_role('<ADMIN_UUID>'::uuid, 'student', 'Thử nghiệm tự đổi');
rollback;

-- D. Sinh viên gọi RPC -> KỲ VỌNG: lỗi "Chỉ Quản trị viên mới được đổi vai trò."
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<STUDENT_UUID>","role":"authenticated"}', true);
select public.set_user_role('<STUDENT_UUID>'::uuid, 'technician', 'Thử nghiệm sinh viên tự nâng');
rollback;

-- E. Admin nâng sinh viên lên kỹ thuật viên -> KỲ VỌNG: role = technician, có 1 dòng role_changes (rồi rollback)
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<ADMIN_UUID>","role":"authenticated"}', true);
select public.set_user_role('<STUDENT_UUID>'::uuid, 'technician', 'Theo quyết định số 01/TEST');
select role from public.profiles where id = '<STUDENT_UUID>';
select old_role, new_role, note from public.role_changes order by id desc limit 1;
rollback;

-- F. Admin đổi vai trò Admin khác -> KỲ VỌNG: lỗi "Không thể đổi vai trò của Quản trị viên."
begin;
set local role authenticated;
select set_config('request.jwt.claims', '{"sub":"<ADMIN_UUID>","role":"authenticated"}', true);
select public.set_user_role('<ADMIN2_UUID>'::uuid, 'student', 'Thử nghiệm đổi admin');
rollback;
