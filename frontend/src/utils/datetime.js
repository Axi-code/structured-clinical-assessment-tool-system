/**
 * DateTime utilities
 * Standard output: YYYY-MM-DD HH:mm:ss
 */

function pad2(n) {
  return String(n).padStart(2, '0')
}

function toDate(input) {
  if (input === null || input === undefined || input === '') return null
  if (input instanceof Date) return isNaN(input.getTime()) ? null : input

  // number-like timestamp (seconds or ms)
  if (typeof input === 'number') {
    const ms = input < 1e12 ? input * 1000 : input
    const d = new Date(ms)
    return isNaN(d.getTime()) ? null : d
  }

  if (typeof input === 'string') {
    const s = input.trim()
    if (!s) return null

    // "YYYY-MM-DD HH:mm:ss" -> "YYYY-MM-DDTHH:mm:ss"
    const normalized = s.includes(' ') && !s.includes('T') ? s.replace(' ', 'T') : s
    const d = new Date(normalized)
    if (!isNaN(d.getTime())) return d

    // fallback: numeric string
    const num = Number(s)
    if (!Number.isNaN(num)) {
      const ms = num < 1e12 ? num * 1000 : num
      const dn = new Date(ms)
      return isNaN(dn.getTime()) ? null : dn
    }
  }

  return null
}

export function formatDateTime(input) {
  const d = toDate(input)
  if (!d) return ''
  const y = d.getFullYear()
  const m = pad2(d.getMonth() + 1)
  const day = pad2(d.getDate())
  const hh = pad2(d.getHours())
  const mm = pad2(d.getMinutes())
  const ss = pad2(d.getSeconds())
  return `${y}-${m}-${day} ${hh}:${mm}:${ss}`
}

