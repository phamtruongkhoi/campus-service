export type Theme = 'light' | 'dark'

const STORAGE_KEY = 'campus-theme'

export function getStoredTheme(): Theme | null {
  try {
    const value = localStorage.getItem(STORAGE_KEY)
    return value === 'light' || value === 'dark' ? value : null
  } catch {
    return null
  }
}

export function getSystemTheme(): Theme {
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

export function applyTheme(theme: Theme): void {
  document.documentElement.dataset.theme = theme
}

/** Gọi một lần trước khi render: ưu tiên lựa chọn đã lưu, nếu không thì theo hệ điều hành. */
export function initTheme(): void {
  applyTheme(getStoredTheme() ?? getSystemTheme())
}

export function setTheme(theme: Theme): void {
  try {
    localStorage.setItem(STORAGE_KEY, theme)
  } catch {
    // Bộ nhớ trình duyệt bị chặn: vẫn đổi giao diện cho phiên hiện tại.
  }
  applyTheme(theme)
}
