import { useEffect, useState } from 'react';
import SearchForm from '../components/SearchForm';
import FlightCard from '../components/FlightCard';
import Feedback, { Loading, EmptyState } from '../components/Feedback';
import Icon from '../components/Icon';
import { loadResults } from '../services/api';
import { formatDate } from '../format';

export default function FlightResults({
  airports,
  airportsLoading,
  airportsError,
  search,
}) {
  const [flights, setFlights] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [retry, setRetry] = useState(0);
  const [sort, setSort] = useState('departure');
  const complete = Boolean(search.from && search.to && search.date);
  useEffect(() => {
    const controller = new AbortController();
    if (!complete) {
      setLoading(false);
      setFlights([]);
      return () => controller.abort();
    }
    setLoading(true);
    setError('');
    setFlights([]);
    loadResults(search, controller.signal)
      .then(setFlights)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [search.from, search.to, search.date, retry, complete]);
  const origin = airports.find((airport) => String(airport.id) === search.from);
  const destination = airports.find(
    (airport) => String(airport.id) === search.to,
  );
  const sorted = [...flights].sort((a, b) =>
    sort === 'seats'
      ? b.availableSeatCount - a.availableSeatCount
      : a.departureTime.localeCompare(b.departureTime),
  );
  return (
    <div className="container page results-page">
      <a href="#/" className="back-link">
        <Icon name="back" size={16} />
        Back to search
      </a>
      <div className="page-heading">
        <div>
          <span className="eyebrow">YOUR NEXT JOURNEY</span>
          <h1>
            {origin && destination ? (
              <>
                {origin.city}
                <span className="heading-arrow"> → </span>
                {destination.city}
              </>
            ) : (
              'Find your flight'
            )}
          </h1>
          <p>
            {complete
              ? `${formatDate(search.date)} · One way`
              : 'Choose your route and departure date to see scheduled flights.'}
          </p>
        </div>
        {complete && !loading && !error && (
          <label className="sort-control">
            Sort by
            <select
              value={sort}
              onChange={(event) => setSort(event.target.value)}
            >
              <option value="departure">Departure time</option>
              <option value="seats">Available seats</option>
            </select>
          </label>
        )}
      </div>
      <div className="results-layout">
        <aside className="results-filters" aria-label="Flight search filters">
          <div className="filter-heading"><Icon name="search" size={20} /><div><h2>Refine your trip</h2><p>Search by route and date</p></div></div>
          <SearchForm airports={airports} loading={airportsLoading} initial={search} compact onRepeatSearch={() => setRetry((value) => value + 1)} />
          <Feedback>{airportsError}</Feedback>
        </aside>
        <div className="results-main">
      {loading ? (
        <Loading>Finding your flights…</Loading>
      ) : error ? (
        <Feedback onRetry={() => setRetry(retry + 1)}>{error}</Feedback>
      ) : sorted.length ? (
        <>
          <div className="results-meta">
            <strong>
              {sorted.length} {sorted.length === 1 ? 'departure' : 'departures'}{' '}
              found
            </strong>
            <span>
              Availability can change until your booking is confirmed.
            </span>
          </div>
          <div className="flight-list">
            {sorted.map((flight, index) => (
              <FlightCard
                key={flight.instanceId || `${flight.flightNumber}-${index}`}
                flight={flight}
              />
            ))}
          </div>
        </>
      ) : (
        <EmptyState
          icon="plane"
          title={
            complete
              ? 'No flights for this journey yet'
              : 'Where would you like to go?'
          }
        >
          {complete
            ? 'Try another date or a different route. Only departures returned by SkyNode appear here.'
            : 'Use the search form above to explore available departures.'}
        </EmptyState>
      )}
        </div>
      </div>
    </div>
  );
}
