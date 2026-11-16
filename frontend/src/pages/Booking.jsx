import { useEffect, useState } from 'react';
import { api, loadBookingContext } from '../services/api';
import SeatSelector from '../components/SeatSelector';
import Field from '../components/Field';
import Icon from '../components/Icon';
import Feedback, { Loading } from '../components/Feedback';
import { formatDate, formatTime, statusLabel } from '../format';

export default function Booking({ instanceId }) {
  const [context, setContext] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selected, setSelected] = useState(null);
  const [unavailable, setUnavailable] = useState([]);
  const [details, setDetails] = useState({ fullName: '', email: '', phone: '', age: '', gender: '' });
  const [card, setCard] = useState({ cardholderName: '', cardNumber: '', expiry: '', cvv: '' });
  const [otp, setOtp] = useState('');
  const [paymentId, setPaymentId] = useState('');
  const [step, setStep] = useState(0);
  const [busy, setBusy] = useState(false);
  const [booking, setBooking] = useState(null);
  const [retry, setRetry] = useState(0);
  const [copied, setCopied] = useState(false);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError('');
    setSelected(null);
    setContext(null);
    setUnavailable([]);
    setStep(0);
    loadBookingContext(instanceId, controller.signal)
      .then(setContext)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [instanceId, retry]);

  async function startPayment(event) {
    event.preventDefault();
    setError('');
    setStep(2);
  }

  async function submitPayment(event) {
    event.preventDefault();
    if (busy) return;
    const [month, year] = card.expiry.split('/').map(Number);
    const now = new Date();
    if (2000 + year < now.getFullYear() ||
        (2000 + year === now.getFullYear() && month < now.getMonth() + 1)) {
      setError('Card expiry must be current or later.');
      return;
    }
    setBusy(true);
    setError('');
    try {
      const payment = await api.startMockPayment({
        ...details, age: Number(details.age),
        flightInstanceId: context.instance.id, seatId: selected.id,
      });
      setCard({ cardholderName: '', cardNumber: '', expiry: '', cvv: '' });
      setPaymentId(payment.paymentId);
      setStep(3);
    } catch (failure) {
      setError(failure.message);
    } finally {
      setBusy(false);
    }
  }

  async function verifyOtp(event) {
    event.preventDefault();
    if (busy || !/^[0-9]{4}$/.test(otp)) return;
    setBusy(true);
    setStep(4);
    setError('');
    try {
      const result = await api.verifyMockPayment(paymentId, otp);
      setBooking(result.booking);
      setOtp('');
      setTimeout(() => setStep(5), 700);
    } catch (failure) {
      setError(failure.status === 0
        ? `${failure.message} The result is unknown; check My Bookings before retrying.`
        : failure.message);
      if (failure.status === 409 && /not available/i.test(failure.message)) {
        setUnavailable((seats) => [...seats, selected.id]);
        setSelected(null);
        setStep(0);
      } else setStep(3);
    } finally {
      setBusy(false);
    }
  }

  async function copyPnr() {
    try {
      await navigator.clipboard.writeText(booking.pnr);
      setCopied(true);
    } catch {
      setError(
        'Copy was unavailable. Select the PNR text to copy it manually.',
      );
    }
  }

  if (loading)
    return (
      <div className="container page">
        <Loading>Getting your departure ready…</Loading>
      </div>
    );
  if (!context)
    return (
      <div className="container page">
        <a className="back-link" href="#/">
          <Icon name="back" size={16} />
          Find another flight
        </a>
        <Feedback onRetry={() => setRetry(retry + 1)}>{error}</Feedback>
      </div>
    );
  const { flight, instance, plane, seats, availableSeatCount } = context;
  const closed =
    ['CANCELLED', 'DEPARTED', 'ARRIVED'].includes(instance.status) ||
    availableSeatCount === 0;
  return (
    <div className="container page booking-page">
      <a className="back-link" href="#/" aria-disabled={busy}>
        <Icon name="back" size={16} />
        Find another flight
      </a>
      <div className="page-heading">
        <div>
          <span className="eyebrow">MAKE IT YOUR JOURNEY</span>
          <h1>
            {step === 5 ? 'You’re going places.' : step === 4 ? 'Payment successful.' : step === 3 ? 'Verify your payment.' : step === 2 ? 'Demo card payment.' : step === 1 ? 'Your details.' : 'A seat for your next chapter.'}
          </h1>
          <p>
            {step === 5 ? 'Your booking is confirmed. Keep your PNR close.' : step === 4 ? 'Preparing your confirmation…' : step === 3 ? 'Enter any four-digit demo OTP.' : step === 2 ? 'No real charge will be made.' : step === 1 ? 'Enter the passenger information for your booking.' : 'Choose a preferred seat. Availability is confirmed when you book.'}
          </p>
        </div>
      </div>
      <ol className="booking-progress" aria-label="Booking progress">
        {['Choose a seat', 'Passenger details', 'Mock payment', 'OTP', 'Processing', 'Confirmation'].map(
          (label, index) => (
            <li
              key={label}
              className={index <= step ? 'current' : ''}
              aria-current={index === step ? 'step' : undefined}
            >
              <span>
                {index < step ? <Icon name="check" size={14} /> : index + 1}
              </span>
              {label}
            </li>
          ),
        )}
      </ol>
      <Feedback>{error}</Feedback>
      {booking && step === 4 ? (
        <div className="confirmation-card panel" role="status">
          <span className="spinner" /> Payment successful (demo). Preparing your booking…
        </div>
      ) : booking ? (
        <div className="confirmation-card panel">
          <span className="confirmation-check">
            <Icon name="check" size={34} />
          </span>
          <span className="eyebrow">BOOKING CONFIRMED</span>
          <h2>See you in the sky, {booking.passengerName}.</h2>
          <p>Payment status: Paid (Demo) · Total: ₹{booking.totalAmount}</p>
          <p>Your booking reference</p>
          <div className="pnr-display">
            <strong>{booking.pnr}</strong>
            <button
              className="icon-button"
              type="button"
              onClick={copyPnr}
              aria-label="Copy PNR"
            >
              <Icon name={copied ? 'check' : 'copy'} />
            </button>
          </div>
          {copied && (
            <p className="success-text" role="status">
              PNR copied
            </p>
          )}
          <p>{booking.passengerEmail} · {booking.passengerPhone} · Age {booking.passengerAge} · {booking.passengerGender}</p>
          <p>Departure: {formatDate(instance.departureDateTime)} at {formatTime(instance.departureDateTime)}</p>
          <p>Arrival: {formatDate(instance.arrivalDateTime)} at {formatTime(instance.arrivalDateTime)}</p>
          <div className="confirmation-details">
            <span>
              <small>Flight</small>
              <strong>{booking.flightNumber}</strong>
            </span>
            <span>
              <small>Seat</small>
              <strong>{booking.seatNumber}</strong>
            </span>
            <span>
              <small>Status</small>
              <strong>{statusLabel(booking.status)}</strong>
            </span>
          </div>
          <p className="confirmation-route">
            {booking.origin}
            <Icon name="arrow" size={17} />
            {booking.destination}
          </p>
          <a
            className="button button-primary"
            href={`#/bookings?pnr=${encodeURIComponent(booking.pnr)}`}
          >
            View my booking
            <Icon name="arrow" size={18} />
          </a>
        </div>
      ) : (
        <div className="booking-layout">
          <section className="panel seat-panel">
            <div className="panel-heading">
              <div>
                <h2>
                  {['Your preferred seat', 'Passenger details', 'Mock payment', 'OTP verification', 'Processing'][step]}
                </h2>
                <p className="muted small">
                  {step === 0 ? `${plane.model} · ${seats.length} configured seats` : step === 2 ? 'Demo charge: ₹4,999. Use any correctly formatted dummy card.' : 'Your seat is confirmed only after OTP verification and booking.'}
                </p>
              </div>
              {step === 0 && (
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Refresh seat choices"
                  onClick={() => setRetry(retry + 1)}
                >
                  <Icon name="refresh" size={18} />
                </button>
              )}
            </div>
            {step === 0 ? (
              <>
                {closed && (
                  <Feedback kind="info">
                    This departure is not open for booking. Please choose
                    another flight.
                  </Feedback>
                )}
                <SeatSelector
                  seats={seats}
                  selected={selected}
                  unavailable={unavailable}
                  disabled={closed}
                  onSelect={setSelected}
                />
                <div className="selection-footer">
                  <span>
                    {selected ? (
                      <>
                        <strong>Seat {selected.seatNumber}</strong>
                        <small>
                          {selected.seatClass || 'Class not specified'}
                        </small>
                      </>
                    ) : (
                      'Choose a seat to continue'
                    )}
                  </span>
                  <button
                    className="button button-primary"
                    type="button"
                    disabled={!selected || closed}
                    onClick={() => {
                      setStep(1);
                      setError('');
                    }}
                  >
                    Continue
                    <Icon name="arrow" size={17} />
                  </button>
                </div>
                <details className="availability-note">
                  <summary>About seat availability</summary>
                  <p>
                    The seat list contains aircraft configuration, not live
                    availability for each seat. Only the remaining-seat total is
                    exposed. Seats rejected by a booking attempt are disabled
                    until you refresh. SkyNode checks your choice before
                    confirming.
                  </p>
                </details>
              </>
            ) : (
              <form onSubmit={step === 1 ? startPayment : step === 2 ? submitPayment : verifyOtp} className="passenger-form">
                {step === 1 && <>
                  <Field label="Full name" required value={details.fullName} onChange={(e) => setDetails({ ...details, fullName: e.target.value })} maxLength={255} autoComplete="name" />
                  <Field label="Email" type="email" required value={details.email} onChange={(e) => setDetails({ ...details, email: e.target.value })} autoComplete="email" />
                  <Field label="Phone" type="tel" required pattern="[0-9+() -]{7,20}" value={details.phone} onChange={(e) => setDetails({ ...details, phone: e.target.value })} autoComplete="tel" />
                  <Field label="Age" type="number" required min="1" max="120" value={details.age} onChange={(e) => setDetails({ ...details, age: e.target.value })} />
                  <Field label="Gender" required value={details.gender} onChange={(e) => setDetails({ ...details, gender: e.target.value })} options={[{value:'',label:'Choose one'}, ...['Male','Female','Other','Prefer not to say'].map(value => ({value,label:value}))]} />
                </>}
                {step === 2 && <>
                  <Field label="Cardholder name" required value={card.cardholderName} onChange={(e) => setCard({ ...card, cardholderName: e.target.value })} />
                  <Field label="Card number" required inputMode="numeric" pattern="[0-9]{16}" maxLength={16} value={card.cardNumber} onChange={(e) => setCard({ ...card, cardNumber: e.target.value })} />
                  <Field label="Expiry (MM/YY)" required placeholder="MM/YY" pattern="(0[1-9]|1[0-2])/[0-9]{2}" maxLength={5} value={card.expiry} onChange={(e) => setCard({ ...card, expiry: e.target.value })} />
                  <Field label="CVV" type="password" required inputMode="numeric" pattern="[0-9]{3}" maxLength={3} value={card.cvv} onChange={(e) => setCard({ ...card, cvv: e.target.value })} />
                  <p className="form-note">Demo only. Card details are checked for format and discarded.</p>
                </>}
                {step === 3 && <>
                  <Feedback kind="success">Demo payment details accepted. No SMS or email was sent.</Feedback>
                  <Field label="Four-digit OTP" required inputMode="numeric" pattern="[0-9]{4}" maxLength={4} value={otp} onChange={(e) => setOtp(e.target.value)} />
                </>}
                <div className="passenger-selection"><span>Seat <strong>{selected?.seatNumber}</strong></span></div>
                <div className="form-actions">
                  <button className="button button-secondary" type="button" disabled={busy} onClick={() => setStep(step === 3 ? 2 : step - 1)}>Back</button>
                  <button className="button button-primary" type="submit" disabled={busy}>{busy ? 'Processing…' : step === 1 ? 'Continue to payment' : step === 2 ? 'Continue to OTP' : 'Verify and book'}</button>
                </div>
              </form>
            )}
          </section>
          <aside className="panel journey-summary">
            <span className="eyebrow">YOUR FLIGHT</span>
            <div className="summary-airline">
              <span className="airline-mark">
                {flight.airlineName?.slice(0, 1)}
              </span>
              <div>
                <strong>{flight.airlineName}</strong>
                <small>{flight.flightNumber}</small>
              </div>
            </div>
            <div className="summary-route">
              <div>
                <span className="timeline-dot" />
                <strong>{formatTime(instance.departureDateTime)}</strong>
                <span>{flight.originName}</span>
                <small>{formatDate(instance.departureDateTime)}</small>
              </div>
              <div>
                <span className="timeline-dot destination" />
                <strong>{formatTime(instance.arrivalDateTime)}</strong>
                <span>{flight.destinationName}</span>
                <small>{formatDate(instance.arrivalDateTime)}</small>
              </div>
            </div>
            <dl className="summary-facts">
              <div>
                <dt>Aircraft</dt>
                <dd>{plane.model}</dd>
              </div>
              <div>
                <dt>Status</dt>
                <dd>{statusLabel(instance.status)}</dd>
              </div>
              <div>
                <dt>Seats remaining</dt>
                <dd>{availableSeatCount ?? 'Not available'}</dd>
              </div>
              {selected && (
                <div>
                  <dt>Preferred seat</dt>
                  <dd>{selected.seatNumber}</dd>
                </div>
              )}
              {step > 0 && details.fullName && <div><dt>Passenger</dt><dd>{details.fullName}</dd></div>}
              {step >= 2 && <div><dt>Demo total</dt><dd>₹4,999</dd></div>}
            </dl>
          </aside>
        </div>
      )}
    </div>
  );
}
