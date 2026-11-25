import { useMemo, useState } from 'react'
import { Button } from '../../../components/Button'
import { Notice } from '../../../components/Notice'
import { SelectField } from '../../../components/SelectField'
import { TextField } from '../../../components/TextField'
import { useAuth } from '../../../auth/useAuth'
import { setUserRole } from '../../../data/users'
import { useUsers } from '../../../hooks/useUsers'
import { formatDateTime } from '../../../lib/ticketFormat'
import {
  NOTE_MAX,
  filterProfiles,
  roleActionFor,
  sortProfiles,
  validateRoleNote,
} from '../../../lib/userRules'
import type { RoleFilter } from '../../../lib/userRules'
import { ROLE_LABELS } from '../../../types/database'
import type { Profile } from '../../../types/database'

const FILTER_OPTIONS = [
  { value: 'all', label: 'Tất cả vai trò' },
  { value: 'student', label: ROLE_LABELS.student },
  { value: 'technician', label: ROLE_LABELS.technician },
  { value: 'admin', label: ROLE_LABELS.admin },
]

function who(p: { full_name: string | null; email: string | null }): string {
  return p.full_name?.trim() || p.email || '(không rõ)'
}

/** Tab Người dùng: cấp / thu hồi vai trò Kỹ thuật viên kèm căn cứ, có nhật ký. */
export function UsersTab() {
  const { user } = useAuth()
  const { profiles, changes, error, loading, reload } = useUsers()
  const [query, setQuery] = useState('')
  const [filter, setFilter] = useState<RoleFilter>('all')
  const [activeId, setActiveId] = useState<string | null>(null)
  const [note, setNote] = useState('')
  const [noteError, setNoteError] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)

  const visible = useMemo(() => sortProfiles(filterProfiles(profiles, query, filter)), [profiles, query, filter])

  function open(p: Profile) {
    setActiveId(p.id)
    setNote('')
    setNoteError(null)
    setMessage(null)
  }

  async function submit(p: Profile) {
    const action = roleActionFor(user?.id, p)
    if (!action.newRole) return
    const invalid = validateRoleNote(note)
    if (invalid) {
      setNoteError(invalid)
      return
    }
    setBusy(true)
    const failure = await setUserRole(p.id, action.newRole, note)
    setBusy(false)
    if (failure) {
      setNoteError(failure)
      return
    }
    setActiveId(null)
    setMessage({
      kind: 'success',
      text: `Đã đổi ${who(p)} thành ${ROLE_LABELS[action.newRole]}. Người này cần đăng nhập lại để áp dụng quyền mới.`,
    })
    await reload()
  }

  if (loading) return <div className="skeleton skeleton-panel" aria-busy="true" />

  return (
    <>
      <section className="panel card" aria-label="Người dùng">
        <h2>Người dùng ({visible.length}/{profiles.length})</h2>
        <p className="chart-note">
          Web chỉ cấp hoặc thu hồi vai trò Kỹ thuật viên. Tài khoản Quản trị viên do hệ thống cấp riêng.
        </p>

        {error ? <Notice kind="error">{error}</Notice> : null}
        {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}

        <div className="tech-toolbar">
          <TextField label="Tìm theo tên, email, MSSV" value={query} onChange={setQuery} />
          <SelectField
            label="Vai trò"
            value={filter}
            onChange={(v) => setFilter(v as RoleFilter)}
            options={FILTER_OPTIONS}
          />
        </div>

        {visible.length === 0 ? (
          <p className="chart-empty">Không có người dùng phù hợp.</p>
        ) : (
          <ul className="manage-list">
            {visible.map((p) => {
              const action = roleActionFor(user?.id, p)
              const isActive = activeId === p.id
              return (
                <li key={p.id} className="manage-row manage-row-wrap">
                  <div className="manage-main">
                    <span className="manage-name">{who(p)}</span>
                    <span className="manage-sub">
                      {p.email ?? '—'}
                      {p.student_id ? ` · MSSV ${p.student_id}` : ''}
                    </span>
                  </div>
                  <span className={`role-tag role-${p.role}`}>{ROLE_LABELS[p.role]}</span>
                  <div className="manage-actions">
                    {action.newRole ? (
                      <Button variant="ghost" onClick={() => open(p)} disabled={busy || isActive}>
                        {action.label}
                      </Button>
                    ) : (
                      <span className="muted">{action.reason}</span>
                    )}
                  </div>
                  {isActive && action.newRole ? (
                    <form
                      className="inline-form manage-full"
                      onSubmit={(e) => {
                        e.preventDefault()
                        void submit(p)
                      }}
                    >
                      <div className="field">
                        <label htmlFor={`note-${p.id}`}>Căn cứ (bắt buộc, ghi vào nhật ký)</label>
                        <div className="field-box">
                          <textarea
                            id={`note-${p.id}`}
                            rows={2}
                            maxLength={NOTE_MAX + 50}
                            value={note}
                            onChange={(e) => setNote(e.target.value)}
                            aria-invalid={noteError ? true : undefined}
                            autoFocus
                          />
                        </div>
                        {noteError ? <p className="field-error">{noteError}</p> : null}
                      </div>
                      <div className="inline-actions">
                        <Button type="submit" loading={busy}>
                          Xác nhận: {action.label}
                        </Button>
                        <Button variant="ghost" onClick={() => setActiveId(null)} disabled={busy}>
                          Hủy
                        </Button>
                      </div>
                    </form>
                  ) : null}
                </li>
              )
            })}
          </ul>
        )}
      </section>

      <section className="panel card" aria-label="Nhật ký đổi vai trò">
        <h2>Nhật ký đổi vai trò (20 gần nhất)</h2>
        {changes.length === 0 ? (
          <p className="chart-empty">Chưa có thay đổi nào.</p>
        ) : (
          <ul className="manage-list">
            {changes.map((c) => (
              <li key={c.id} className="manage-row manage-row-wrap">
                <div className="manage-main">
                  <span className="manage-name">
                    {who({ full_name: c.target_name, email: c.target_email })}: {ROLE_LABELS[c.old_role]} →{' '}
                    {ROLE_LABELS[c.new_role]}
                  </span>
                  <span className="manage-sub">Căn cứ: {c.note}</span>
                </div>
                <span className="muted">{formatDateTime(c.created_at)}</span>
              </li>
            ))}
          </ul>
        )}
      </section>
    </>
  )
}
