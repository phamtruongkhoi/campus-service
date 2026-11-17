# Kịch bản demo (5 phút) - Campus Service & Issue Reporting System

## 1. Chuẩn bị (xong trước giờ demo 30 phút)
- [ ] Pin đầy, cắm sạc các thiết bị; tắt chế độ tiết kiệm pin, tắt thông báo làm phiền (Không làm phiền).
- [ ] Mạng: **hotspot 4G của điện thoại dự phòng đã bật sẵn**, mọi thiết bị đã kết nối và mở thử được trang web. Không phụ thuộc Wi-Fi trường.
- [ ] Đã nạp dữ liệu demo (supabase/seed/demo_data.sql), bảng tin có phiếu công khai, Tổng quan Admin có số liệu.
- [ ] Thiết bị A (sinh viên): đã đăng nhập tài khoản sinh viên demo, đang ở tab Trang chủ.
- [ ] Thiết bị B (Admin): đã đăng nhập `admin@demo.edu.vn`, đang ở tab Tổng quan.
- [ ] Thiết bị C (kỹ thuật viên, nếu có): đã đăng nhập `tech1@demo.edu.vn`, đang ở tab Radar. Nếu chỉ có 2 thiết bị: dùng thiết bị B, đăng xuất Admin và đăng nhập tech1 khi tới bước 4 (mất khoảng 20 giây, đã tính trong kịch bản).
- [ ] 1-2 ảnh thật sẵn trong thư viện ảnh của thiết bị A để đính kèm phiếu.
- [ ] Mật khẩu các tài khoản ghi trên giấy (không ghi vào repo).
- [ ] Video dự phòng đã chép vào điện thoại, máy tính và Drive; thử mở được.
- [ ] APK dự phòng (campus-service-v1.0-demo.apk) cài sẵn trên 1 điện thoại thật.
- [ ] Xem lại bảng kiểm thử TC01-TC14 trong HANDOFF.md (đã điền kết quả).

## 2. Dòng thời gian
| Thời gian | Ai/thiết bị | Việc làm | Điều giảng viên nên thấy / lời nói |
|---|---|---|---|
| 0:00-0:30 | Nói | Giới thiệu | Bài toán: báo cáo sự cố cơ sở vật chất bằng giấy, chậm, khó theo dõi. Hệ thống có 3 vai trò: Sinh viên, Phòng CSVC (Admin), Kỹ thuật viên. Android (Kotlin, Jetpack Compose) + Supabase (Postgres, Auth, Storage, Realtime). Điểm nhấn: cập nhật **thời gian thực**. |
| 0:30-1:00 | A (sinh viên) | Trang chủ: bảng tin sự cố công khai | Sinh viên thấy vị trí, trạng thái, ETA của sự cố đang xử lý (không lộ nội dung phiếu người khác). |
| 1:00-1:40 | A | Tạo phiếu: chọn khu vực, loại lỗi, tiêu đề, mô tả, đính kèm 1-2 ảnh, Gửi | Phiếu xuất hiện ở Lịch sử, trạng thái **Chờ tiếp nhận**. |
| 1:40-2:30 | B (Admin) | Nhận thông báo "Có phiếu mới"; Tổng quan tăng số liệu; tab Điều phối: mở phiếu, chọn ưu tiên Cao, giao tech1, ETA 2 ngày, bật công khai, Lưu | Nhìn sang máy A: **trạng thái tự đổi sang Đã phân công, hiện ETA và thông báo, không cần tải lại**. Đây là điểm nhấn Realtime + Flow. |
| 2:30-3:20 | C (kỹ thuật viên) | Radar (toàn cảnh theo tòa nhà, khối lượng việc); tab Công việc: mở phiếu, Nhận việc, rồi Báo đã khắc phục | Máy A và máy B đổi trạng thái ngay (Đang xử lý rồi Đã khắc phục). |
| 3:20-4:00 | A | Mở phiếu: khung Nghiệm thu, chọn 5 sao + nhận xét, Đóng phiếu | Phiếu **Đã đóng**, timeline đủ 5 mốc; kỹ thuật viên nhận thông báo đã được nghiệm thu. |
| 4:00-4:30 | B (Admin) | Lịch sử: tìm kiếm, lọc theo trạng thái/ưu tiên, mở phiếu xem đánh giá; Tổng quan cập nhật biểu đồ | Quản lý toàn trường và thống kê. |
| 4:30-5:00 | Nói | Tổng kết | Bảo mật bằng RLS và trigger ở tầng CSDL; kiến trúc MVVM + Repository; hạn chế và hướng phát triển (mở lại phiếu khi chưa đạt, push notification, màn hình quản trị). |

Nếu bị cắt thời gian: bỏ bước 0:30-1:00 và 4:00-4:30, giữ nguyên chuỗi Sinh viên -> Admin -> Kỹ thuật viên -> Sinh viên (luồng chính, thể hiện Realtime).

## 3. Điểm nhấn kỹ thuật (nói ngắn gọn khi có cơ hội)
- **Realtime:** Supabase Realtime (Postgres changes) bọc thành Kotlin Flow (callbackFlow); ViewModel debounce 400 ms rồi tải lại; chỉ lắng nghe khi màn hình đang hiển thị.
- **Bảo mật:** Row Level Security theo vai trò; vai trò do CSDL quyết định, không do app; trigger giới hạn cột sinh viên được sửa; bảng tin công khai tách riêng (public_incidents) để không lộ dữ liệu.
- **Vòng đời 5 bước:** Open -> Assigned -> In progress -> Resolved -> Closed, mỗi lần đổi trạng thái được trigger ghi vào ticket_events (timeline).
- **Kiểm thử:** 14 ca kiểm thử toàn luồng trên nhiều thiết bị (TC01-TC14).

## 4. Kế hoạch dự phòng
| Sự cố | Cách xử lý |
|---|---|
| Mất mạng / Supabase không phản hồi | Chuyển sang hotspot 4G; nếu vẫn không được, **mở video dự phòng** và thuyết minh theo kịch bản |
| Không nhận được email OTP | Không demo đăng ký; dùng tài khoản đã có. Nói rõ: giới hạn gửi email mặc định, sẽ cấu hình SMTP riêng khi triển khai |
| App crash trên 1 thiết bị | Mở lại app (phiên đăng nhập được lưu); nếu không được dùng thiết bị cài APK dự phòng |
| Realtime không cập nhật | Bấm "Làm mới" ở màn hình đó, nói rõ đây là lớp dự phòng, rồi tiếp tục |
| Quên mật khẩu | Dùng tài khoản dự phòng (admin2@, admin3@demo.edu.vn); tài khoản sinh viên/kỹ thuật viên đặt lại trên Supabase Dashboard |

## 5. Câu hỏi dự kiến và gợi ý trả lời
1. **Vì sao chọn Supabase?** Có sẵn Auth, Postgres, Storage, Realtime trong một nền tảng; RLS cho phân quyền ở tầng dữ liệu; phù hợp thời gian ngắn.
2. **Realtime hoạt động thế nào?** Supabase phát thay đổi của bảng qua WebSocket; app bọc thành Flow, nhận tín hiệu rồi tải lại dữ liệu theo quyền của người dùng.
3. **Bảo mật ra sao?** RLS theo vai trò, trigger guard cho sinh viên, bảng tin công khai chỉ chứa thông tin tối thiểu; khóa trong app là khóa công khai (publishable), không có khóa quản trị.
4. **Vì sao không dùng Hilt / Navigation Compose / push?** Giảm rủi ro build trong 3 ngày (ADR-01, ADR-09); Repository đã tách nên có thể chuyển sau.
5. **Nếu mất mạng thì sao?** App báo lỗi rõ ràng, không crash; chưa có bộ nhớ đệm ngoại tuyến (hướng phát triển).
6. **Làm sao biết hệ thống đúng?** 14 ca kiểm thử toàn luồng, đối chiếu số liệu giao diện với truy vấn SQL.
7. **Khác biệt so với yêu cầu ban đầu?** ETA chọn theo mốc ngày; gộp "nhận việc" và "bắt đầu xử lý" thành một thao tác; thông báo chỉ trong app. Đều có lý do và đã ghi trong tài liệu.
