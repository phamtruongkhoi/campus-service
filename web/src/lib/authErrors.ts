import type { AuthError } from '@supabase/supabase-js'

// Bài học từ Android (HANDOFF 16/11): KHÔNG dùng chung một hàm dịch lỗi cho nhiều luồng,
// và khi không nhận ra lỗi thì hiện lỗi thật thay vì đoán. Vì vậy mỗi luồng một hàm riêng.

function isNetworkError(error: AuthError): boolean {
  return (
    error.name === 'AuthRetryableFetchError' ||
    /failed to fetch|networkerror|network request failed/i.test(error.message)
  )
}

function isRateLimit(error: AuthError): boolean {
  return (
    error.code === 'over_email_send_rate_limit' ||
    error.code === 'over_request_rate_limit' ||
    error.status === 429
  )
}

const NETWORK_MESSAGE = 'Không kết nối được máy chủ. Hãy kiểm tra mạng rồi thử lại.'
const RATE_LIMIT_MESSAGE = 'Gửi quá nhiều yêu cầu. Hãy chờ vài phút rồi thử lại.'

function fallback(error: AuthError): string {
  return `Có lỗi xảy ra: ${error.message}`
}

export function translateLoginError(error: AuthError): string {
  if (isNetworkError(error)) return NETWORK_MESSAGE
  if (isRateLimit(error)) return RATE_LIMIT_MESSAGE
  if (error.code === 'invalid_credentials') return 'Email hoặc mật khẩu không đúng.'
  if (error.code === 'email_not_confirmed') {
    return 'Email chưa được xác thực. Hãy đăng ký lại để nhận mã OTP.'
  }
  return fallback(error)
}

export function translateSignUpError(error: AuthError): string {
  if (isNetworkError(error)) return NETWORK_MESSAGE
  if (isRateLimit(error)) return RATE_LIMIT_MESSAGE
  if (error.code === 'user_already_exists') return 'Email này đã được đăng ký.'
  if (error.code === 'weak_password') return 'Mật khẩu quá yếu, hãy chọn mật khẩu mạnh hơn.'
  if (error.code === 'email_address_invalid') return 'Email không hợp lệ hoặc không được chấp nhận.'
  return fallback(error)
}

export function translateOtpError(error: AuthError): string {
  if (isNetworkError(error)) return NETWORK_MESSAGE
  if (isRateLimit(error)) return RATE_LIMIT_MESSAGE
  if (error.code === 'otp_expired') return 'Mã OTP sai hoặc đã hết hạn.'
  return fallback(error)
}

export function translateRecoveryError(error: AuthError): string {
  if (isNetworkError(error)) return NETWORK_MESSAGE
  if (isRateLimit(error)) return RATE_LIMIT_MESSAGE
  if (error.code === 'same_password') return 'Mật khẩu mới phải khác mật khẩu cũ.'
  if (error.code === 'weak_password') return 'Mật khẩu quá yếu, hãy chọn mật khẩu mạnh hơn.'
  return fallback(error)
}
