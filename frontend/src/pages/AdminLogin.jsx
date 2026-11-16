import { useState } from 'react';
import { api } from '../services/api';
import Field from '../components/Field';
import Feedback from '../components/Feedback';
import Icon from '../components/Icon';

export default function AdminLogin({ onLogin, sessionMessage = '' }) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [busy, setBusy] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');

  async function login(event) {
    event.preventDefault();
    if (busy) return;
    setBusy(true);
    setError('');
    try {
      const session = await api.login(email.trim(), password);
      setPassword('');
      onLogin(session);
    } catch (failure) {
      setError(failure.message);
      setPassword('');
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="admin-login-layout">
      <section className="admin-login-intro">
        <div>
          <span className="feature-icon"><Icon name="globe" size={30} /></span>
          <span className="eyebrow">SKYNODE ADMIN</span>
          <h1>Welcome to your<br />Admin Portal.</h1>
          <p>Manage the routes, aircraft, and departures that keep every journey moving.</p>
        </div>
      </section>
      <div className="admin-login-page">
        <section className="panel admin-login-card">
          <a className="back-link" href="#/">
            <Icon name="back" size={16} />
            Back to flights
          </a>
          <span className="feature-icon">
            <Icon name="settings" size={25} />
          </span>
          <h2>Admin sign in</h2>
          <p className="muted">Use your administrator credentials to continue.</p>
          <Feedback kind="info">{!error && sessionMessage}</Feedback>
          <Feedback>{error}</Feedback>
          <form onSubmit={login}>
            <Field
              label="Email"
              name="email"
              type="email"
              autoComplete="username"
              required
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              disabled={busy}
            />
            <Field
              label="Password"
              name="password"
              type={showPassword ? 'text' : 'password'}
              autoComplete="current-password"
              endControl={
                <button
                  type="button"
                  className="password-toggle"
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  aria-pressed={showPassword}
                  disabled={busy}
                  onClick={() => setShowPassword(!showPassword)}
                >
                  {showPassword ? 'Hide' : 'Show'}
                </button>
              }
              required
              value={password}
              onChange={(event) => setPassword(event.target.value)}
              disabled={busy}
            />
            <button
              className="button button-primary"
              type="submit"
              disabled={busy}
            >
              {busy ? (
                <>
                  <span className="spinner" />
                  Signing in…
                </>
              ) : (
                <>
                  Sign in
                  <Icon name="arrow" size={17} />
                </>
              )}
            </button>
          </form>
          <p className="form-note">
            Looking for your trip?{' '}
            <a className="text-button" href="#/bookings">
              Find your booking
            </a>
          </p>
        </section>
      </div>
    </div>
  );
}
