import { useEffect, useState } from 'react';
import { api } from '../services/api';
import Feedback, { EmptyState } from '../components/Feedback';
import Field from '../components/Field';
import Icon from '../components/Icon';
import ConfirmationDialog from '../components/ConfirmationDialog';
import { formatDate, formatTime, statusClass, statusLabel } from '../format';

export default function MyBookings({ initialPnr = '' }) {
  const [pnr, setPnr] = useState(initialPnr);
  const [booking, setBooking] = useState(null);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');
  const [loading, setLoading] = useState(false);
  const [cancelling, setCancelling] = useState(false);
  const [confirm, setConfirm] = useState(false);
  useEffect(() => {
    if (!initialPnr) return;
    const controller = new AbortController();
    setPnr(initialPnr);
    setLoading(true);
    setError('');
    setBooking(null);
    api
      .booking(initialPnr, controller.signal)
      .then(setBooking)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, [initialPnr]);

  async function lookup(event) {
    event.preventDefault();
    if (!pnr.trim() || loading) return;
    setLoading(true);
    setError('');
    setSuccess('');
    setBooking(null);
    try {
      setBooking(await api.booking(pnr.trim().toUpperCase()));
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoading(false);
    }
  }
  async function cancel() {
    setCancelling(true);
    setError('');
    setSuccess('');
    try {
      setBooking(await api.cancel(booking.id));
      setSuccess('Your booking has been cancelled and the seat released.');
    } catch (failure) {
      setError(failure.message);
    } finally {
      setCancelling(false);
      setConfirm(false);
    }
  }
  return (
    <>
      <section className="page-hero">
        <div className="container hero-copy">
          <div>
            <span className="eyebrow">YOUR PLANS, IN ONE PLACE</span>
            <h1>Your journey, at a glance.</h1>
            <p>Enter your PNR to find your booking. No account needed.</p>
          </div>
        </div>
      </section>
      <div className="container page bookings-page hero-content">
        <form className="lookup-form panel" onSubmit={lookup}>
          <Field
            label="Booking reference (PNR)"
            required
            value={pnr}
            onChange={(event) => setPnr(event.target.value.toUpperCase())}
            autoComplete="off"
            autoCapitalize="characters"
            maxLength={255}
            placeholder="Enter your PNR"
            disabled={loading || cancelling}
          />
          <button
            className="button button-primary"
            type="submit"
            disabled={loading || cancelling || !pnr.trim()}
          >
            {loading ? (
              <>
                <span className="spinner" />
                Finding booking…
              </>
            ) : (
              <>
                <Icon name="search" size={18} />
                Find booking
              </>
            )}
          </button>
        </form>
        <Feedback>{error}</Feedback>
        <Feedback kind="success">{success}</Feedback>
        {booking ? (
          <article className="panel booking-detail">
            <div className="booking-detail-top">
              <div>
                <span className="eyebrow">BOOKING REFERENCE</span>
                <strong className="detail-pnr">{booking.pnr}</strong>
              </div>
              <span className={`badge ${statusClass(booking.status)}`}>
                {statusLabel(booking.status)}
              </span>
            </div>
            <div className="booking-detail-route">
              <h2>{booking.origin}</h2>
              <Icon name="arrow" size={24} />
              <h2>{booking.destination}</h2>
            </div>
            <dl className="booking-facts">
              <div>
                <dt>Passenger</dt>
                <dd>{booking.passengerName}</dd>
              </div>
              <div>
                <dt>Flight</dt>
                <dd>{booking.flightNumber}</dd>
              </div>
              <div>
                <dt>Seat</dt>
                <dd>{booking.seatNumber}</dd>
              </div>
              <div>
                <dt>Booked on</dt>
                <dd>{formatDate(booking.bookingTimestamp)}</dd>
              </div>
              {booking.departureDateTime && <div><dt>Departure</dt><dd>{formatDate(booking.departureDateTime)} · {formatTime(booking.departureDateTime)}</dd></div>}
              {booking.arrivalDateTime && <div><dt>Arrival</dt><dd>{formatDate(booking.arrivalDateTime)} · {formatTime(booking.arrivalDateTime)}</dd></div>}
              {booking.passengerEmail && <div><dt>Email</dt><dd>{booking.passengerEmail}</dd></div>}
              {booking.passengerPhone && <div><dt>Phone</dt><dd>{booking.passengerPhone}</dd></div>}
              {booking.passengerAge && <div><dt>Age</dt><dd>{booking.passengerAge}</dd></div>}
              {booking.passengerGender && <div><dt>Gender</dt><dd>{booking.passengerGender}</dd></div>}
              {booking.paymentStatus && <div><dt>Payment</dt><dd>Paid (Demo)</dd></div>}
              {booking.totalAmount && <div><dt>Demo total</dt><dd>₹{booking.totalAmount.toLocaleString('en-IN')}</dd></div>}
            </dl>
            <div className="booking-detail-footer">
              <p>
                <Icon name="info" size={16} />
                Keep your PNR to retrieve this booking again.
              </p>
              {booking.status !== 'CANCELLED' && (
                <button
                  className="button button-danger-outline"
                  type="button"
                  onClick={() => setConfirm(true)}
                  disabled={cancelling || loading}
                >
                  Cancel booking
                </button>
              )}
            </div>
          </article>
        ) : (
          !loading &&
          !error && (
            <EmptyState icon="ticket" title="A booking reference is all you need">
              You’ll find your PNR on the confirmation screen after booking. Enter
              it above to view your trip.
            </EmptyState>
          )
        )}
        {confirm && (
          <ConfirmationDialog
            title="Cancel this booking?"
            confirmLabel="Yes, cancel booking"
            busy={cancelling}
            onConfirm={cancel}
            onCancel={() => setConfirm(false)}
          >
            <p>
              This will cancel booking <strong>{booking.pnr}</strong> for{' '}
              {booking.passengerName} and release seat {booking.seatNumber}. You
              can make a new booking afterwards, subject to availability.
            </p>
          </ConfirmationDialog>
        )}
      </div>
    </>
  );
}
