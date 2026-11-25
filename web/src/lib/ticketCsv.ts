import { PRIORITY_LABELS, STATUS_LABELS } from '../types/database'
import type { Ticket } from '../types/database'
import type { CsvColumn } from './csv'
import { categoryLabel, formatDateTime } from './ticketFormat'
import { areaLabel, assigneeLabel } from './ticketLogic'

/** Các cột của tệp CSV lịch sử phiếu. */
export const TICKET_CSV_COLUMNS: readonly CsvColumn<Ticket>[] = [
  { header: 'Mã phiếu', value: (t) => t.id },
  { header: 'Tiêu đề', value: (t) => t.title },
  { header: 'Mô tả', value: (t) => t.description },
  { header: 'Khu vực', value: (t) => areaLabel(t) },
  { header: 'Loại lỗi', value: (t) => categoryLabel(t) },
  { header: 'Trạng thái', value: (t) => STATUS_LABELS[t.status] },
  { header: 'Ưu tiên', value: (t) => PRIORITY_LABELS[t.priority] },
  { header: 'Người gửi', value: (t) => t.creator?.full_name },
  { header: 'MSSV', value: (t) => t.creator?.student_id },
  { header: 'Kỹ thuật viên', value: (t) => assigneeLabel(t) },
  { header: 'ETA', value: (t) => (t.eta ? formatDateTime(t.eta) : '') },
  { header: 'Ngày tạo', value: (t) => formatDateTime(t.created_at) },
  { header: 'Ngày khắc phục', value: (t) => (t.resolved_at ? formatDateTime(t.resolved_at) : '') },
  { header: 'Số sao', value: (t) => t.rating },
  { header: 'Nhận xét', value: (t) => t.feedback },
]
