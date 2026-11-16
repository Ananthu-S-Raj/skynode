import { useEffect, useRef, useState } from 'react';
import Header from './components/Header';
import Icon from './components/Icon';
import Home from './pages/Home';
import FlightResults from './pages/FlightResults';
import Booking from './pages/Booking';
import MyBookings from './pages/MyBookings';
import Admin from './pages/Admin';
import AdminLogin from './pages/AdminLogin';
import { api } from './services/api';
import Feedback, { EmptyState, Loading } from './components/Feedback';

export function adminReturnUrl(path, params) {
  return path === '/admin/login' && params.get('returnTo') === 'h2-console'
    ? 'http://localhost:8080/h2-console'
    : null;
}

export default function App() {
  const [route, setRoute] = useState(window.location.hash.slice(1) || '/');
  const [airports, setAirports] = useState([]);
  const [airportsLoading, setAirportsLoading] = useState(true);
  const [airportsError, setAirportsError] = useState('');
  const [retry, setRetry] = useState(0);
  const [admin, setAdmin] = useState(null);
  const authChecked = useRef(false);
  const [sessionLoading, setSessionLoading] = useState(true);
  const [sessionMessage, setSessionMessage] = useState('');
  const [logoutBusy, setLogoutBusy] = useState(false);
  const [authError, setAuthError] = useState('');
  const url = new URL(
    route.startsWith('/') ? route : '/',
    'http://skynode.local',
  );
  const path = url.pathname;
  const consoleReturnUrl = adminReturnUrl(path, url.searchParams);
  useEffect(() => {
    if (consoleReturnUrl && admin && !sessionLoading)
      window.location.assign(consoleReturnUrl);
  }, [consoleReturnUrl, admin, sessionLoading]);
  useEffect(() => {
    const navigate = () => setRoute(window.location.hash.slice(1) || '/');
    window.addEventListener('hashchange', navigate);
    return () => window.removeEventListener('hashchange', navigate);
  }, []);
  useEffect(() => {
    window.scrollTo(0, 0);
    document.getElementById('main-content')?.focus({ preventScroll: true });
    document.title = `${path === '/admin' ? 'Workspace' : path === '/bookings' ? 'My Bookings' : path.startsWith('/book/') ? 'Book your flight' : path === '/flights' ? 'Find a flight' : 'A better way to get there'} · SkyNode`;
  }, [route, path]);
  useEffect(() => {
    if (path !== '/' && path !== '/flights') return;
    const controller = new AbortController();
    setAirportsLoading(true);
    setAirportsError('');
    api
      .list('airports', controller.signal)
      .then(setAirports)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setAirportsError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setAirportsLoading(false);
      });
    return () => controller.abort();
  }, [path, retry]);
  useEffect(() => {
    if (authChecked.current && !path.startsWith('/admin')) return;
    const controller = new AbortController();
    setSessionLoading(true);
    api
      .session(controller.signal)
      .then(setAdmin)
      .catch((failure) => {
        if (failure.name !== 'AbortError') {
          setAdmin(null);
          setSessionMessage(failure.message);
        }
      })
      .finally(() => {
        if (!controller.signal.aborted) {
          authChecked.current = true;
          setSessionLoading(false);
        }
      });
    return () => controller.abort();
  }, [path]);
  useEffect(() => {
    const expired = () => {
      setAdmin(null);
      setSessionMessage('Your admin session expired. Please sign in again.');
      if (window.location.hash.startsWith('#/admin')) window.location.hash = '/admin/login';
    };
    window.addEventListener('skynode:unauthorized', expired);
    return () => window.removeEventListener('skynode:unauthorized', expired);
  }, []);
  useEffect(() => {
    const checkSession = () => {
      if (document.visibilityState !== 'visible') return;
      api.session().then(setAdmin).catch(() => setAdmin(null));
    };
    document.addEventListener('visibilitychange', checkSession);
    return () => document.removeEventListener('visibilitychange', checkSession);
  }, []);

  async function logout() {
    setLogoutBusy(true);
    setAuthError('');
    try {
      await api.logout();
      setAdmin(null);
      setSessionMessage('You have been signed out.');
      window.location.hash = '/admin/login';
    } catch (failure) {
      setAuthError(failure.message);
    } finally {
      setLogoutBusy(false);
    }
  }

  const search = {
    from: url.searchParams.get('from') || '',
    to: url.searchParams.get('to') || '',
    date: url.searchParams.get('date') || '',
  };
  let page;
  if (path === '/')
    page = (
      <Home
        airports={airports}
        loading={airportsLoading}
        error={airportsError}
        onRetry={() => setRetry(retry + 1)}
      />
    );
  else if (path === '/flights')
    page = (
      <FlightResults
        airports={airports}
        airportsLoading={airportsLoading}
        airportsError={airportsError}
        search={search}
      />
    );
  else if (/^\/book\/\d+$/.test(path))
    page = <Booking key={path} instanceId={path.split('/').at(-1)} />;
  else if (path === '/bookings')
    page = <MyBookings initialPnr={url.searchParams.get('pnr') || ''} />;
  else if (path === '/admin' || path === '/admin/login') {
    page = sessionLoading ? (
      <div className="container page">
        <Loading>Checking admin session…</Loading>
      </div>
    ) : admin ? (
      consoleReturnUrl ? <Loading>Opening the local database console…</Loading> : <Admin />
    ) : (
      <AdminLogin
        sessionMessage={sessionMessage}
        onLogin={(session) => {
          setAdmin(session);
          setSessionMessage('');
          setAuthError('');
          if (!consoleReturnUrl) window.location.hash = '/admin';
        }}
      />
    );
  } else
    page = (
      <div className="container page">
        <EmptyState
          icon="plane"
          title="Let’s get you back on course"
          action={
            <a className="button button-primary" href="#/">
              Find a flight
            </a>
          }
        >
          This page isn’t part of your itinerary.
        </EmptyState>
      </div>
    );
  return (
    <>
      <Header
        path={path}
        admin={admin}
        onLogout={logout}
        logoutBusy={logoutBusy}
      />
      <main id="main-content" tabIndex={-1}>
        {authError && (
          <div className="container">
            <Feedback>{authError}</Feedback>
          </div>
        )}
        {page}
      </main>
      <footer className="site-footer">
        <div className="container footer-top">
          <a className="brand footer-brand" href="#/">
            <span className="brand-mark">
              <Icon name="plane" size={20} />
            </span>
            SkyNode<span className="brand-period">.</span>
          </a>
          <span>A better way to get there.</span>
          <nav aria-label="Footer navigation">
            <a href="#/">Find a flight</a>
            <a href="#/bookings">My bookings</a>
            {admin && <a href="#/admin">Workspace</a>}
          </nav>
        </div>
        <div className="container footer-bottom">
          <span>SkyNode Flight Booking System</span>
          <span>
            <Icon name="globe" size={14} />
            Made for the journey ahead.
          </span>
        </div>
      </footer>
    </>
  );
}
