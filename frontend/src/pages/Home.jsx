import { useEffect, useState } from 'react';
import SearchForm from '../components/SearchForm';
import Icon from '../components/Icon';
import Feedback from '../components/Feedback';
import { api } from '../services/api';
import { today } from '../format';
import kochiPhoto from '../assets/kochi.jpg';
import chennaiPhoto from '../assets/chennai.jpg';
import bengaluruPhoto from '../assets/bengaluru.jpg';

const photos = { Kochi: kochiPhoto, Chennai: chennaiPhoto, Bengaluru: bengaluruPhoto };

export default function Home({ airports, loading, error, onRetry }) {
  const [flights, setFlights] = useState([]);
  useEffect(() => {
    const controller = new AbortController();
    api.list('flights', controller.signal).then(setFlights).catch(() => setFlights([]));
    return () => controller.abort();
  }, []);
  const byName = new Map(airports.map((airport) => [airport.name, airport]));
  const routes = flights.flatMap((flight) => {
    const from = byName.get(flight.originName);
    const to = byName.get(flight.destinationName);
    return from && to ? [{ from, to, id: flight.id }] : [];
  }).slice(0, 4);
  const destinations = [...new Map(routes.map(({ to }) => [to.id, to])).values()]
    .filter((airport) => photos[airport.city])
    .slice(0, 3);
  return (
    <>
      <section className="home-hero">
        <div className="container">
          <div className="hero-copy">
            <div>
              <span className="eyebrow">
                <span className="eyebrow-line" />
                YOUR NEXT JOURNEY STARTS HERE
              </span>
              <h1>
                Seamless journeys.<br /><span>Connected skies.</span>
              </h1>
              <p>
                Discover your next departure, choose your seat, and travel with confidence.
              </p>
            </div>
          </div>
          <SearchForm airports={airports} loading={loading} />
          <Feedback onRetry={onRetry}>{error}</Feedback>
          <div className="hero-bottom">
            <span>
              <Icon name="globe" size={16} />A simpler journey, from the first
              search.
            </span>
            <a href="#/bookings">
              Already booked? Find your trip
              <Icon name="arrow" size={16} />
            </a>
          </div>
        </div>
      </section>
      {destinations.length > 0 && (
        <section className="container destinations-section" aria-label="Destinations">
          <div className="section-heading"><span className="eyebrow">EXPLORE THE NETWORK</span><h2>Places worth discovering</h2><p>Destinations served by flights listed in SkyNode.</p></div>
          <div className="destination-grid">
            {destinations.map((airport) => (
              <article className="destination-card" key={airport.id}>
                {photos[airport.city] && <img src={photos[airport.city]} alt="" loading="lazy" />}
                <div className="destination-caption"><small>DESTINATION</small><h3>{airport.city}</h3><p>{airport.name}</p></div>
              </article>
            ))}
          </div>
        </section>
      )}
      {routes.length > 0 && (
        <section className="container popular-routes" aria-label="Routes to explore">
          <div className="section-heading"><span className="eyebrow">FLY WITH SKYNODE</span><h2>Routes to explore</h2><p>Choose a route and date to see scheduled flights.</p></div>
          <div className="route-list">{routes.map(({ from, to, id }) => (
            <a key={id} href={`#/flights?${new URLSearchParams({ from: String(from.id), to: String(to.id), date: today() })}`}>
              <span><strong>{from.city}</strong><small>{from.name}</small></span><Icon name="arrow" size={18} />
              <span><strong>{to.city}</strong><small>{to.name}</small></span><Icon name="arrow" size={18} />
            </a>
          ))}</div>
        </section>
      )}
      <section
        className="container journey-benefits"
        aria-label="Travel with SkyNode"
      >
        <div className="section-intro">
          <span className="eyebrow">LESS PLANNING. MORE POSSIBILITIES.</span>
          <h2>
            A little simpler.
            <br />A lot more SkyNode.
          </h2>
        </div>
        <div className="benefit">
          <span className="feature-icon">
            <Icon name="search" size={23} />
          </span>
          <h3>Find your way</h3>
          <p>Explore scheduled departures and see how many seats remain.</p>
        </div>
        <div className="benefit">
          <span className="feature-icon">
            <Icon name="seat" size={23} />
          </span>
          <h3>Make it your journey</h3>
          <p>Choose a preferred seat and complete a simple demo checkout.</p>
        </div>
        <div className="benefit">
          <span className="feature-icon">
            <Icon name="ticket" size={23} />
          </span>
          <h3>Keep plans in reach</h3>
          <p>
            Your PNR brings your booking back. Look it up or cancel in a few
            clicks.
          </p>
        </div>
      </section>
      <section className="container booking-banner">
        <div>
          <Icon name="ticket" size={28} />
          <div>
            <h2>Good plans deserve a place to land.</h2>
            <p>Find your booking, check the details, and take the next step.</p>
          </div>
        </div>
        <a href="#/bookings" className="button button-secondary">
          Manage my booking
          <Icon name="arrow" size={17} />
        </a>
      </section>
    </>
  );
}
