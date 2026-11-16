import { useEffect, useState } from 'react';
import Field from './Field';
import Icon from './Icon';
import Feedback from './Feedback';
import { today } from '../format';

export default function SearchForm({
  airports,
  loading,
  initial = {},
  compact = false,
  onRepeatSearch,
}) {
  const [from, setFrom] = useState(initial.from || '');
  const [to, setTo] = useState(initial.to || '');
  const [date, setDate] = useState(initial.date || today());
  const [error, setError] = useState('');
  useEffect(() => {
    setFrom(initial.from || '');
    setTo(initial.to || '');
    setDate(initial.date || today());
  }, [initial.from, initial.to, initial.date]);
  const options = [
    { value: '', label: loading ? 'Loading airports…' : 'Choose an airport' },
    ...airports.map((airport) => ({
      value: String(airport.id),
      label: `${airport.city} · ${airport.name}`,
    })),
  ];
  function search(event) {
    event.preventDefault();
    if (from === to) {
      setError('Choose different departure and arrival airports.');
      return;
    }
    setError('');
    const next = `/flights?${new URLSearchParams({ from, to, date })}`;
    if (window.location.hash === `#${next}` && onRepeatSearch) onRepeatSearch();
    else window.location.hash = next;
  }
  return (
    <form
      className={`search-card ${compact ? 'compact' : ''}`}
      onSubmit={search}
    >
      {!compact && (
        <div className="search-type">
          <span>
            <Icon name="plane" size={17} />
            One way
          </span>
          <small>Your next journey starts here</small>
        </div>
      )}
      <div className="search-fields">
        <Field
          label="From"
          icon="pin"
          options={options}
          required
          value={from}
          onChange={(event) => setFrom(event.target.value)}
          disabled={loading || !airports.length}
        />
        <button
          className="swap-button"
          type="button"
          aria-label="Swap departure and arrival"
          onClick={() => {
            setFrom(to);
            setTo(from);
          }}
          disabled={loading}
        >
          <Icon name="swap" size={18} />
        </button>
        <Field
          label="To"
          icon="pin"
          options={options}
          required
          value={to}
          onChange={(event) => setTo(event.target.value)}
          disabled={loading || !airports.length}
        />
        <Field
          label="Departure date"
          icon="calendar"
          type="date"
          required
          value={date}
          onChange={(event) => setDate(event.target.value)}
        />
        <button
          className="button button-primary search-submit"
          type="submit"
          disabled={loading || !airports.length}
        >
          <Icon name="search" size={18} />
          Search flights
        </button>
      </div>
      <Feedback>{error}</Feedback>
      {!loading && airports.length === 0 && (
        <p className="form-note">
          No airports are available yet. Add airports in Admin to start
          searching.
        </p>
      )}
    </form>
  );
}
