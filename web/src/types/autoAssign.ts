/** Cài đặt tự động phân công (bảng auto_assign_settings, đúng một dòng). */
export interface AutoAssignSettings {
  enabled: boolean
  /** null = không đặt ETA khi tự giao. */
  eta_days: number | null
  /** Không có ai đúng chuyên môn thì giao cho người sẵn sàng khác. */
  fallback_any: boolean
}

export interface TechnicianSetting {
  technician_id: string
  is_available: boolean
}

export interface TechnicianCategory {
  technician_id: string
  category_id: number
}

export const DEFAULT_AUTO_ASSIGN: AutoAssignSettings = {
  enabled: false,
  eta_days: null,
  fallback_any: true,
}
