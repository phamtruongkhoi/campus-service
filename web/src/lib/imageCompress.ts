import { fitWithin } from './studentRules'

const MAX_SIDE = 1600
const QUALITY = 0.8

/** Nén ảnh thành JPEG (cạnh dài tối đa 1600px, chất lượng 0.8), tôn trọng hướng chụp. Ném lỗi nếu không đọc được. */
export async function compressImage(file: File): Promise<Blob> {
  const bitmap = await createImageBitmap(file, { imageOrientation: 'from-image' })
  try {
    const { width, height } = fitWithin(bitmap.width, bitmap.height, MAX_SIDE)
    const canvas = document.createElement('canvas')
    canvas.width = width
    canvas.height = height
    const ctx = canvas.getContext('2d')
    if (!ctx) throw new Error('Trình duyệt không hỗ trợ xử lý ảnh.')
    ctx.fillStyle = '#ffffff' // nền trắng cho ảnh PNG trong suốt
    ctx.fillRect(0, 0, width, height)
    ctx.drawImage(bitmap, 0, 0, width, height)
    return await new Promise<Blob>((resolve, reject) => {
      canvas.toBlob((blob) => (blob ? resolve(blob) : reject(new Error('Không nén được ảnh.'))), 'image/jpeg', QUALITY)
    })
  } finally {
    bitmap.close()
  }
}
