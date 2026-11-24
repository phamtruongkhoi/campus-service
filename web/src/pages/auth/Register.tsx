import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { AuthCard } from '../../components/AuthCard'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { TextField } from '../../components/TextField'
import { translateSignUpError } from '../../lib/authErrors'
import { supabase } from '../../lib/supabase'
import {
  validateEmail,
  validateFullName,
  validatePassword,
  validatePasswordConfirm,
  validateStudentId,
} from '../../lib/validators'

interface FieldErrors {
  fullName: string | null
  studentId: string | null
  email: string | null
  password: string | null
  confirm: string | null
}

const NO_ERRORS: FieldErrors = {
  fullName: null,
  studentId: null,
  email: null,
  password: null,
  confirm: null,
}

export function Register() {
  const navigate = useNavigate()
  const [fullName, setFullName] = useState('')
  const [studentId, setStudentId] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [confirm, setConfirm] = useState('')
  const [errors, setErrors] = useState<FieldErrors>(NO_ERRORS)
  const [formError, setFormError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    const next: FieldErrors = {
      fullName: validateFullName(fullName),
      studentId: validateStudentId(studentId),
      email: validateEmail(email),
      password: validatePassword(password),
      confirm: validatePasswordConfirm(password, confirm),
    }
    setErrors(next)
    if (Object.values(next).some((v) => v !== null)) return

    setLoading(true)
    const cleanEmail = email.trim()
    // full_name và student_id nằm trong metadata: trigger handle_new_user đọc đúng hai khóa này.
    const { data, error } = await supabase.auth.signUp({
      email: cleanEmail,
      password,
      options: { data: { full_name: fullName.trim(), student_id: studentId.trim() } },
    })
    setLoading(false)

    if (error) {
      setFormError(translateSignUpError(error))
      return
    }
    // Khi bật xác thực email, Supabase không báo lỗi với email đã đăng ký mà trả về user không có identities.
    if (data.user && data.user.identities?.length === 0) {
      setFormError('Email này đã được đăng ký.')
      return
    }
    // Nếu dự án tắt xác thực email, đã có phiên ngay: GuestOnly sẽ tự chuyển trang.
    if (data.session) return

    navigate('/verify', { state: { email: cleanEmail, mode: 'signup' } })
  }

  return (
    <AuthCard title="Đăng ký" subtitle="Tài khoản dành cho sinh viên">
      <form className="auth-form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <TextField
          label="Họ và tên"
          autoComplete="name"
          value={fullName}
          onChange={setFullName}
          error={errors.fullName}
        />
        <TextField
          label="MSSV"
          autoComplete="off"
          value={studentId}
          onChange={setStudentId}
          error={errors.studentId}
        />
        <TextField
          label="Email"
          type="email"
          autoComplete="email"
          value={email}
          onChange={setEmail}
          error={errors.email}
        />
        <TextField
          label="Mật khẩu"
          type="password"
          autoComplete="new-password"
          value={password}
          onChange={setPassword}
          error={errors.password}
          hint="Ít nhất 8 ký tự."
        />
        <TextField
          label="Nhập lại mật khẩu"
          type="password"
          autoComplete="new-password"
          value={confirm}
          onChange={setConfirm}
          error={errors.confirm}
        />
        {formError ? <Notice kind="error">{formError}</Notice> : null}
        <Button type="submit" block loading={loading}>
          Đăng ký và nhận mã OTP
        </Button>
      </form>
      <div className="auth-links">
        <span>
          Đã có tài khoản? <Link to="/login">Đăng nhập</Link>
        </span>
      </div>
    </AuthCard>
  )
}
