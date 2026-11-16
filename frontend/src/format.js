export function formatTime(value) {
  const match = value?.match(/T(\d{2}:\d{2})/);
  return match &&
    Number(match[1].slice(0, 2)) < 24 &&
    Number(match[1].slice(3)) < 60
    ? match[1]
    : '—';
}

export function formatDate(value, short = false) {
  if (!value) return '—';
  const date = new Date(`${value.slice(0, 10)}T12:00:00`);
  if (
    Number.isNaN(date.getTime()) ||
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}` !==
      value.slice(0, 10)
  )
    return value;
  return new Intl.DateTimeFormat('en', {
    month: short ? 'short' : 'long',
    day: 'numeric',
    ...(short ? {} : { year: 'numeric' }),
  }).format(date);
}

export function today() {
  const date = new Date();
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}

export function statusClass(status) {
  if (status === 'CANCELLED') return 'cancelled';
  if (status === 'DELAYED') return 'delayed';
  return 'normal';
}

export function statusLabel(status) {
  return status ? status.charAt(0) + status.slice(1).toLowerCase() : 'Unknown';
}
