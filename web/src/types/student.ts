import type { TicketStatus } from './database'

/** Một dòng của bảng tin công khai (bảng public_incidents, đồng bộ từ tickets bằng trigger). */
export interface PublicIncident {
  ticket_id: string
  area_name: string | null
  building: string | null
  category_name: string | null
  status: TicketStatus
  eta: string | null
  updated_at: string | null
}

export interface AreaOption {
  id: number
  name: string
  building: string | null
}

export interface CategoryOption {
  id: number
  name: string
}
