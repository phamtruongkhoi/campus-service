import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { AuthCard } from '../../components/AuthCard'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { TextField } from '../../components/TextField'
import {
  translateOtpError,
  translateRecoveryError,
  translateSignUpError,
} from '../../lib/authErrors'
import { supabase } from '../../lib/supabase'
import { validateOtp, validatePassword, validatePasswordConfirm } from '../../lib/validators'

type VerifyMode = 'signup' | 'recovery'

interface VerifyLocationState {
  email?: string
  mode?: VerifyMode
}

const RESEND_SECONDS = 60

/**
 * Dùng chung cho hai luồng:
 *  - signup: nhập OTP -> có phiên -> về trang chủ.
 *  - recovery: nhập OTP -> (có phiên tạm) -> đặt mật khẩu mới -> đăng xuất -> về trang đăng nhập.
 * Route này KHÔNG bọc GuestOnly vì ở luồng recovery người dùng đã có phiên tạm khi đang đặt mật khẩu.
 */
export function VerifyOtp() {
  const location = useLocation()
  const navigate = useNavigate()
  const state = location.state as VerifyLocationState | null
  const email = state?.email
  const mode = state?.mode

  const [step, setStep] = useState<'code' | 'password'>('code')
  const [otp, setOtp] = useState('')
  const [otpError, setOtpError] = useState<string | null>(null)
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [confirmError, setConfirmError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [infoMessage, setInfoMessage] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const [secondsLeft, setSecondsLeft] = useState(RESEND_SECONDS)

  useEffect(() => {
    if (secondsLeft <= 0) return
    const timer = window.setTimeout(() => setSecondsLeft((s) => s - 1), 1000)
    return () => window.clearTimeout(timer)
  }, [secondsLeft])

  if (!email || !mode) return <Navigate to="/login" replace />

  async function handleVerify(e: FormEvent) {
    e.preventDefault()
    if (!email || !mode) return
    setFormError(null)
    setInfoMessage(null)
    const err = validateOtp(otp)
    setOtpError(err)
    if (err) return

    setLoading(true)
    const { error } = await supabase.auth.verifyOtp({
      email,
      token: otp.trim(),
      type: mode === 'signup' ? 'signup' : 'recovery',
    })
    setLoading(false)

    if (error) {
      setFormError(translateOtpError(error))
      return
    }
    if (mode === 'signup') {
      navigate('/', { replace: true })
    } else {
      setStep('password')
    }
  }

  async function handleSetPassword(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const pErr = validatePassword(password)
    const cErr = validatePasswordConfirm(password, confirm)
    setPasswordError(pErr)
    setConfirmError(cErr)
    if (pErr || cErr) return

    setLoading(true)
    const { error } = await supabase.auth.updateUser({ password })
    if (error) {
      setLoading(false)
      setFormError(translateRecoveryError(error))
      return
    }
    // Bắt đăng nhập lại bằng mật khẩu mới (giống Android) thay vì giữ phiên của luồng khôi phục.
    await supabase.auth.signOut()
    setLoading(false)
    navigate('/login', {
      replace: true,
      state: { info: 'Đã đổi mật khẩu. Hãy đăng nhập lại bằng mật khẩu mới.' },
    })
  }

  async function handleResend() {
    if (!email || !mode || secondsLeft > 0) return
    setFormError(null)
    setInfoMessage(null)
    setLoading(true)
    const { error } =
      mode === 'signup'
        ? await supabase.auth.resend({ type: 'signup', email })
        : await supabase.auth.resetPasswordForEmail(email)
    setLoading(false)
    if (error) {
      setFormError(mode === 'signup' ? translateSignUpError(error) : translateRecoveryError(error))
      return
    }
    setSecondsLeft(RESEND_SECONDS)
    setInfoMessage('Đã gửi lại mã OTP. Hãy kiểm tra email.')
  }

  if (step === 'password') {
    return (
      <AuthCard title="Đặt mật khẩu mới" subtitle="Mã OTP đã được xác nhận">
        <form className="auth-form" onSubmit={(e) => void handleSetPassword(e)} noValidate>
          <TextField
            label="Mật khẩu mới"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={setPassword}
            error={passwordError}
            hint="Ít nhất 8 ký tự."
          />
          <TextField
            label="Nhập lại mật khẩu mới"
            type="password"
            autoComplete="new-password"
            value={confirm}
            onChange={setConfirm}
            error={confirmError}
          />
          {formError ? <Notice kind="error">{formError}</Notice> : null}
          <Button type="submit" block loading={loading}>
            Đổi mật khẩu
          </Button>
        </form>
      </AuthCard>
    )
  }

  return (
    <AuthCard
      title="Nhập mã OTP"
      subtitle={`Mã xác thực đã được gửi tới ${email}`}
    >
      <form className="auth-form" onSubmit={(e) => void handleVerify(e)} noValidate>
        <TextField
          label="Mã OTP"
          className="otp-input"
          inputMode="numeric"
          autoComplete="one-time-code"
          maxLength={10}
          value={otp}
          onChange={(v) => setOtp(v.replace(/\D/g, ''))}
          error={otpError}
        />
        {infoMessage ? <Notice kind="info">{infoMessage}</Notice> : null}
        {formError ? <Notice kind="error">{formError}</Notice> : null}
        <Button type="submit" block loading={loading}>
          Xác nhận
        </Button>
      </form>
      <div className="auth-links">
        <Button variant="ghost" disabled={secondsLeft > 0 || loading} onClick={() => void handleResend()}>
          {secondsLeft > 0 ? `Gửi lại mã sau ${secondsLeft}s` : 'Gửi lại mã'}
        </Button>
        <Link to="/login">Quay lại đăng nhập</Link>
      </div>
    </AuthCard>
  )
}
