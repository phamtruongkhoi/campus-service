import { useId, useState } from 'react'
import type { InputHTMLAttributes } from 'react'

interface TextFieldProps
  extends Omit<InputHTMLAttributes<HTMLInputElement>, 'onChange' | 'value'> {
  label: string
  value: string
  onChange: (value: string) => void
  error?: string | null
  hint?: string
}

export function TextField({
  label,
  value,
  onChange,
  error,
  hint,
  type = 'text',
  className,
  ...rest
}: TextFieldProps) {
  const id = useId()
  const messageId = `${id}-msg`
  const [visible, setVisible] = useState(false)
  const isPassword = type === 'password'
  const inputClass = [error ? 'has-error' : '', className ?? ''].filter(Boolean).join(' ')

  return (
    <div className="field">
      <label htmlFor={id}>{label}</label>
      <div className="field-box">
        <input
          {...rest}
          id={id}
          type={isPassword && visible ? 'text' : type}
          value={value}
          className={inputClass || undefined}
          aria-invalid={error ? true : undefined}
          aria-describedby={error || hint ? messageId : undefined}
          onChange={(e) => onChange(e.target.value)}
        />
        {isPassword ? (
          <button
            type="button"
            className="field-toggle"
            onClick={() => setVisible((v) => !v)}
            aria-label={visible ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
          >
            {visible ? 'Ẩn' : 'Hiện'}
          </button>
        ) : null}
      </div>
      {error ? (
        <p className="field-error" id={messageId}>
          {error}
        </p>
      ) : hint ? (
        <p className="field-hint" id={messageId}>
          {hint}
        </p>
      ) : null}
    </div>
  )
}
