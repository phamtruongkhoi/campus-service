import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../../components/AuthCard'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { TextField } from '../../components/TextField'
import { translateRecoveryError } from '../../lib/authErrors'
import { supabase } from '../../lib/supabase'
import { validateEmail } from '../../lib/validators'

export function ForgotPassword() {
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [emailError, setEmailError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const eErr = validateEmail(email)
    setEmailError(eErr)
    if (eErr) return

    setLoading(true)
    const cleanEmail = email.trim()
    const { error } = await supabase.auth.resetPasswordForEmail(cleanEmail)
    setLoading(false)

    if (error) {
      setFormError(translateRecoveryError(error))
      return
    }
    navigate('/verify', { state: { email: cleanEmail, mode: 'recovery' } })
  }

  return (
    <AuthCard title="Quên mật khẩu" subtitle="Nhập email để nhận mã OTP đặt lại mật khẩu">
      <form className="auth-form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={setEmail}
          error={emailError}
        />
        {formError ? <Notice kind="error">{formError}</Notice> : null}
        <Button type="submit" block loading={loading}>
          Gửi mã OTP
        </Button>
      </form>
      <div className="auth-links">
        <Link to="/login">Quay lại đăng nhập</Link>
      </div>
    </AuthCard>
  )
}
