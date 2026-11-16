import { useState } from 'react';
import Icon from './Icon';

export default function Header({ path, admin, onLogout, logoutBusy }) {
  const [open, setOpen] = useState(false);
  const adminPath = admin ? '/admin' : '/admin/login';
  const links = [
    { path: '/', label: 'Flights', icon: 'plane' },
    { path: '/bookings', label: 'My Bookings', icon: 'ticket' },
    {
      path: adminPath,
      label: admin ? 'Admin' : 'Admin login',
      icon: 'settings',
    },
  ];
  const active = path.startsWith('/admin')
    ? adminPath
    : path.startsWith('/bookings')
      ? '/bookings'
      : '/';
  return (
    <>
      <a
        className="skip-link"
        href="#main-content"
        onClick={(event) => {
          event.preventDefault();
          document.getElementById('main-content')?.focus();
        }}
      >
        Skip to content
      </a>
      <header className="site-header">
        <div className="container header-inner">
          <a
            className="brand"
            href="#/"
            aria-label="SkyNode home"
            onClick={() => setOpen(false)}
          >
            <span className="brand-mark">
              <Icon name="plane" size={24} />
            </span>
            SkyNode<span className="brand-period">.</span>
          </a>
          <button
            type="button"
            className="icon-button mobile-toggle"
            aria-label={open ? 'Close navigation' : 'Open navigation'}
            aria-expanded={open}
            aria-controls="primary-nav"
            onClick={() => setOpen(!open)}
          >
            <Icon name={open ? 'close' : 'menu'} />
          </button>
          <nav
            id="primary-nav"
            className={open ? 'navigation is-open' : 'navigation'}
            aria-label="Main navigation"
          >
            {links.map((link) => (
              <a
                key={link.path}
                href={`#${link.path}`}
                className={
                  active === link.path ? 'nav-link active' : 'nav-link'
                }
                aria-current={active === link.path ? 'page' : undefined}
                onClick={() => setOpen(false)}
              >
                <Icon name={link.icon} size={17} />
                {link.label}
              </a>
            ))}
            {admin && (
              <button
                type="button"
                className="nav-link nav-logout"
                onClick={onLogout}
                disabled={logoutBusy}
              >
                <Icon name="back" size={17} />
                {logoutBusy ? 'Signing out…' : 'Log out'}
              </button>
            )}
          </nav>
          <span className="header-tagline">A better way to get there</span>
        </div>
      </header>
    </>
  );
}
