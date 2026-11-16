const paths = {
  plane: 'm3 11 18-8-8 18-2-8-8-2Zm8 2L21 3',
  arrow: 'M4 12h16m-6-6 6 6-6 6',
  back: 'M20 12H4m6-6-6 6 6 6',
  swap: 'M4 7h16m-4-4 4 4-4 4M20 17H4m4-4-4 4 4 4',
  pin: 'M20 10c0 6-8 11-8 11S4 16 4 10a8 8 0 1 1 16 0ZM15 10a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  calendar: 'M4 5h16v16H4zM7 3v4m10-4v4M4 10h16',
  search: 'M16 16l5 5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
  seat: 'M5 3h5v10h9v7H5V3Zm5 6h7v4M5 20v2m14-2v2',
  ticket: 'M3 6h18v5a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4V6Zm12 0v3m0 3v2m0 3v2',
  check: 'm5 12 4 4L19 6',
  close: 'm6 6 12 12M6 18 18 6',
  menu: 'M4 6h16M4 12h16M4 18h16',
  plus: 'M12 5v14M5 12h14',
  refresh:
    'M20 7V3m0 4h-4M4 17v4m0-4h4M5 7a8 8 0 0 1 13-2l2 2M4 17l2 2a8 8 0 0 0 13-2',
  info: 'M12 11v6m0-10h.01M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  clock: 'M12 6v6l4 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  globe:
    'M2 12h20M12 2a18 18 0 0 1 0 20 18 18 0 0 1 0-20ZM22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  settings: 'M4 7h16M4 17h16M8 4v6m8 4v6',
  copy: 'M8 8h13v13H8zM16 8V3H3v13h5',
};

export default function Icon({ name, size = 20, ...props }) {
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      {...props}
    >
      <path d={paths[name] || paths.plane} />
    </svg>
  );
}
