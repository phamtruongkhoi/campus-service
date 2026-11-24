import { useState } from 'react'
import type { FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/useAuth'
import { Button } from '../components/Button'
import { Notice } from '../components/Notice'
import { TextField } from '../components/TextField'
import { translateLoginError, translateRecoveryError } from '../lib/authErrors'
import { supabase } from '../lib/supabase'
import {
  validateFullName,
  validatePassword,
  validatePasswordConfirm,
  validateStudentId,
} from '../lib/validators'
import { ROLE_LABELS } from '../types/database'
import type { Profile as ProfileRow } from '../types/database'

/**
 * Hồ sơ dùng chung 3 vai trò (HANDOFF §2):
 *  - Sinh viên: sửa họ tên + MSSV, đổi mật khẩu (xác minh mật khẩu cũ), xóa tài khoản.
 *  - Admin: sửa họ tên.
 *  - Kỹ thuật viên: chỉ xem.
 */
export function Profile() {
  const { profile, user, refreshProfile } = useAuth()
  if (!profile) return null

  return (
    <div className="page">
      <h1 className="page-title">Hồ sơ</h1>
      <InfoSection profile={profile} onSaved={refreshProfile} />
      {profile.role === 'student' ? (
        <>
          <PasswordSection email={user?.email ?? profile.email ?? ''} />
          <DeleteSection />
        </>
      ) : null}
    </div>
  )
}

function InfoSection({ profile, onSaved }: { profile: ProfileRow; onSaved: () => void }) {
  const isStudent = profile.role === 'student'
  const canEditName = profile.role !== 'technician'

  const [fullName, setFullName] = useState(profile.full_name ?? '')
  const [studentId, setStudentId] = useState(profile.student_id ?? '')
  const [nameError, setNameError] = useState<string | null>(null)
  const [idError, setIdError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSave(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSaved(false)
    const nErr = validateFullName(fullName)
    const iErr = isStudent ? validateStudentId(studentId) : null
    setNameError(nErr)
    setIdError(iErr)
    if (nErr || iErr) return

    // Chỉ gửi đúng các cột được phép (quyền UPDATE trên profiles chỉ cấp cho full_name, student_id).
    const changes = isStudent
      ? { full_name: fullName.trim(), student_id: studentId.trim() }
      : { full_name: fullName.trim() }

    setLoading(true)
    const { data, error } = await supabase
      .from('profiles')
      .update(changes)
      .eq('id', profile.id)
      .select('id')
    setLoading(false)

    if (error) {
      setFormError(`Không lưu được: ${error.message}`)
      return
    }
    // RLS chặn thì Supabase không báo lỗi mà trả về 0 dòng: phải kiểm tra riêng.
    if (!data || data.length === 0) {
      setFormError('Không lưu được: không có quyền cập nhật hồ sơ này.')
      return
    }
    setSaved(true)
    onSaved()
  }

  return (
    <section className="panel card">
      <h2>Thông tin cá nhân</h2>
      <form className="auth-form" onSubmit={(e) => void handleSave(e)} noValidate>
        <TextField label="Email" value={profile.email ?? ''} onChange={() => undefined} disabled readOnly />
        <TextField label="Vai trò" value={ROLE_LABELS[profile.role]} onChange={() => undefined} disabled readOnly />
        <TextField
          label="Họ và tên"
          value={fullName}
          onChange={setFullName}
          error={nameError}
          disabled={!canEditName}
          autoComplete="name"
        />
        {isStudent ? (
          <TextField label="MSSV" value={studentId} onChange={setStudentId} error={idError} autoComplete="off" />
        ) : null}
        {!canEditName ? <Notice kind="info">Kỹ thuật viên chỉ xem hồ sơ, không chỉnh sửa.</Notice> : null}
        {formError ? <Notice kind="error">{formError}</Notice> : null}
        {saved ? <Notice kind="success">Đã lưu thông tin.</Notice> : null}
        {canEditName ? (
          <div>
            <Button type="submit" loading={loading}>
              Lưu thay đổi
            </Button>
          </div>
        ) : null}
      </form>
    </section>
  )
}

function PasswordSection({ email }: { email: string }) {
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [currentError, setCurrentError] = useState<string | null>(null)
  const [nextError, setNextError] = useState<string | null>(null)
  const [confirmError, setConfirmError] = useState<string | null>(null)
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const [loading, setLoading] = useState(false)

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setFormError(null)
    setSuccess(false)
    const cErr = current === '' ? 'Vui lòng nhập mật khẩu hiện tại.' : null
    const nErr = validatePassword(next)
    const fErr = validatePasswordConfirm(next, confirm)
    setCurrentError(cErr)
    setNextError(nErr)
    setConfirmError(fErr)
    if (cErr || nErr || fErr) return

    setLoading(true)
    // Xác minh mật khẩu cũ bằng cách đăng nhập lại (giống app Android).
    const verify = await supabase.auth.signInWithPassword({ email, password: current })
    if (verify.error) {
      setLoading(false)
      setFormError(
        verify.error.code === 'invalid_credentials'
          ? 'Mật khẩu hiện tại không đúng.'
          : translateLoginError(verify.error),
      )
      return
    }
    const { error } = await supabase.auth.updateUser({ password: next })
    setLoading(false)

    if (error) {
      setFormError(translateRecoveryError(error))
      return
    }
    setCurrent('')
    setNext('')
    setConfirm('')
    setSuccess(true)
  }

  return (
    <section className="panel card">
      <h2>Đổi mật khẩu</h2>
      <form className="auth-form" onSubmit={(e) => void handleSubmit(e)} noValidate>
        <TextField
          label="Mật khẩu hiện tại"
          type="password"
          autoComplete="current-password"
          value={current}
          onChange={setCurrent}
          error={currentError}
        />
        <TextField
          label="Mật khẩu mới"
          type="password"
          autoComplete="new-password"
          value={next}
          onChange={setNext}
          error={nextError}
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
        {success ? <Notice kind="success">Đã đổi mật khẩu.</Notice> : null}
        <div>
          <Button type="submit" loading={loading}>
            Đổi mật khẩu
          </Button>
        </div>
      </form>
    </section>
  )
}

function DeleteSection() {
  const navigate = useNavigate()
  const [confirming, setConfirming] = useState(false)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleDelete() {
    setError(null)
    setLoading(true)
    const { error: rpcError } = await supabase.rpc('delete_my_account')
    if (rpcError) {
      setLoading(false)
      setError(`Không xóa được tài khoản: ${rpcError.message}`)
      return
    }
    // Tài khoản đã mất nên chỉ xóa phiên cục bộ, không gọi máy chủ để thu hồi token.
    await supabase.auth.signOut({ scope: 'local' })
    setLoading(false)
    navigate('/login', { replace: true, state: { info: 'Tài khoản đã được xóa.' } })
  }

  return (
    <section className="panel card danger-zone">
      <h2>Xóa tài khoản</h2>
      <p>
        Xóa vĩnh viễn tài khoản cùng toàn bộ phiếu sự cố do bạn tạo. Không thể hoàn tác.
      </p>
      {error ? <Notice kind="error">{error}</Notice> : null}
      {confirming ? (
        <div className="fullpage-actions">
          <Button className="btn-danger" loading={loading} onClick={() => void handleDelete()}>
            Xác nhận xóa
          </Button>
          <Button variant="ghost" disabled={loading} onClick={() => setConfirming(false)}>
            Hủy
          </Button>
        </div>
      ) : (
        <div>
          <Button variant="ghost" className="btn-danger-outline" onClick={() => setConfirming(true)}>
            Xóa tài khoản
          </Button>
        </div>
      )}
    </section>
  )
}
