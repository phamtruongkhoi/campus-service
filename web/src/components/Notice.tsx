import type { ReactNode } from 'react'

interface NoticeProps {
  kind?: 'error' | 'info' | 'success'
  children: ReactNode
}

export function Notice({ kind = 'info', children }: NoticeProps) {
  return (
    <div className={`notice notice-${kind}`} role={kind === 'error' ? 'alert' : 'status'}>
      {children}
    </div>
  )
}
