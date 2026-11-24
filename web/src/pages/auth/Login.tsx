import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { AuthCard } from '../../components/AuthCard'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { TextField } from '../../components/TextField'
import { translateLoginError } from '../../lib/authErrors'
import { supabase } from '../../lib/supabase'
import { validateEmail } from '../../lib/validators'

interface LoginLocationState {
  info?: string
}

export function Login() {
  const location = useLocation()
  const info = (location.state as LoginLocationState | null)?.info

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [emailError, setEmailError] = useState<string | null>(null)
  const [passwordError, setPasswordError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const eErr = validateEmail(email)
    const pErr = password === '' ? 'Vui lòng nhập mật khẩu.' : null
    setEmailError(eErr)
    setPasswordError(pErr)
    if (eErr || pErr) return

    setLoading(true)
    const { error } = await supabase.auth.signInWithPassword({ email: email.trim(), password })
    setLoading(false)
    // Thành công: AuthProvider nhận phiên mới, GuestOnly tự chuyển về trang chủ.
    if (error) setFormError(translateLoginError(error))
  }

  return (
    <AuthCard title="Đăng nhập" subtitle="Hệ thống báo sự cố và dịch vụ trong trường">
      {info ? <Notice kind="success">{info}</Notice> : null}
      <form className="auth-form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={setEmail}
          error={emailError}
        />
        <TextField
          label="Mật khẩu"
          type="password"
          autoComplete="current-password"
          value={password}
          onChange={setPassword}
          error={passwordError}
        />
        {formError ? <Notice kind="error">{formError}</Notice> : null}
        <Button type="submit" block loading={loading}>
          Đăng nhập
        </Button>
      </form>
      <div className="auth-links">
        <Link to="/forgot">Quên mật khẩu?</Link>
        <Link to="/register">Đăng ký sinh viên</Link>
      </div>
    </AuthCard>
  )
}
