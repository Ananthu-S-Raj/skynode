import Icon from './Icon';
import { formatDate, formatTime, statusClass, statusLabel } from '../format';

export default function FlightCard({ flight }) {
  const closed = ['CANCELLED', 'ARRIVED', 'DEPARTED'].includes(flight.status);
  const disabled =
    flight.availableSeatCount === 0 || closed || !flight.instanceId;
  return (
    <article className="flight-card">
      <div className="flight-airline">
        <span className="airline-mark">
          {flight.airlineName?.slice(0, 1) || <Icon name="plane" />}
        </span>
        <div>
          <h3>{flight.airlineName}</h3>
          <span className="small muted">
            {flight.flightNumber}
            {flight.aircraftModel ? ` · ${flight.aircraftModel}` : ''}
          </span>
        </div>
        <span className={`badge ${statusClass(flight.status)}`}>
          {statusLabel(flight.status)}
        </span>
      </div>
      <div className="flight-card-body">
        <div className="flight-route">
          <div className="route-point">
            <strong>{formatTime(flight.departureTime)}</strong>
            <span>{flight.originName}</span>
            <small>{formatDate(flight.departureTime, true)}</small>
          </div>
          <div className="route-connector">
            <span className="route-dot" />
            <span className="route-line" />
            <Icon name="plane" size={21} />
            <span className="route-line" />
            <span className="route-dot filled" />
          </div>
          <div className="route-point arrival">
            <strong>{formatTime(flight.arrivalTime)}</strong>
            <span>{flight.destinationName}</span>
            <small>{formatDate(flight.arrivalTime, true)}</small>
          </div>
        </div>
        <div className="flight-action">
          <span
            className={flight.availableSeatCount ? 'seat-count' : 'small muted'}
          >
            <Icon name="seat" size={16} />
            {flight.availableSeatCount}{' '}
            {flight.availableSeatCount === 1 ? 'seat' : 'seats'} remaining
          </span>
          {disabled ? (
            <button className="button button-secondary" disabled>
              {closed
                ? 'Not open for booking'
                : flight.availableSeatCount === 0
                  ? 'Fully booked'
                  : 'Schedule unavailable'}
            </button>
          ) : (
            <a
              className="button button-primary"
              href={`#/book/${flight.instanceId}`}
            >
              View seats
              <Icon name="arrow" size={17} />
            </a>
          )}
        </div>
      </div>
      {flight.resolutionError && (
        <p className="card-note">{flight.resolutionError}</p>
      )}
    </article>
  );
}
