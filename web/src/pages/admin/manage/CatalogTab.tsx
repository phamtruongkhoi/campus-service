import { useMemo, useState } from 'react'
import { Button } from '../../../components/Button'
import { Notice } from '../../../components/Notice'
import { TextField } from '../../../components/TextField'
import {
  createArea,
  createCategory,
  deleteArea,
  deleteCategory,
  updateArea,
  updateCategory,
} from '../../../data/catalog'
import { useCatalog } from '../../../hooks/useCatalog'
import {
  BUILDING_MAX,
  NAME_MAX,
  PROTECTED_CATEGORY,
  distinctBuildings,
  findDuplicateArea,
  findDuplicateCategory,
  isProtectedCategory,
  normalizeName,
  usageCounts,
  validateBuilding,
  validateCatalogName,
} from '../../../lib/catalogRules'

export type CatalogKind = 'area' | 'category'

interface Row {
  id: number
  name: string
  building: string
  used: number
}

/** Tab Khu vực / Loại lỗi dùng chung một giao diện. */
export function CatalogTab({ kind }: { kind: CatalogKind }) {
  const { catalog, error, loading, reload } = useCatalog()
  const isArea = kind === 'area'
  const noun = isArea ? 'khu vực' : 'loại lỗi'

  const [editingId, setEditingId] = useState<number | 'new' | null>(null)
  const [name, setName] = useState('')
  const [building, setBuilding] = useState('')
  const [fieldError, setFieldError] = useState<string | null>(null)
  const [message, setMessage] = useState<{ kind: 'error' | 'success'; text: string } | null>(null)
  const [busy, setBusy] = useState(false)
  const [confirmId, setConfirmId] = useState<number | null>(null)

  const rows: Row[] = useMemo(() => {
    if (!catalog) return []
    const counts = usageCounts(catalog.usage)
    return isArea
      ? catalog.areas.map((a) => ({
          id: a.id,
          name: a.name,
          building: a.building ?? '',
          used: counts.byArea.get(a.id) ?? 0,
        }))
      : catalog.categories.map((c) => ({
          id: c.id,
          name: c.name,
          building: '',
          used: counts.byCategory.get(c.id) ?? 0,
        }))
  }, [catalog, isArea])

  const buildings = useMemo(() => (catalog ? distinctBuildings(catalog.areas) : []), [catalog])
  const listId = `buildings-${kind}`

  function startNew() {
    setEditingId('new')
    setName('')
    setBuilding('')
    setFieldError(null)
    setMessage(null)
    setConfirmId(null)
  }

  function startEdit(row: Row) {
    setEditingId(row.id)
    setName(row.name)
    setBuilding(row.building)
    setFieldError(null)
    setMessage(null)
    setConfirmId(null)
  }

  function cancel() {
    setEditingId(null)
    setFieldError(null)
  }

  async function save() {
    if (!catalog || editingId === null) return
    const nameError = validateCatalogName(name) ?? (isArea ? validateBuilding(building) : null)
    if (nameError) {
      setFieldError(nameError)
      return
    }
    const excludeId = editingId === 'new' ? undefined : editingId
    const duplicate = isArea
      ? findDuplicateArea(catalog.areas, name, building, excludeId)
      : findDuplicateCategory(catalog.categories, name, excludeId)
    if (duplicate) {
      setFieldError(isArea ? 'Khu vực này (cùng tòa nhà) đã tồn tại.' : 'Loại lỗi này đã tồn tại.')
      return
    }
    const cleanName = normalizeName(name)
    const cleanBuilding = normalizeName(building)
    setBusy(true)
    setFieldError(null)
    const failure = isArea
      ? editingId === 'new'
        ? await createArea(cleanName, cleanBuilding)
        : await updateArea(editingId, cleanName, cleanBuilding)
      : editingId === 'new'
        ? await createCategory(cleanName)
        : await updateCategory(editingId, cleanName)
    setBusy(false)
    if (failure) {
      setFieldError(failure)
      return
    }
    setMessage({ kind: 'success', text: editingId === 'new' ? `Đã thêm ${noun}.` : `Đã lưu ${noun}.` })
    setEditingId(null)
    await reload()
  }

  async function remove(row: Row) {
    setBusy(true)
    setMessage(null)
    const failure = isArea ? await deleteArea(row.id) : await deleteCategory(row.id)
    setBusy(false)
    setConfirmId(null)
    if (failure) {
      setMessage({ kind: 'error', text: failure })
    } else {
      setMessage({ kind: 'success', text: `Đã xóa ${noun} "${row.name}".` })
    }
    await reload()
  }

  if (loading) return <div className="skeleton skeleton-panel" aria-busy="true" />

  return (
    <section className="panel card" aria-label={`Quản lý ${noun}`}>
      <div className="panel-head">
        <h2>
          {isArea ? 'Khu vực' : 'Loại lỗi'} ({rows.length})
        </h2>
        <Button onClick={startNew} disabled={editingId !== null || busy}>
          Thêm {noun}
        </Button>
      </div>

      {error ? <Notice kind="error">{error}</Notice> : null}
      {message ? <Notice kind={message.kind}>{message.text}</Notice> : null}

      {editingId !== null ? (
        <form
          className="inline-form"
          onSubmit={(e) => {
            e.preventDefault()
            void save()
          }}
        >
          <TextField
            label={isArea ? 'Tên khu vực' : 'Tên loại lỗi'}
            value={name}
            onChange={setName}
            maxLength={NAME_MAX + 20}
            autoFocus
            error={fieldError}
          />
          {isArea ? (
            <div>
              <TextField
                label="Tòa nhà (tùy chọn)"
                value={building}
                onChange={setBuilding}
                maxLength={BUILDING_MAX + 20}
                list={listId}
              />
              <datalist id={listId}>
                {buildings.map((b) => (
                  <option key={b} value={b} />
                ))}
              </datalist>
            </div>
          ) : null}
          <div className="inline-actions">
            <Button type="submit" loading={busy}>
              Lưu
            </Button>
            <Button variant="ghost" onClick={cancel} disabled={busy}>
              Hủy
            </Button>
          </div>
        </form>
      ) : null}

      {rows.length === 0 ? (
        <p className="chart-empty">Chưa có {noun} nào.</p>
      ) : (
        <ul className="manage-list">
          {rows.map((row) => {
            const protectedRow = !isArea && isProtectedCategory(row.name)
            const canDelete = row.used === 0 && !protectedRow
            return (
              <li key={row.id} className="manage-row">
                <div className="manage-main">
                  <span className="manage-name">{row.name}</span>
                  {isArea && row.building ? <span className="manage-sub">{row.building}</span> : null}
                </div>
                <span className="manage-count">{row.used} phiếu</span>
                <div className="manage-actions">
                  {protectedRow ? (
                    <span className="muted">Mục mặc định "{PROTECTED_CATEGORY}"</span>
                  ) : confirmId === row.id ? (
                    <>
                      <span className="muted">Xóa "{row.name}"?</span>
                      <Button variant="ghost" className="btn-danger-outline" onClick={() => void remove(row)} loading={busy}>
                        Xác nhận xóa
                      </Button>
                      <Button variant="ghost" onClick={() => setConfirmId(null)} disabled={busy}>
                        Không
                      </Button>
                    </>
                  ) : (
                    <>
                      <Button variant="ghost" onClick={() => startEdit(row)} disabled={busy || editingId !== null}>
                        Sửa
                      </Button>
                      <Button
                        variant="ghost"
                        className="btn-danger-outline"
                        onClick={() => setConfirmId(row.id)}
                        disabled={!canDelete || busy || editingId !== null}
                        title={canDelete ? undefined : `Đang có ${row.used} phiếu sử dụng`}
                      >
                        Xóa
                      </Button>
                    </>
                  )}
                </div>
              </li>
            )
          })}
        </ul>
      )}
      <p className="chart-note">Chỉ xóa được {noun} chưa có phiếu nào dùng. Đổi tên sẽ cập nhật cả các phiếu cũ.</p>
    </section>
  )
}
