// Logic thuần cho Radar của kỹ thuật viên. Quy tắc khớp RadarViewModel.kt phía Android.

import type { Ticket, TicketPriority } from '../types/database'
import { isUnfinished, parseTime } from './ticketLogic'
import type { CountItem } from './ticketLogic'
import { assigneeLabel } from './ticketLogic'

export const NO_BUILDING_LABEL = 'Khu khác'
export const UNASSIGNED_LABEL = 'Chưa phân công'

const PRIORITY_RANK: Record<TicketPriority, number> = { low: 0, medium: 1, high: 2 }

/** Sự cố chưa xong: chờ tiếp nhận, đã phân công, đang xử lý. */
export function activeTickets(tickets: readonly Ticket[]): Ticket[] {
  return tickets.filter((t) => isUnfinished(t.status))
}

export interface RadarCounts {
  active: number
  unassigned: number
  inProgress: number
}

export function radarCounts(tickets: readonly Ticket[]): RadarCounts {
  const active = activeTickets(tickets)
  return {
    active: active.length,
    unassigned: active.filter((t) => t.assigned_to === null).length,
    inProgress: active.filter((t) => t.status === 'in_progress').length,
  }
}

/** Số việc đang gánh của từng kỹ thuật viên, kèm cột "Chưa phân công"; nhiều việc nhất lên đầu. */
export function workload(tickets: readonly Ticket[], userId: string): CountItem[] {
  const perTech = new Map<string, { label: string; count: number }>()
  let unassigned = 0
  for (const t of activeTickets(tickets)) {
    if (t.assigned_to === null) {
      unassigned++
      continue
    }
    const entry = perTech.get(t.assigned_to)
    if (entry) {
      entry.count++
    } else {
      const name = assigneeLabel(t)
      perTech.set(t.assigned_to, { label: t.assigned_to === userId ? `${name} (Bạn)` : name, count: 1 })
    }
  }
  const items: CountItem[] = [...perTech.values()].map((v) => ({ label: v.label, count: v.count }))
  if (unassigned > 0) items.push({ label: UNASSIGNED_LABEL, count: unassigned })
  return items.sort((a, b) => b.count - a.count || a.label.localeCompare(b.label, 'vi'))
}

export interface BuildingGroup {
  building: string
  tickets: Ticket[]
  colleagues: string[]
  mine: boolean
}

function byPriorityThenOldest(a: Ticket, b: Ticket): number {
  return (
    PRIORITY_RANK[b.priority] - PRIORITY_RANK[a.priority] ||
    (parseTime(a.created_at) ?? 0) - (parseTime(b.created_at) ?? 0)
  )
}

/** Gom theo tòa nhà (khu vực lân cận = cùng tòa). Nhóm có việc của mình lên trước, rồi nhóm nhiều phiếu hơn. */
export function groupByBuilding(tickets: readonly Ticket[], userId: string): BuildingGroup[] {
  const groups = new Map<string, Ticket[]>()
  for (const t of activeTickets(tickets)) {
    const key = t.areas?.building?.trim() || NO_BUILDING_LABEL
    const list = groups.get(key)
    if (list) list.push(t)
    else groups.set(key, [t])
  }
  return [...groups.entries()]
    .map(([building, list]) => {
      const seen = new Set<string>()
      const colleagues: string[] = []
      for (const t of list) {
        if (t.assigned_to === null || seen.has(t.assigned_to)) continue
        seen.add(t.assigned_to)
        colleagues.push(t.assigned_to === userId ? 'Bạn' : assigneeLabel(t))
      }
      return {
        building,
        tickets: [...list].sort(byPriorityThenOldest),
        colleagues,
        mine: list.some((t) => t.assigned_to === userId),
      }
    })
    .sort(
      (a, b) =>
        Number(b.mine) - Number(a.mine) || b.tickets.length - a.tickets.length || a.building.localeCompare(b.building, 'vi'),
    )
}
