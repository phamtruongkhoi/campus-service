import { useSyncExternalStore } from 'react'
import { setTheme } from '../theme'
import type { Theme } from '../theme'

// Đọc theme trực tiếp từ <html data-theme>, nên nhiều nút đặt ở nhiều chỗ luôn đồng bộ với nhau.
function subscribe(onChange: () => void): () => void {
  const observer = new MutationObserver(onChange)
  observer.observe(document.documentElement, { attributes: true, attributeFilter: ['data-theme'] })
  return () => observer.disconnect()
}

function getSnapshot(): Theme {
  return document.documentElement.dataset.theme === 'dark' ? 'dark' : 'light'
}

export function ThemeToggle() {
  const theme = useSyncExternalStore(subscribe, getSnapshot)
  const next: Theme = theme === 'dark' ? 'light' : 'dark'

  return (
    <button
      type="button"
      className="theme-toggle"
      onClick={() => setTheme(next)}
      aria-label={next === 'dark' ? 'Chuyển sang giao diện tối' : 'Chuyển sang giao diện sáng'}
    >
      <span aria-hidden="true">{theme === 'dark' ? '☀' : '☾'}</span>
      <span>{theme === 'dark' ? 'Giao diện sáng' : 'Giao diện tối'}</span>
    </button>
  )
}
