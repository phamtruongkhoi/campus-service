import { useState } from 'react'
import type { FormEvent } from 'react'
import { Button } from '../Button'
import { Notice } from '../Notice'
import { SelectField } from '../SelectField'
import { technicianName } from '../../data/useTechnicians'
import { updateDispatch } from '../../data/dispatch'
import { ETA_CHOICES, nextStatusOnAssign, resolveEta } from '../../lib/dispatch'
import type { EtaChoiceId } from '../../lib/dispatch'
import { PRIORITY_LABELS } from '../../types/database'
import type { Profile, Ticket, TicketPriority } from '../../types/database'

interface DispatchFormProps {
  ticket: Ticket
  technicians: readonly Profile[]
  /** Gọi sau khi lưu thành công để tải lại danh sách phiếu. */
  onSaved: () => Promise<void>
}

const PRIORITY_OPTIONS = (['low', 'medium', 'high'] as const).map((p) => ({
  value: p,
  label: PRIORITY_LABELS[p],
}))

const NO_TECHNICIAN = ''

/**
 * Form điều phối. Dùng `key={ticket.id}` ở nơi gọi để form khởi tạo lại khi chọn phiếu khác.
 * Phiếu đã đóng thì khóa: tránh đổi kỹ thuật viên của phiếu đã có đánh giá.
 */
export function DispatchForm({ ticket, technicians, onSaved }: DispatchFormProps) {
  const [priority, setPriority] = useState<TicketPriority>(ticket.priority)
  const [technicianId, setTechnicianId] = useState(ticket.assigned_to ?? NO_TECHNICIAN)
  const [etaChoice, setEtaChoice] = useState<EtaChoiceId>('keep')
  const [isPublic, setIsPublic] = useState(ticket.is_public)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [info, setInfo] = useState<string | null>(null)

  const locked = ticket.status === 'closed'

  const technicianOptions = [
    { value: NO_TECHNICIAN, label: 'Chưa phân công' },
    ...technicians.map((t) => ({ value: t.id, label: technicianName(t) })),
  ]

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    if (locked) return
    setError(null)
    setInfo(null)
    setSaving(true)

    const assignedTo = technicianId === NO_TECHNICIAN ? null : technicianId
    const failure = await updateDispatch(ticket.id, {
      priority,
      assigned_to: assignedTo,
      eta: resolveEta(etaChoice, ticket.eta, Date.now()),
      is_public: isPublic,
      status: nextStatusOnAssign(ticket.status, assignedTo),
    })
    setSaving(false)

    if (failure) {
      setError(failure)
      return
    }
    setEtaChoice('keep')
    setInfo('Đã lưu thay đổi.')
    await onSaved()
  }

  return (
    <section className="dispatch">
      <h3>Điều phối</h3>
      {locked ? <Notice kind="info">Phiếu đã đóng, không thể điều phối.</Notice> : null}
      <form className="auth-form" onSubmit={(e) => void handleSubmit(e)}>
        <SelectField
          label="Mức ưu tiên"
          value={priority}
          options={PRIORITY_OPTIONS}
          disabled={locked || saving}
          onChange={(v) => setPriority(v as TicketPriority)}
        />
        <SelectField
          label="Kỹ thuật viên"
          value={technicianId}
          options={technicianOptions}
          disabled={locked || saving}
          onChange={setTechnicianId}
        />
        <SelectField
          label="ETA (thời gian dự kiến hoàn thành)"
          value={etaChoice}
          options={ETA_CHOICES.map((c) => ({ value: c.id, label: c.label }))}
          disabled={locked || saving}
          onChange={(v) => setEtaChoice(v as EtaChoiceId)}
        />
        <label className="check">
          <input
            type="checkbox"
            checked={isPublic}
            disabled={locked || saving}
            onChange={(e) => setIsPublic(e.target.checked)}
          />
          <span>Hiển thị công khai trên bảng tin</span>
        </label>
        {error ? <Notice kind="error">{error}</Notice> : null}
        {info ? <Notice kind="success">{info}</Notice> : null}
        <div>
          <Button type="submit" loading={saving} disabled={locked}>
            Lưu thay đổi
          </Button>
        </div>
      </form>
    </section>
  )
}
