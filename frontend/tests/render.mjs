import assert from 'node:assert/strict';
import { createServer } from 'vite';
import react from '@vitejs/plugin-react';
import React from 'react';
import { renderToStaticMarkup } from 'react-dom/server';

// This is a static render smoke check, not a substitute for browser interaction tests.
const server = await createServer({
  configFile: false,
  plugins: [react()],
  server: { middlewareMode: true, hmr: false, ws: false, watch: null },
  appType: 'custom',
});
try {
  const { default: App, adminReturnUrl } = await server.ssrLoadModule('/src/App.jsx');
  assert.equal(adminReturnUrl('/admin/login', new URLSearchParams('returnTo=h2-console')), 'http://localhost:8080/h2-console');
  for (const value of ['https://example.invalid', '//example.invalid', 'javascript:alert(1)', '/h2-console'])
    assert.equal(adminReturnUrl('/admin/login', new URLSearchParams({ returnTo: value })), null);
  assert.equal(adminReturnUrl('/admin', new URLSearchParams('returnTo=h2-console')), null);
  const routes = [
    ['/', 'Seamless journeys'],
    ['/flights?from=1&to=2&date=2027-05-15', 'Finding your flights'],
    ['/book/12', 'Getting your departure ready'],
    ['/bookings', 'Your journey, at a glance'],
    ['/admin', 'Checking admin session'],
    ['/admin/login', 'Checking admin session'],
    ['/missing', 'back on course'],
  ];
  for (const [route, expected] of routes) {
    globalThis.window = { location: { hash: `#${route}` } };
    const html = renderToStaticMarkup(React.createElement(App));
    assert.ok(html.includes(expected), `Render failed: ${route}`);
    assert.ok(html.includes('main-content'));
    assert.ok(html.includes('Main navigation'));
    console.log(`Static render passed: ${route}`);
  }
  const { default: AdminLogin } = await server.ssrLoadModule(
    '/src/pages/AdminLogin.jsx',
  );
  const { default: Header } = await server.ssrLoadModule(
    '/src/components/Header.jsx',
  );
  const login = renderToStaticMarkup(
    React.createElement(AdminLogin, { onLogin() {} }),
  );
  assert.ok(
    login.includes('type="email"') && login.includes('type="password"'),
  );
  assert.ok(login.includes('current-password') && login.includes('Sign in'));
  assert.ok(login.includes('admin-login-layout') && login.includes('admin-login-intro'));
  const { default: MyBookings } = await server.ssrLoadModule('/src/pages/MyBookings.jsx');
  const bookings = renderToStaticMarkup(React.createElement(MyBookings));
  assert.ok(bookings.includes('page-hero') && bookings.includes('lookup-form'));
  const { default: SeatSelector } = await server.ssrLoadModule('/src/components/SeatSelector.jsx');
  const seatMap = renderToStaticMarkup(React.createElement(SeatSelector, {
    seats: [{ id: 1, seatNumber: '1A' }, { id: 2, seatNumber: '1C' }, { id: 3, seatNumber: '2A' }],
    unavailable: [2], selected: { id: 1 }, onSelect() {},
  }));
  assert.equal((seatMap.match(/class="seat-row"/g) || []).length, 2);
  assert.equal((seatMap.match(/aria-label="Seat /g) || []).length, 3);
  assert.ok(seatMap.includes('Seat 1C, unavailable'));
  console.log('Static checks passed: shared heroes and fixed console return destination');
  const anonymous = renderToStaticMarkup(
    React.createElement(Header, { path: '/' }),
  );
  assert.ok(
    anonymous.includes('Admin login') && !anonymous.includes('Log out'),
  );
  const authenticated = renderToStaticMarkup(
    React.createElement(Header, {
      path: '/admin',
      admin: { email: 'test@example.test' },
    }),
  );
  assert.ok(
    authenticated.includes('Log out') && !authenticated.includes('Admin login'),
  );
  console.log(
    'Static render passed: login form and authenticated/anonymous navigation',
  );
} finally {
  delete globalThis.window;
  await server.close();
}
