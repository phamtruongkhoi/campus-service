export interface Area {
  id: number
  name: string
  building: string | null
}

export interface Category {
  id: number
  name: string
}

export interface RoleChange {
  id: number
  target_id: string
  target_email: string | null
  target_name: string | null
  actor_id: string
  old_role: 'student' | 'admin' | 'technician'
  new_role: 'student' | 'admin' | 'technician'
  note: string
  created_at: string
}

/** Mỗi dòng là một phiếu, chỉ cần khóa ngoại để đếm số phiếu dùng khu vực / loại lỗi. */
export interface UsageRow {
  area_id: number | null
  category_id: number | null
}
