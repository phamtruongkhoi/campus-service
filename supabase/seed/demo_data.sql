-- =====================================================================
-- Dữ liệu demo (mục 3.6): 15 phiếu đủ trạng thái, đủ tòa nhà
-- Chạy trong Supabase Dashboard > SQL Editor.
-- Điều kiện: đã có tech1, tech2, tech3@demo.edu.vn (role technician)
--            và ít nhất 1 sinh viên (dùng tối đa 3 sinh viên đầu tiên làm chủ phiếu).
-- =====================================================================

-- BƯỚC 1 (TÙY CHỌN, MẶC ĐỊNH ĐANG TẮT): xóa TOÀN BỘ phiếu hiện có
-- (kèm sự kiện, ảnh liên kết, bảng tin công khai) để màn hình sạch.
-- Bỏ dấu "--" ở dòng dưới nếu thật sự muốn xóa.
-- delete from tickets;

-- BƯỚC 2: nạp dữ liệu demo
do $$
declare
  stu uuid[];
  tec uuid[];
  titles text[] := array[
    'Bóng đèn phòng A101 nhấp nháy',
    'Wi-Fi phòng A201 rất chậm',
    'Máy chiếu phòng B101 không lên hình',
    'Điều hòa phòng B201 không mát',
    'Thang máy Tòa A kẹt cửa tầng 3',
    'Vòi nước nhà vệ sinh tầng 1 Tòa B bị rò',
    'Gãy chân bàn phòng C101',
    'Mất kết nối mạng phòng máy B203',
    'Quạt trần phòng A102 kêu to',
    'Ổ cắm điện phòng C202 bị cháy',
    'Cửa thư viện khó đóng',
    'Đèn sân trường không sáng buổi tối',
    'Rác đầy ở căng tin',
    'Bảng trắng phòng A202 bị hỏng',
    'Nhà xe thiếu đèn chiếu sáng'
  ];
  descs text[] := array[
    'Bóng đèn giữa phòng nhấp nháy liên tục, ảnh hưởng buổi học.',
    'Tốc độ Wi-Fi rất thấp, nhiều sinh viên không vào được hệ thống học trực tuyến.',
    'Máy chiếu bật lên nhưng không có hình, đã thử đổi cáp HDMI.',
    'Điều hòa chạy nhưng chỉ ra gió nóng, phòng rất oi.',
    'Cửa thang máy tầng 3 đóng mở không hết, có tiếng kêu lạ.',
    'Vòi rửa tay bị rò nước liên tục, sàn nhà vệ sinh ướt trơn.',
    'Một chân bàn bị gãy, bàn bị nghiêng khi ngồi.',
    'Toàn bộ máy trong phòng máy mất mạng từ đầu giờ sáng.',
    'Quạt trần phát tiếng kêu lớn khi chạy ở tốc độ cao.',
    'Ổ cắm gần bàn giáo viên có mùi khét và bị đen.',
    'Cửa chính thư viện nặng, khó đóng hết, gió lùa vào.',
    'Hai trụ đèn gần sân bóng không sáng vào buổi tối.',
    'Thùng rác ở căng tin đầy, có mùi, cần thu gom.',
    'Bảng trắng bị trầy, không viết xóa được.',
    'Khu nhà xe phía sau thiếu đèn, buổi tối rất tối.'
  ];
  areas int[] := array[1,3,7,9,5,11,12,10,2,13,14,17,15,4,16];
  cats  int[] := array[1,2,3,4,5,6,7,2,4,1,8,1,6,3,1];
  prios text[] := array['medium','high','high','medium','high','medium','low','high','low','high','low','medium','low','medium','medium'];
  stats text[] := array['open','open','assigned','in_progress','in_progress','assigned','open','resolved','closed','resolved','closed','in_progress','open','closed','assigned'];
  seeded uuid[] := '{}';
  i int;
  tid uuid;
  st text;
  tech uuid;
  stu_id uuid;
begin
  select array_agg(id) into stu
    from (select id from profiles where role = 'student' order by created_at limit 3) s;
  select array_agg(id order by email) into tec
    from profiles where email in ('tech1@demo.edu.vn','tech2@demo.edu.vn','tech3@demo.edu.vn');

  if stu is null or tec is null or array_length(tec, 1) < 3 then
    raise exception 'Thiếu sinh viên hoặc chưa đủ 3 kỹ thuật viên tech1..tech3';
  end if;

  for i in 1..array_length(titles, 1) loop
    stu_id := stu[1 + (i % array_length(stu, 1))];
    tech   := tec[1 + (i % 3)];
    st     := stats[i];

    insert into tickets (created_by, area_id, category_id, title, description,
                         status, priority, is_public, created_at)
    values (stu_id, areas[i], cats[i], titles[i], descs[i],
            'open', prios[i]::ticket_priority, (i % 3 = 0),
            now() - (interval '9 hours' * i))
    returning id into tid;
    seeded := seeded || tid;

    if st <> 'open' then
      update tickets
         set assigned_to = tech,
             eta = now() + (interval '1 day' * (1 + (i % 3))),
             status = 'assigned'
       where id = tid;
    end if;
    if st in ('in_progress', 'resolved', 'closed') then
      update tickets set status = 'in_progress' where id = tid;
    end if;
    if st in ('resolved', 'closed') then
      update tickets set status = 'resolved' where id = tid;
    end if;
    if st = 'closed' then
      update tickets
         set status = 'closed',
             rating = case when i % 2 = 0 then 5 else 4 end,
             feedback = case when i % 2 = 0 then 'Xử lý nhanh, cảm ơn phòng CSVC.'
                             else 'Đã khắc phục tốt.' end
       where id = tid;
    end if;
  end loop;

  -- Chỉnh mốc thời gian timeline cho tự nhiên (trigger ghi cùng một giờ)
  update ticket_events e
     set created_at = t.created_at + case e.status::text
           when 'open' then interval '0 minutes'
           when 'assigned' then interval '2 hours'
           when 'in_progress' then interval '5 hours'
           when 'resolved' then interval '1 day'
           else interval '1 day 6 hours' end
    from tickets t
   where t.id = e.ticket_id and e.ticket_id = any(seeded);
end $$;

-- BƯỚC 3: kiểm tra (nếu đã xóa hết phiếu ở bước 1, phải ra:
-- open 4, assigned 3, in_progress 3, resolved 2, closed 3, tổng 15)
select status, count(*) from tickets group by status order by status;
