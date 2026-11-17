# Campus Service & Issue Reporting System

Ứng dụng Android báo cáo và theo dõi sự cố cơ sở vật chất trong trường học (demo học phần Công nghệ phần mềm nâng cao).

- **Vai trò:** Sinh viên, Admin (Phòng CSVC), Kỹ thuật viên.
- **Vòng đời phiếu:** Open -> Assigned -> In progress -> Resolved -> Closed.
- **Điểm nhấn:** cập nhật thời gian thực bằng Supabase Realtime + Kotlin Flow.

## Công nghệ
Kotlin 2.2, Jetpack Compose (Material 3), MVVM (ViewModel + StateFlow), supabase-kt 3.2.4 (Auth, Postgrest, Realtime, Storage), Ktor OkHttp. Android Studio Otter 3, AGP 9.0.1, JDK 21, minSdk 26.

## Chức năng chính
- **Sinh viên:** đăng ký/đăng nhập bằng email + OTP, bảng tin sự cố công khai, tạo phiếu (tối đa 3 ảnh), lịch sử và timeline realtime, nghiệm thu (đánh giá 1-5 sao), hồ sơ cá nhân.
- **Admin:** tổng quan và biểu đồ, điều phối (ưu tiên, kỹ thuật viên, ETA, công khai), lịch sử toàn trường có tìm kiếm và bộ lọc.
- **Kỹ thuật viên:** Maintenance Radar (theo tòa nhà), bảng công việc, lịch sử đã giải quyết.
- **Chung:** thông báo trong app khi trạng thái đổi.

## Cấu trúc thư mục
```
app/src/main/java/com/campus/service/
  data/        model, remote (SupabaseProvider), repository
  ui/          auth, student, admin, technician, profile, notification, common, navigation, theme
  util/        validate, nén ảnh, định dạng ngày
supabase/
  schema.sql, migrations/, seed/    # CSDL, RLS, trigger, dữ liệu demo
```

## Cài đặt
1. Tạo project Supabase; chạy `supabase/schema.sql`, sau đó các file trong `supabase/migrations/` theo thứ tự tên.
2. Tạo tài khoản Admin/Kỹ thuật viên trong Supabase Dashboard (Auto Confirm User) rồi gán vai trò bằng SQL (xem cuối `schema.sql`).
3. Tạo file `local.properties` (không đưa lên Git) ở thư mục gốc:
   ```
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_KEY=<publishable key>
   ```
4. Mở bằng Android Studio, Sync Gradle, chạy trên thiết bị/emulator có Internet.

> Chỉ dùng **publishable key** trong app. Không bao giờ đưa `service_role` key vào mã nguồn hay Git.

## Bảo mật
Row Level Security theo vai trò; trigger giới hạn cột sinh viên được sửa; bảng tin công khai tách riêng (`public_incidents`).

## Hạn chế đã biết / hướng phát triển
Mở lại phiếu khi chưa đạt, push notification khi app đóng, màn hình quản trị người dùng và danh mục, bộ nhớ đệm ngoại tuyến, kiểm thử tự động, lọc và phân trang phía server.
