// Hàm thuần (không phụ thuộc React/Supabase) để dễ unit test ở mục 6.4.
// Mỗi hàm trả về thông báo lỗi tiếng Việt, hoặc null nếu hợp lệ.

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const STUDENT_ID_RE = /^[A-Za-z0-9]{4,20}$/
const OTP_RE = /^\d{6,10}$/

export const PASSWORD_MIN_LENGTH = 8

export function validateEmail(value: string): string | null {
  const v = value.trim()
  if (v === '') return 'Vui lòng nhập email.'
  if (!EMAIL_RE.test(v)) return 'Email không hợp lệ.'
  return null
}

export function validatePassword(value: string): string | null {
  if (value === '') return 'Vui lòng nhập mật khẩu.'
  if (value.length < PASSWORD_MIN_LENGTH) {
    return `Mật khẩu cần ít nhất ${PASSWORD_MIN_LENGTH} ký tự.`
  }
  return null
}

export function validatePasswordConfirm(password: string, confirm: string): string | null {
  if (confirm === '') return 'Vui lòng nhập lại mật khẩu.'
  if (password !== confirm) return 'Mật khẩu nhập lại không khớp.'
  return null
}

export function validateFullName(value: string): string | null {
  const v = value.trim()
  if (v === '') return 'Vui lòng nhập họ tên.'
  if (v.length < 2) return 'Họ tên quá ngắn.'
  return null
}

export function validateStudentId(value: string): string | null {
  const v = value.trim()
  if (v === '') return 'Vui lòng nhập MSSV.'
  if (!STUDENT_ID_RE.test(v)) return 'MSSV gồm 4–20 chữ cái hoặc chữ số, không có khoảng trắng.'
  return null
}

/** Supabase của dự án đang gửi mã 8 số; chấp nhận 6–10 số giống app Android. */
export function validateOtp(value: string): string | null {
  const v = value.trim()
  if (v === '') return 'Vui lòng nhập mã OTP.'
  if (!OTP_RE.test(v)) return 'Mã OTP chỉ gồm 6–10 chữ số.'
  return null
}
