import { useEffect, useMemo, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import '../../styles/student.css'
import { useAuth } from '../../auth/useAuth'
import { Button } from '../../components/Button'
import { Notice } from '../../components/Notice'
import { SelectField } from '../../components/SelectField'
import { TextField } from '../../components/TextField'
import { createTicket, fetchOptions } from '../../data/student'
import { compressImage } from '../../lib/imageCompress'
import {
  DESCRIPTION_MAX,
  MAX_IMAGES,
  TITLE_MAX,
  validateDescription,
  validateImageFile,
  validateTitle,
} from '../../lib/studentRules'
import type { AreaOption, CategoryOption } from '../../types/student'

interface Picked {
  key: string
  blob: Blob
  url: string
}

function areaText(a: AreaOption): string {
  return a.building && a.building.trim() ? `${a.building} - ${a.name}` : a.name
}

/** Sinh viên - Tạo phiếu: khu vực, loại lỗi, tiêu đề, mô tả, tối đa 3 ảnh (nén JPEG phía trình duyệt). */
export function StudentCreate() {
  const { user } = useAuth()
  const [areas, setAreas] = useState<AreaOption[]>([])
  const [categories, setCategories] = useState<CategoryOption[]>([])
  const [loadingOptions, setLoadingOptions] = useState(true)
  const [loadError, setLoadError] = useState<string | null>(null)

  const [areaId, setAreaId] = useState('')
  const [categoryId, setCategoryId] = useState('')
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [images, setImages] = useState<Picked[]>([])
  const [processing, setProcessing] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [errors, setErrors] = useState<Record<string, string | null>>({})
  const [formError, setFormError] = useState<string | null>(null)
  const [success, setSuccess] = useState(false)
  const imagesRef = useRef<Picked[]>([])

  useEffect(() => {
    imagesRef.current = images
  }, [images])

  // Giải phóng các URL xem trước khi rời trang.
  useEffect(
    () => () => {
      imagesRef.current.forEach((p) => URL.revokeObjectURL(p.url))
    },
    [],
  )

  useEffect(() => {
    let cancelled = false
    void fetchOptions().then((result) => {
      if (cancelled) return
      setAreas(result.areas)
      setCategories(result.categories)
      setLoadError(result.error)
      setLoadingOptions(false)
    })
    return () => {
      cancelled = true
    }
  }, [])

  const areaOptions = useMemo(
    () => [
      { value: '', label: 'Chọn khu vực' },
      ...areas.map((a) => ({ value: String(a.id), label: areaText(a) })),
    ],
    [areas],
  )
  const categoryOptions = useMemo(
    () => [{ value: '', label: 'Chọn loại lỗi' }, ...categories.map((c) => ({ value: String(c.id), label: c.name }))],
    [categories],
  )

  async function addFiles(fileList: FileList | null) {
    if (!fileList || fileList.length === 0) return
    const room = MAX_IMAGES - images.length
    if (room <= 0) return
    const files = Array.from(fileList).slice(0, room)
    setProcessing(true)
    setFormError(null)
    const added: Picked[] = []
    let skipped = 0
    for (const file of files) {
      if (validateImageFile(file)) {
        skipped++
        continue
      }
      try {
        const blob = await compressImage(file)
        added.push({ key: crypto.randomUUID(), blob, url: URL.createObjectURL(blob) })
      } catch {
        skipped++
      }
    }
    setImages((current) => [...current, ...added].slice(0, MAX_IMAGES))
    if (skipped > 0) setFormError('Có ảnh không đọc được hoặc không hợp lệ và đã bị bỏ qua.')
    setProcessing(false)
  }

  function removeImage(key: string) {
    setImages((current) => {
      const target = current.find((p) => p.key === key)
      if (target) URL.revokeObjectURL(target.url)
      return current.filter((p) => p.key !== key)
    })
  }

  function resetForm() {
    images.forEach((p) => URL.revokeObjectURL(p.url))
    setImages([])
    setAreaId('')
    setCategoryId('')
    setTitle('')
    setDescription('')
    setErrors({})
  }

  async function submit() {
    if (!user) return
    const next = {
      area: areaId ? null : 'Vui lòng chọn khu vực.',
      category: categoryId ? null : 'Vui lòng chọn loại lỗi.',
      title: validateTitle(title),
      description: validateDescription(description),
    }
    setErrors(next)
    if (Object.values(next).some(Boolean)) return

    setSubmitting(true)
    setFormError(null)
    setSuccess(false)
    const failure = await createTicket({
      userId: user.id,
      areaId: Number(areaId),
      categoryId: Number(categoryId),
      title,
      description,
      images: images.map((p) => p.blob),
    })
    setSubmitting(false)
    if (failure) {
      setFormError(`Không gửi được phiếu: ${failure}`)
      return
    }
    resetForm()
    setSuccess(true)
  }

  const busy = submitting || processing

  return (
    <div className="page">
      <div className="page-head">
        <h1 className="page-title">Tạo phiếu yêu cầu</h1>
      </div>

      {loadError ? <Notice kind="error">{loadError}</Notice> : null}
      {success ? (
        <Notice kind="success">
          Đã gửi phiếu yêu cầu thành công. <Link to="/student/tickets">Xem phiếu của tôi</Link>
        </Notice>
      ) : null}

      <form
        className="create-form card"
        noValidate
        onSubmit={(e) => {
          e.preventDefault()
          void submit()
        }}
      >
        <SelectField label="Khu vực" value={areaId} options={areaOptions} onChange={setAreaId} disabled={loadingOptions} />
        {errors.area ? <p className="field-error">{errors.area}</p> : null}
        <SelectField label="Loại lỗi" value={categoryId} options={categoryOptions} onChange={setCategoryId} disabled={loadingOptions} />
        {errors.category ? <p className="field-error">{errors.category}</p> : null}

        <TextField
          label="Tiêu đề"
          value={title}
          onChange={setTitle}
          maxLength={TITLE_MAX + 20}
          error={errors.title}
          hint={`${title.trim().length}/${TITLE_MAX}`}
        />

        <div className="field">
          <label htmlFor="create-desc">Mô tả (tùy chọn)</label>
          <div className="field-box">
            <textarea
              id="create-desc"
              rows={4}
              value={description}
              maxLength={DESCRIPTION_MAX + 100}
              onChange={(e) => setDescription(e.target.value)}
              aria-invalid={errors.description ? true : undefined}
            />
          </div>
          {errors.description ? (
            <p className="field-error">{errors.description}</p>
          ) : (
            <p className="field-hint">
              {description.trim().length}/{DESCRIPTION_MAX}
            </p>
          )}
        </div>

        <div className="field">
          <span className="field-label">
            Ảnh đính kèm ({images.length}/{MAX_IMAGES})
          </span>
          {images.length > 0 ? (
            <ul className="preview-grid">
              {images.map((p, index) => (
                <li key={p.key} className="preview">
                  <img src={p.url} alt={`Ảnh đính kèm ${index + 1}`} />
                  <button type="button" className="preview-remove" aria-label={`Bỏ ảnh ${index + 1}`} onClick={() => removeImage(p.key)} disabled={busy}>
                    ×
                  </button>
                </li>
              ))}
            </ul>
          ) : null}
          {images.length < MAX_IMAGES ? (
            <input
              type="file"
              accept="image/*"
              multiple
              disabled={busy}
              aria-label="Chọn ảnh đính kèm"
              onChange={(e) => {
                void addFiles(e.target.files)
                e.target.value = ''
              }}
            />
          ) : (
            <p className="field-hint">Đã đủ {MAX_IMAGES} ảnh.</p>
          )}
          {processing ? <p className="field-hint">Đang xử lý ảnh…</p> : null}
        </div>

        {formError ? <Notice kind="error">{formError}</Notice> : null}
        <Button type="submit" loading={submitting} disabled={processing || loadingOptions}>
          Gửi phiếu
        </Button>
      </form>
    </div>
  )
}
