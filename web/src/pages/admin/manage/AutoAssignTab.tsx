import { useEffect, useMemo, useState } from 'react'
import '../../../styles/autoassign.css'
import { Button } from '../../../components/Button'
import { Notice } from '../../../components/Notice'
import { SelectField } from '../../../components/SelectField'
import {
  runAutoAssignNow,
  saveAutoAssign,
  setSpecialty,
  setTechnicianAvailability,
} from '../../../data/autoAssign'
import { useCatalog } from '../../../hooks/useCatalog'
import { useAutoAssign } from '../../../hooks/useAutoAssign'
import { technicianName, useTechnicians } from '../../../data/useTechnicians'
import {
  ETA_DAY_CHOICES,
  assignedMessage,
  availabilityMap,
  etaChoiceToDays,
  etaDaysToChoice,
  specialtyMap,
  summarize,
} from '../../../lib/autoAssignRules'

type Msg = { kind: 'error' | 'success'; text: string }

/** Tab Tự động: bật/tắt tự động phân công, ETA mặc định, trạng thái sẵn sàng và chuyên môn của kỹ thuật viên. */
export function AutoAssignTab() {
  const aa = useAutoAssign()
  const cat = useCatalog()
  const techs = useTechnicians()

  const [enabled, setEnabled] = useState(false)
  const [etaChoice, setEtaChoice] = useState('')
  const [fallback, setFallback] = useState(true)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<Msg | null>(null)

  // Đồng bộ form với dữ liệu tải về (lần đầu và sau mỗi lần tải lại).
  const settings = aa.data?.settings
  useEffect(() => {
    if (!settings) return
    setEnabled(settings.enabled)
    setEtaChoice(etaDaysToChoice(settings.eta_days))
    setFallback(settings.fallback_any)
  }, [settings])

  const available = useMemo(() => availabilityMap(aa.data?.availability ?? []), [aa.data])
  const specialties = useMemo(() => specialtyMap(aa.data?.specialties ?? []), [aa.data])
  const availableCount = techs.technicians.filter((t) => available.get(t.id)).length

  if (aa.loading || cat.loading || techs.loading) return <div className="skeleton skeleton-panel" aria-busy="true" />

  const loadError = aa.error ?? cat.error ?? techs.error
  const categories = cat.catalog?.categories ?? []

  async function run(action: () => Promise<{ error: string | null; assigned: number }>, okText?: string) {
    setBusy(true)
    setMessage(null)
    const result = await action()
    setBusy(false)
    if (result.error) {
      setMessage({ kind: 'error', text: result.error })
      return
    }
    setMessage({ kind: 'success', text: `${okText ? okText + ' ' : ''}${assignedMessage(result.assigned)}`.trim() })
    await aa.reload()
  }

  async function toggleSpecialty(techId: string, categoryId: number, on: boolean) {
    setBusy(true)
    setMessage(null)
    const failure = await setSpecialty(techId, categoryId, on)
    setBusy(false)
    if (failure) setMessage({ kind: 'error', text: failure })
    await aa.reload()
  }

  return (
    <>
      <section className="panel card" aria-label="Cài đặt tự động phân công">
        <h2>Tự động phân công</h2>
        <p className="chart-note">
          Hệ thống tự giao phiếu mới cho kỹ thuật viên đang sẵn sàng, kể cả khi không có Admin trực. Ưu tiên người đúng
          chuyên môn, rồi người đang có ít phiếu chưa xong nhất.
        </p>

        {loadError ? <Notice kind="error">{loadError}</Notice> : null}
        {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}

        <form
          className="aa-form"
          onSubmit={(e) => {
            e.preventDefault()
            void run(
              () => saveAutoAssign({ enabled, eta_days: etaChoiceToDays(etaChoice), fallback_any: fallback }),
              'Đã lưu cài đặt.',
            )
          }}
        >
          <label className="check-row">
            <input type="checkbox" checked={enabled} onChange={(e) => setEnabled(e.target.checked)} />
            <span>Bật tự động phân công</span>
          </label>
          <SelectField
            label="ETA mặc định khi tự giao"
            value={etaChoice}
            onChange={setEtaChoice}
            options={ETA_DAY_CHOICES}
          />
          <label className="check-row">
            <input type="checkbox" checked={fallback} onChange={(e) => setFallback(e.target.checked)} />
            <span>Không có ai đúng chuyên môn thì giao cho người sẵn sàng khác</span>
          </label>
          <div className="inline-actions">
            <Button type="submit" loading={busy}>
              Lưu cài đặt
            </Button>
            <Button
              variant="ghost"
              onClick={() => void run(runAutoAssignNow)}
              disabled={busy || !settings?.enabled}
              title={settings?.enabled ? undefined : 'Hãy bật và lưu cài đặt trước'}
            >
              Giao ngay các phiếu đang chờ
            </Button>
          </div>
        </form>

        <p className="aa-summary">{summarize(Boolean(settings?.enabled), availableCount, Boolean(settings?.fallback_any ?? true))}</p>
      </section>

      <section className="panel card" aria-label="Kỹ thuật viên trực">
        <h2>Kỹ thuật viên ({availableCount}/{techs.technicians.length} sẵn sàng)</h2>
        <p className="chart-note">
          Kỹ thuật viên tự bật Sẵn sàng trên trang của họ; Admin cũng có thể đổi thay họ. Chọn loại lỗi mà mỗi người xử lý
          để được ưu tiên giao.
        </p>
        {techs.technicians.length === 0 ? (
          <p className="chart-empty">Chưa có kỹ thuật viên nào.</p>
        ) : (
          <ul className="manage-list">
            {techs.technicians.map((t) => {
              const on = available.get(t.id) === true
              const mine = specialties.get(t.id) ?? new Set<number>()
              return (
                <li key={t.id} className="manage-row manage-row-wrap">
                  <div className="manage-main">
                    <span className="manage-name">{technicianName(t)}</span>
                    <span className="manage-sub">{t.email ?? '—'}</span>
                  </div>
                  <button
                    type="button"
                    className={on ? 'avail-btn avail-on' : 'avail-btn'}
                    role="switch"
                    aria-checked={on}
                    disabled={busy}
                    onClick={() => void run(() => setTechnicianAvailability(t.id, !on))}
                  >
                    <span className="avail-dot" aria-hidden="true" />
                    {on ? 'Sẵn sàng' : 'Nghỉ'}
                  </button>
                  <div className="manage-full chip-row" role="group" aria-label={`Chuyên môn của ${technicianName(t)}`}>
                    {categories.length === 0 ? <span className="muted">Chưa có loại lỗi.</span> : null}
                    {categories.map((c) => (
                      <button
                        key={c.id}
                        type="button"
                        className="chip-toggle"
                        aria-pressed={mine.has(c.id)}
                        disabled={busy}
                        onClick={() => void toggleSpecialty(t.id, c.id, !mine.has(c.id))}
                      >
                        {c.name}
                      </button>
                    ))}
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </section>
    </>
  )
}
