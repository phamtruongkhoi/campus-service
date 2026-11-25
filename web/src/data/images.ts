import { supabase } from '../lib/supabase'

const BUCKET = 'ticket-images'

/** Đường dẫn công khai của ảnh trong bucket ticket-images (policy đọc mở cho mọi người). */
export function ticketImageUrl(path: string): string {
  return supabase.storage.from(BUCKET).getPublicUrl(path).data.publicUrl
}
