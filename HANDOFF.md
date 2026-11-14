# HANDOFF – Campus Service & Issue Reporting System (Demo)

> File này là nguồn thông tin chung của dự án. Khi có thay đổi, cập nhật tại đây và gửi lại cho Claude để chỉnh code/kế hoạch theo.

## 1. Mục tiêu & phạm vi

- **Mục đích:** Demo trước giảng viên.
- **Nền tảng:** App Android (Kotlin). Bản web để sau.
- **Phạm vi:** **Đầy đủ theo PRD ban đầu**, không cắt giảm tính năng. Làm theo chiều rộng: đủ 3 vai trò (Sinh viên, Admin, Kỹ thuật viên) với toàn bộ module trong PRD.
- **Luồng chính khi demo:**
  Sinh viên tạo phiếu → Admin phân công → Kỹ thuật viên xử lý → Sinh viên nghiệm thu/đánh giá (cập nhật realtime ở mọi bước).

## 2. Các quyết định đã chốt

| Hạng mục | Quyết định |
|---|---|
| UI | Jetpack Compose (Material 3) |
| Kiến trúc | MVVM + Hilt + Kotlin Coroutines/Flow (StateFlow/SharedFlow) |
| Backend | Supabase (PostgreSQL, Auth, Storage, Realtime) |
| OTP | Chỉ qua **Email** (Supabase Auth). Không làm SMS |
| Tài khoản | Sinh viên tự đăng ký. **Admin & Kỹ thuật viên được seed sẵn** (giải thích khi demo: do bộ phận dữ liệu của trường cấp) |
| Mức ưu tiên | 3 cấp: Thấp / Trung bình / Cao |
| Bảng tin công khai | Admin có công tắc "công khai" và nhập ETA khi phân công |
| Biểu đồ Admin | Cơ bản: số ticket theo trạng thái, theo loại lỗi (sẽ bổ sung sau) |
| Ảnh đính kèm | Tối đa 3 ảnh/phiếu, lưu Supabase Storage |
| Vòng đời ticket | Open → Assigned → In Progress → Resolved → Closed |
| Xóa tài khoản | Qua hàm SQL `delete_my_account()` (xóa luôn phiếu của sinh viên đó, chấp nhận được cho demo) |
| Quyền Profile Kỹ thuật viên | Hạn chế ở mức giao diện: chỉ xem thông tin và đăng xuất |

## 3. Dữ liệu mẫu (TẠM – chủ dự án sẽ bổ sung/chỉnh sửa)

### Khu vực / phòng
- Tòa A: Phòng A101, A102, A201, A202, Thang máy Tòa A, Sảnh Tòa A
- Tòa B: Phòng B101, B102, B201, Phòng máy B203, Nhà vệ sinh Tầng 1 Tòa B
- Tòa C: Phòng C101, C202, Thư viện
- Khu chung: Căng tin, Nhà xe, Sân trường, Ký túc xá

### Loại lỗi
- Điện / Chiếu sáng
- Mạng / Wi-Fi
- Thiết bị phòng học (máy chiếu, micro, bảng)
- Điều hòa / Quạt
- Thang máy
- Nước / Vệ sinh
- Bàn ghế / Nội thất
- Khác

### Tài khoản seed (TẠM)
- 1 Admin: admin@demo.edu.vn
- 3 Kỹ thuật viên: tech1@demo.edu.vn, tech2@demo.edu.vn, tech3@demo.edu.vn
- 3 Sinh viên: sv1@demo.edu.vn, sv2@demo.edu.vn, sv3@demo.edu.vn
- (Mật khẩu sẽ đặt khi tạo, ghi lại ở nơi an toàn, KHÔNG commit lên git)

## 4. Việc cần bổ sung sau (TODO của chủ dự án)

- [ ] Danh sách phòng/khu vực thật của trường
- [ ] Danh sách loại lỗi thật
- [ ] Chốt danh sách biểu đồ thống kê của Admin (hiện chỉ làm cơ bản)
- [x] Tạo project Supabase, chạy `schema.sql` (không lỗi), cung cấp Project URL + anon (publishable) key. **Khóa lưu trong `local.properties` của app, KHÔNG ghi vào file này và KHÔNG commit.** (KHÔNG bao giờ gửi `service_role`/secret key.)
- [x] Phiên bản Android Studio: **Otter 3 Feature Drop (2025.2.3)**
- [ ] **Sửa template email OTP** (Authentication → Email Templates → *Confirm signup* và *Reset password* phải chứa `{{ .Token }}`). Hiện tại email vẫn gửi **link** thay vì mã OTP. Phải sửa xong trước khi làm mục 1.4. 📝 Ghi vào báo cáo (Chương 2.4 Rủi ro): vì sao ban đầu gửi link, đã sửa thế nào.
- [ ] Cấu hình SMTP riêng (Gmail App Password) trước ngày demo, đăng ký sẵn tài khoản sinh viên

## 4b. Phiên bản công cụ/thư viện đã tra cứu (14/11/2026)

> Otter 3 hỗ trợ tối đa **AGP 9.0** (Gradle tối thiểu 9.1.0). Dagger/Hilt từ 2.59 trở đi **bắt buộc AGP 9**, nên bộ này khớp với Otter.

| Thư viện | Phiên bản | Ghi chú |
|---|---|---|
| Android Gradle Plugin | 9.0.x (do wizard của Otter tự chọn) | Không tự nâng lên 9.5 vì Otter không hỗ trợ |
| supabase-kt | 3.9.0 (nếu lỗi sync thì thử 3.8.0) | Group `io.github.jan-tennert.supabase`, dùng BOM |
| Ktor engine | cùng dòng Ktor 3.x với supabase-kt | supabase-kt 3.2.4 dùng Ktor 3.3.0; nếu lỗi runtime thì báo Claude |
| Dagger/Hilt | 2.60.1 | Hilt Gradle Plugin cho AGP 9 |
| androidx.hilt | 1.4.0 | Compose `hiltViewModel()` nằm ở artifact riêng từ 1.3.0 |
| KSP | **chờ khớp với Kotlin của project** | Không đoán; lấy sau khi có `libs.versions.toml` do wizard sinh ra |
| androidx.core (core-ktx) | **1.17.0** (ghim lại) | Wizard sinh 1.19.1 cần AGP 9.1+ và compileSdk 37, Otter không hỗ trợ |
| androidx.lifecycle | **2.10.0** (ghim lại) | 2.11.0 cần AGP 9.2+ và compileSdk 37 |

> 📝 **Sự cố 14/11 (ghi vào báo cáo Chương 2.4 Rủi ro):** sau khi tạo project, Gradle báo 6 lỗi AAR metadata do thư viện AndroidX mới hơn AGP 9.0.1 của Android Studio Otter. Cách xử lý: ghim `core-ktx` về 1.17.0 và `lifecycle` về 2.10.0 trong `libs.versions.toml`, **không** nâng AGP. Bài học: không để wizard/IDE tự chọn phiên bản mới nhất khi công cụ build chưa theo kịp.

## 5. LỊCH TRÌNH CHI TIẾT: 3 NGÀY x 8 GIỜ (14/11 – 16/11/2026)

> Quỹ thời gian: 24 giờ cho toàn bộ phạm vi PRD. Mỗi việc có số giờ dự kiến (đã gồm thời gian test nhanh ngay sau khi làm xong). Tổng mỗi ngày đúng 8 giờ, **không có giờ đệm**, nên nếu một việc bị trễ thì dồn vào mục "Hoàn thiện" của ngày 3 trước, chứ không bỏ tính năng.

### NGÀY 1 (Thứ Bảy 14/11): Nền tảng + Xác thực + Khung 3 vai trò + Sinh viên tạo phiếu

| # | Việc | Giờ | Kết quả kiểm tra được |
|---|---|---|---|
| 1.1 | **Supabase:** tạo project, chạy `schema.sql`, tạo tài khoản Admin/Kỹ thuật viên và gán vai trò, cấu hình SMTP + template email dùng `{{ .Token }}` | 0.75h | Bảng và dữ liệu mẫu có trong Table Editor; gửi được email OTP thử |
| 1.2 | **Project Android:** Gradle, Compose M3, Hilt, supabase-kt (Auth, Postgrest, Realtime, Storage), theme, cấu trúc package (data / domain / ui), module khởi tạo Supabase client | 1.0h | App chạy, gọi được Supabase (đọc bảng `areas`) |
| 1.3 | **Đăng nhập + Đăng ký:** form, validate, nhập họ tên + MSSV + email + mật khẩu | 1.25h | Gửi được yêu cầu đăng ký, đăng nhập đúng/sai báo lỗi rõ |
| 1.4 | **OTP:** xác thực OTP khi đăng ký; Quên mật khẩu (nhập email → OTP → đặt mật khẩu mới) | 1.25h | Đăng ký xong nhận OTP, nhập đúng vào được app; quên mật khẩu đổi được |
| 1.5 | **Phiên đăng nhập + phân quyền:** lưu session, tự đăng nhập lại, đọc role từ `profiles`, Navigation + bottom bar riêng cho 3 vai trò | 1.0h | 3 tài khoản mẫu đăng nhập vào 3 giao diện khác nhau |
| 1.6 | **Profile Sinh viên:** xem/sửa họ tên và MSSV, đổi mật khẩu, đăng xuất, xóa tài khoản | 1.5h | Đủ 4 chức năng chạy thật; xóa tài khoản đưa về màn đăng nhập |
| 1.7 | **Sinh viên: form tạo phiếu** (chọn khu vực, loại lỗi, tiêu đề, mô tả), lưu vào DB (chưa ảnh) | 1.25h | Tạo phiếu xong thấy dòng mới trong bảng `tickets` |

**Cuối ngày 1:** đủ Module 1 (xác thực + phân quyền), Profile sinh viên, tạo phiếu cơ bản.

### NGÀY 2 (Chủ Nhật 15/11): Sinh viên hoàn chỉnh + Toàn bộ Admin

| # | Việc | Giờ | Kết quả kiểm tra được |
|---|---|---|---|
| 2.1 | **Upload ≤ 3 ảnh:** chọn ảnh từ máy, nén, upload Storage, lưu `ticket_images` | 1.0h | Tạo phiếu kèm ảnh, ảnh hiển thị lại được |
| 2.2 | **Danh sách phiếu + Lịch sử + Chi tiết phiếu** của sinh viên | 1.0h | Xem lại mọi phiếu đã gửi, mở được chi tiết có ảnh |
| 2.3 | **Theo dõi realtime:** timeline trạng thái từ `ticket_events`, Repository + Supabase Realtime + Flow | 1.0h | Đổi trạng thái trong DB thì màn hình sinh viên tự cập nhật |
| 2.4 | **Trang chủ sinh viên – Bảng tin công khai:** vị trí, trạng thái, ETA, cập nhật realtime | 1.25h | Admin bật công khai một phiếu thì bảng tin tự hiện thêm |
| 2.5 | **Admin Dashboard:** tổng quan toàn bộ ticket + biểu đồ (theo trạng thái, theo loại lỗi) | 1.25h | Số liệu và biểu đồ khớp dữ liệu, tự cập nhật khi có phiếu mới |
| 2.6 | **Admin Điều phối:** xem phiếu mới, đặt mức ưu tiên, phân công kỹ thuật viên cụ thể, nhập ETA, công tắc công khai | 1.5h | Phân công xong, sinh viên thấy trạng thái Assigned realtime |
| 2.7 | **Admin Lịch sử toàn trường:** tìm kiếm, lọc (trạng thái, loại lỗi, khu vực, ưu tiên) + Profile Admin (xem thông tin, đăng xuất) | 1.0h | Lọc ra đúng kết quả; Profile Admin hoạt động |

**Cuối ngày 2:** hoàn chỉnh vai trò Sinh viên (trừ nghiệm thu) và vai trò Admin.

### NGÀY 3 (Thứ Hai 16/11): Kỹ thuật viên + Nghiệm thu + Hoàn thiện + Chuẩn bị demo

| # | Việc | Giờ | Kết quả kiểm tra được |
|---|---|---|---|
| 3.1 | **Kỹ thuật viên – Bảng công việc:** danh sách phiếu được giao, chi tiết, xác nhận tiếp nhận, cập nhật Đang xử lý / Đã hoàn thành | 1.5h | Đổi trạng thái thì Admin và sinh viên thấy ngay |
| 3.2 | **Kỹ thuật viên – Maintenance Radar:** toàn cảnh sự cố toàn trường, khối lượng công việc chung, đồng nghiệp đang làm ở khu vực lân cận (nhóm theo khu vực), realtime | 1.5h | Thấy được sự cố và đồng nghiệp theo khu vực, tự cập nhật |
| 3.3 | **Kỹ thuật viên – Lịch sử** (sự cố bản thân đã giải quyết) **+ Profile hạn chế quyền** | 0.75h | Lịch sử đúng; Profile chỉ xem và đăng xuất |
| 3.4 | **Sinh viên – Nghiệm thu:** đánh giá sao + bình luận, đóng phiếu (Resolved → Closed) | 1.0h | Phiếu đóng, đánh giá lưu lại và Admin xem được |
| 3.5 | **Hoàn thiện:** trạng thái loading / lỗi / rỗng, validate form, thông báo trong app khi trạng thái đổi, đồng nhất giao diện | 1.25h | Không crash ở các luồng chính, thông báo lỗi dễ hiểu |
| 3.6 | **Test toàn luồng** với 3 tài khoản trên 2–3 thiết bị/giả lập, sửa lỗi, nạp dữ liệu demo đẹp (nhiều phiếu đủ trạng thái, có ảnh) | 1.25h | Chạy trọn vòng đời 5 bước không lỗi |
| 3.7 | **Chuẩn bị demo:** build APK, viết kịch bản 3–5 phút, quay video dự phòng, tập trình bày | 0.75h | Có APK, kịch bản và video |

**Cuối ngày 3:** đủ 3 vai trò, vòng đời 5 bước, realtime, sẵn sàng demo.

### Tổng hợp giờ

| Ngày | Giờ |
|---|---|
| Ngày 1 | 0.75 + 1.0 + 1.25 + 1.25 + 1.0 + 1.5 + 1.25 = **8.0h** |
| Ngày 2 | 1.0 + 1.0 + 1.0 + 1.25 + 1.25 + 1.5 + 1.0 = **8.0h** |
| Ngày 3 | 1.5 + 1.5 + 0.75 + 1.0 + 1.25 + 1.25 + 0.75 = **8.0h** |

### Rủi ro cần biết trước
- **Lỗi môi trường** (Gradle, thư viện Supabase, emulator) thường ngốn nhiều giờ hơn dự tính, nhất là ngày 1. Nếu gặp lỗi lạ, gửi nguyên văn lỗi cho Claude ngay thay vì tự mò.
- **Không có giờ đệm.** Nếu trễ, phần "Hoàn thiện" (3.5) co lại trước, không cắt tính năng. Nên giữ nhịp theo bảng giờ ở trên.
- **Kịch bản demo nên có 2–3 thiết bị/giả lập** để show realtime giữa các vai trò.

### Kịch bản demo gợi ý
1. Sinh viên mở bảng tin, thấy "Thang máy Tòa A đang sửa" nên không gửi trùng.
2. Sinh viên tạo phiếu mới (kèm ảnh) về lỗi khác.
3. (Máy 2) Admin thấy phiếu xuất hiện ngay, đặt ưu tiên, phân công, nhập ETA, bật công khai.
4. (Máy 3) Kỹ thuật viên nhận việc, chuyển Đang xử lý, rồi Đã khắc phục. Radar hiển thị đồng nghiệp ở khu vực lân cận.
5. Sinh viên thấy trạng thái đổi realtime, nghiệm thu, đánh giá, phiếu Đóng.
6. Admin xem biểu đồ thống kê và lịch sử.

### Lưu ý kỹ thuật quan trọng
- **Gửi OTP email:** dịch vụ email mặc định của Supabase giới hạn rất ít email mỗi giờ. Để demo không bị nghẽn: cấu hình SMTP riêng (Gmail App Password hoặc Resend) trong Authentication > SMTP, và **đăng ký sẵn tài khoản sinh viên trước giờ demo**. Template email (Authentication > Email Templates) cần dùng `{{ .Token }}` để gửi mã OTP thay vì link.
- **Bảng tin công khai:** phiếu có `is_public = true` mọi người dùng đăng nhập đều đọc được (đủ cho demo; bản thật nên tách view chỉ lộ vị trí/trạng thái/ETA).
- **Bucket ảnh** đang để public cho đơn giản khi demo.

## 5c. Minh chứng cần chụp / ghi lại cho Báo cáo Tiểu luận (Công nghệ phần mềm nâng cao)

> Báo cáo theo mẫu MAUBAOCAO_CNPMNC (11 chương + kết luận + phụ lục). Nộp: file báo cáo + sản phẩm demo ở Tuần 12.
> Quy ước đánh dấu trong hướng dẫn của Claude:
> - 📸 = chụp màn hình thật (đặt tên file theo mã, ví dụ `H03_tao_phieu.png`)
> - 🖊️ = hình tự vẽ (Claude có thể sinh sẵn bằng Mermaid/PlantUML, không cần chụp)
> - 📝 = cần ghi chép lại ngay khi làm để điền bảng trong báo cáo
> - **Không chụp lộ:** mật khẩu, Project URL, anon/service key, App password, SMTP.

### A. Ảnh app đang chạy (Chương 11.1 – minh chứng chức năng; hỗ trợ Chương 3, 4)
| Mã | Nội dung ảnh | Chụp ở bước |
|---|---|---|
| H01 | Màn Đăng nhập, Đăng ký, nhập OTP, Quên mật khẩu | 1.3 – 1.4 |
| H02 | Giao diện 3 vai trò sau đăng nhập (Sinh viên / Admin / Kỹ thuật viên) | 1.5 |
| H03 | Profile sinh viên (sửa thông tin, đổi mật khẩu, xóa tài khoản) | 1.6 |
| H04 | Form tạo phiếu (có ảnh đính kèm) | 1.7 + 2.1 |
| H05 | Lịch sử phiếu + Chi tiết phiếu + Timeline trạng thái | 2.2 – 2.3 |
| H06 | Trang chủ sinh viên: Bảng tin công khai | 2.4 |
| H07 | Admin Dashboard + biểu đồ thống kê | 2.5 |
| H08 | Admin Điều phối (ưu tiên, phân công, ETA, công tắc công khai) | 2.6 |
| H09 | Admin Lịch sử + bộ lọc | 2.7 |
| H10 | Kỹ thuật viên: Bảng công việc + cập nhật trạng thái | 3.1 |
| H11 | Maintenance Radar | 3.2 |
| H12 | Sinh viên nghiệm thu + đánh giá | 3.4 |

### B. Quản lý mã nguồn (Chương 5, Phụ lục C) – chụp ngay khi có, đừng để cuối
| Mã | Nội dung ảnh |
|---|---|
| G01 | Cấu trúc repository (cây thư mục + README) |
| G02 | Danh sách Issues (mỗi việc trong lịch mục 5 = 1 Issue, đánh mã 1.1, 1.2...) |
| G03 | Một Issue chi tiết (nhãn, người phụ trách, liên kết commit/PR) |
| G04 | Một Pull Request có comment code review |
| G05 | Lịch sử commit / sơ đồ branch |
| G06 | Release/Tag (ví dụ v0.1-auth, v0.2-admin, v1.0-demo) |

**Thói quen Git để có minh chứng (chỉ tốn vài phút/việc):** tạo repo GitHub ngay ở bước 1.2; mỗi việc là 1 Issue; làm trên branch riêng (`feature/1.3-login`); gộp vào `main` qua Pull Request (nếu có thành viên khác thì để họ review); commit theo dạng `feat: ...`, `fix: ...`; gắn tag cuối mỗi ngày.

### C. Quản lý dự án (Chương 2)
| Mã | Nội dung ảnh |
|---|---|
| P01 | Bảng công việc (GitHub Projects hoặc Trello) thể hiện kế hoạch 3 ngày |
| P02 | Bảng Estimate vs Actual (số liệu từ mục 📝 bên dưới) |

### D. Hạ tầng, bảo mật, triển khai (Chương 4.4, 8, 9)
| Mã | Nội dung ảnh | Ghi chú |
|---|---|---|
| S01 | Supabase **Database → Schema Visualizer** (ERD) | Dùng cho 4.4. Chụp được ngay sau khi chạy `schema.sql` |
| S02 | Supabase **Authentication → Policies** (danh sách RLS) | Dùng cho 9.3 Security. Chụp được ngay |
| S03 | Supabase Logs / Realtime Inspector | Tùy chọn, cho 9.4 Observability |
| S04 | GitHub Actions chạy build thành công | Chỉ nếu nhóm làm CI cho 8.2 (Claude có thể hỗ trợ, khoảng 30 phút) |

### E. Kiểm thử (Chương 7.3 – cần phân tích, không chèn nhiều ảnh)
| Mã | Nội dung ảnh |
|---|---|
| Q01 | Kết quả chạy test (unit test trong Android Studio hoặc log) |
| Q02 | 2–3 test case tiêu biểu chạy thật trên emulator (trạng thái trước/sau) |

### F. Hình tự vẽ 🖊️ (Claude sinh sau khi app xong, nhóm chỉ cần kiểm lại)
| Mã | Hình | Chương |
|---|---|---|
| D01 | Use Case Diagram (3 vai trò) | 3.5 |
| D02 | State/Activity Diagram vòng đời ticket 5 bước | 3.5 |
| D03 | Sequence Diagram: tạo phiếu → phân công → xử lý → đóng | 4.4 |
| D04 | Sơ đồ kiến trúc tổng thể (Android MVVM ↔ Supabase Auth/DB/Storage/Realtime) | 4.2 |
| D05 | Class/Component Diagram | 4.4 |
| D06 | Pipeline build/test/deploy | 8.2 |

### G. Cần ghi chép trong lúc làm 📝 (rất khó viết lại nếu để cuối)
- [ ] **Thời gian thực tế (Actual) của từng việc** trong lịch mục 5, để điền bảng Estimate vs Actual (Chương 2.3). Giờ trong lịch chính là Estimate.
- [ ] **Ít nhất 1 thay đổi yêu cầu quan trọng** và tác động lên thiết kế/code/dữ liệu/kiểm thử (Chương 3.6). Ghi ngay khi xảy ra, ví dụ thay đổi phạm vi từ "cắt giảm" về "đủ PRD" ngày 14/11.
- [ ] **Quyết định kiến trúc (ADR):** vì sao Supabase thay backend tự viết, vì sao Compose + MVVM, vì sao RLS, vì sao chỉ OTP email... (Chương 4.3)
- [ ] **Rủi ro đã gặp** và cách xử lý (Chương 2.4), ví dụ giới hạn email OTP, lỗi Gradle.
- [ ] **Technical debt đã biết** (Chương 6.3), ví dụ bucket ảnh public, bảng tin lộ cả mô tả phiếu, giới hạn chuyển trạng thái chỉ kiểm ở giao diện, xóa tài khoản kéo theo xóa phiếu.
- [ ] **Việc dùng AI (Chương 10):** dùng Claude cho việc gì (viết SQL, code, lịch...), nhóm kiểm chứng thế nào (chạy thử, đọc code, test). Nhóm chịu trách nhiệm cuối cùng.
- [ ] **Lỗi phát hiện trong lúc test** và cách sửa (Chương 7.3).

### H. Không cần ảnh
Chương 1, Chương 10 (chỉ cần bảng và mô tả), Kết luận, Tài liệu tham khảo, trang bìa. Bảng phân công cần minh chứng Issue/PR/Commit: lấy từ phần B.

### Điểm cần chú ý
- Template chấm cả **quy trình** (yêu cầu, thiết kế, quản lý cấu hình, ước lượng), không chỉ app chạy được. Các mục 📝 và phần B quan trọng ngang với code.
- Bảng "Phân công và mức độ đóng góp" cần minh chứng Issue/PR/Commit theo từng người, nên nếu nhóm nhiều người thì mỗi người nên commit bằng tài khoản GitHub của mình.

## 6. Nhật ký thay đổi

- 2026-11-14: Chốt phạm vi, tech stack, dữ liệu mẫu tạm.
- 2026-11-14: Lịch 3 ngày x 8 giờ, phạm vi đầy đủ theo PRD (không cắt giảm), thêm hàm `delete_my_account()`.
- 2026-11-14: Thay danh sách ảnh kiểm thử (template gửi nhầm) bằng danh sách minh chứng cho báo cáo Công nghệ phần mềm nâng cao (mục 5c).
- 2026-11-14 15:04: Xong mục 1.1 (Supabase tạo, `schema.sql` chạy không lỗi). Email thử gửi dạng link thay vì mã OTP, ghi nợ cần sửa template trước mục 1.4. Android Studio Otter 3 (2025.2.3), thêm mục 4b về phiên bản thư viện.
