// Xuất CSV: hàm thuần (toCsv, escapeCsvCell) và một hàm tải về (downloadCsv).

export interface CsvColumn<T> {
  header: string
  value: (row: T) => string | number | null | undefined
}

// Ô bắt đầu bằng = + - @ (hoặc tab/xuống dòng) có thể bị Excel hiểu là công thức (CSV injection).
// Dữ liệu như tiêu đề phiếu do người dùng nhập nên phải thêm dấu ' phía trước.
const FORMULA_START = /^[=+\-@\t\r]/

export function escapeCsvCell(raw: string | number | null | undefined): string {
  if (raw === null || raw === undefined) return ''
  let text = String(raw)
  if (typeof raw === 'string' && FORMULA_START.test(text)) text = `'${text}`
  if (/[",\r\n]/.test(text)) return `"${text.replace(/"/g, '""')}"`
  return text
}

export function toCsv<T>(rows: readonly T[], columns: readonly CsvColumn<T>[]): string {
  const header = columns.map((c) => escapeCsvCell(c.header)).join(',')
  const lines = rows.map((row) => columns.map((c) => escapeCsvCell(c.value(row))).join(','))
  return [header, ...lines].join('\r\n')
}

/** Tên tệp theo giờ địa phương: lich-su-phieu-20261125-1056.csv */
export function csvFilename(prefix: string, date: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0')
  const stamp =
    `${date.getFullYear()}${pad(date.getMonth() + 1)}${pad(date.getDate())}` +
    `-${pad(date.getHours())}${pad(date.getMinutes())}`
  return `${prefix}-${stamp}.csv`
}

/** Tải CSV về máy. Thêm BOM UTF-8 để Excel hiển thị đúng tiếng Việt. */
export function downloadCsv(filename: string, content: string): void {
  const blob = new Blob(['﻿', content], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  link.remove()
  URL.revokeObjectURL(url)
}
