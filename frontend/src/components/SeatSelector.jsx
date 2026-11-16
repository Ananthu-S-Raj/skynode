import Icon from './Icon';
import { EmptyState } from './Feedback';

export default function SeatSelector({
  seats,
  selected,
  unavailable,
  disabled,
  onSelect,
}) {
  if (!seats.length)
    return (
      <EmptyState icon="seat" title="No seats configured">
        There are no seats configured for this aircraft. Please choose another
        flight.
      </EmptyState>
    );
  const sorted = [...seats].sort((a, b) =>
    a.seatNumber.localeCompare(b.seatNumber, undefined, { numeric: true }),
  );
  const numbered = sorted.every((seat) => /^\d+[A-Za-z]+$/.test(seat.seatNumber));
  const rows = numbered ? sorted.reduce((groups, seat) => {
    const number = seat.seatNumber.match(/^\d+/)[0];
    (groups[number] ||= []).push(seat);
    return groups;
  }, {}) : null;
  function seatButton(seat) {
    const blocked = unavailable.includes(seat.id);
    const active = selected?.id === seat.id;
    return (
      <button type="button" key={seat.id}
        className={`seat ${active ? 'selected' : ''} ${blocked ? 'unavailable' : ''}`}
        disabled={disabled || blocked} aria-pressed={active}
        aria-label={`Seat ${seat.seatNumber}${seat.seatClass ? `, ${seat.seatClass}` : ''}${blocked ? ', unavailable' : ', availability checked on confirmation'}`}
        onClick={() => onSelect(seat)}>
        <Icon name={blocked ? 'close' : 'seat'} size={18} /><span>{seat.seatNumber}</span>
      </button>
    );
  }
  return (
    <div className="seat-selector">
      <div className="seat-legend">
        <span>
          <i className="legend-seat" />
          Check at confirmation
        </span>
        <span>
          <i className="legend-seat chosen" />
          Selected
        </span>
        <span>
          <i className="legend-seat blocked" />
          Unavailable
        </span>
      </div>
      <div className="cabin">
        <div className="cabin-front">
          <Icon name="plane" size={24} />
          <span>Choose your preferred seat</span>
        </div>
        <div className={rows ? 'seat-rows' : 'seat-grid'} role="group" aria-label="Configured aircraft seats">
          {rows ? Object.entries(rows).map(([number, rowSeats]) => {
            const middle = Math.ceil(rowSeats.length / 2);
            return <div className="seat-row" key={number}>
              <span className="row-number">{number}</span>
              <div className="seat-row-side">{rowSeats.slice(0, middle).map(seatButton)}</div>
              <span className="seat-aisle" aria-hidden="true" />
              <div className="seat-row-side">{rowSeats.slice(middle).map(seatButton)}</div>
            </div>;
          }) : sorted.map(seatButton)}
        </div>
      </div>
      <p className="seat-map-note">
        Only configured seats are shown. The aisle is illustrative; availability is checked when booking.
      </p>
    </div>
  );
}
