import { useEffect, useState } from 'react'

/** Thời điểm hiện tại (ms), cập nhật định kỳ để các nhãn "Quá hạn" tự đổi khi ETA trôi qua. */
export function useNow(intervalMs = 60_000): number {
  const [now, setNow] = useState(() => Date.now())

  useEffect(() => {
    const timer = window.setInterval(() => setNow(Date.now()), intervalMs)
    return () => window.clearInterval(timer)
  }, [intervalMs])

  return now
}
