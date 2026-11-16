import test, { afterEach } from 'node:test';
import assert from 'node:assert/strict';
import {
  api,
  request,
  ApiError,
  resolveResults,
  loadBookingContext,
  resetCsrfToken,
} from '../src/services/api.js';

const originalFetch = globalThis.fetch;
afterEach(() => {
  globalThis.fetch = originalFetch;
  resetCsrfToken();
});
const response = (data, status = 200) =>
  new Response(data === null ? null : JSON.stringify(data), { status });
function mockWrite(handler) {
  globalThis.fetch = async (url, init) =>
    url.endsWith('/auth/csrf')
      ? response({ token: 'test-csrf', headerName: 'X-CSRF-TOKEN' })
      : handler(url, init);
}
const flight = {
  id: 7,
  flightNumber: 'SN101',
  airlineName: 'Sky Airline',
  originName: 'Origin',
  destinationName: 'Destination',
  aircraftRegistration: 'REG-1',
};
const instance = {
  id: 12,
  flightId: 7,
  departureDateTime: '2027-05-15T09:00:00',
  arrivalDateTime: '2027-05-15T11:00:00',
  status: 'SCHEDULED',
};
const result = {
  flightNumber: 'SN101',
  airlineName: 'Sky Airline',
  originName: 'Origin',
  destinationName: 'Destination',
  departureTime: instance.departureDateTime,
  arrivalTime: instance.arrivalDateTime,
  status: 'SCHEDULED',
  availableSeatCount: 2,
};
const plane = { id: 3, registrationNumber: 'REG-1', model: 'A320' };

test('search sends only supplied optional filters and preserves empty results', async () => {
  let url;
  globalThis.fetch = async (value) => {
    url = value;
    return response([]);
  };
  assert.deepEqual(await api.search({ from: '1', date: '2027-05-15' }), []);
  assert.equal(
    url,
    '/api/v1/flights/search?originAirportId=1&departureDate=2027-05-15',
  );
  await api.search({ to: '2', date: '2027-05-15' });
  assert.equal(
    url,
    '/api/v1/flights/search?destinationAirportId=2&departureDate=2027-05-15',
  );
});

test('booking sends exactly the existing passenger, instance and seat fields', async () => {
  let sent;
  mockWrite(async (url, init) => {
    sent = { url, init };
    return response({ id: 1, pnr: 'AB12CD34', status: 'CONFIRMED' });
  });
  assert.equal((await api.book('Passenger', 12, 8)).pnr, 'AB12CD34');
  assert.equal(sent.url, '/api/v1/bookings');
  assert.equal(sent.init.method, 'POST');
  assert.deepEqual(JSON.parse(sent.init.body), {
    passengerName: 'Passenger',
    flightInstanceId: 12,
    seatId: 8,
  });
});

test('cancellation uses PUT without an invented request body', async () => {
  mockWrite(async (url, init) => {
    assert.equal(url, '/api/v1/bookings/17/cancel');
    assert.equal(init.method, 'PUT');
    assert.equal(init.body, undefined);
    return response({ id: 17, status: 'CANCELLED' });
  });
  assert.equal((await api.cancel(17)).status, 'CANCELLED');
});

test('conflict status and actual backend message are preserved without write retries', async () => {
  let count = 0;
  mockWrite(async () => {
    count++;
    return response(
      { message: 'Seat is not available for booking on this flight instance' },
      409,
    );
  });
  await assert.rejects(
    api.book('Passenger', 12, 8),
    (error) =>
      error instanceof ApiError &&
      error.status === 409 &&
      /Seat is not available/.test(error.message),
  );
  assert.equal(count, 1);
});

test('PNR paths are encoded and missing bookings remain errors', async () => {
  globalThis.fetch = async (url) => {
    assert.equal(url, '/api/v1/bookings/pnr/A%2FB%3F');
    return response({ message: 'Booking not found' }, 404);
  };
  await assert.rejects(
    api.booking('A/B?'),
    (error) => error.status === 404 && error.message === 'Booking not found',
  );
});

test('network failures, invalid JSON and empty success responses are handled', async () => {
  globalThis.fetch = async () => {
    throw new TypeError('network failure');
  };
  await assert.rejects(api.list('airports'), (error) => error.status === 0);
  globalThis.fetch = async () => new Response('<html>Not JSON</html>');
  await assert.rejects(api.list('airports'), /unexpected response/);
  mockWrite(async () => response(null, 204));
  assert.equal(await api.remove('airports', 1), null);
});

test('aborted reads do not become connection-failure messages', async () => {
  globalThis.fetch = async () => {
    throw new DOMException('Aborted', 'AbortError');
  };
  await assert.rejects(
    request('/airports'),
    (error) => error.name === 'AbortError',
  );
});

test('search results join to a real instance and unique aircraft registration', () => {
  const [resolved] = resolveResults([result], [flight], [instance], [plane]);
  assert.equal(resolved.instanceId, 12);
  assert.equal(resolved.aircraftId, 3);
  assert.equal(resolved.availableSeatCount, 2);
  assert.deepEqual(resolveResults([], [flight], [instance], [plane]), []);
});

test('ambiguous or missing schedules never receive guessed bookable IDs', () => {
  const [ambiguous] = resolveResults(
    [result],
    [flight],
    [instance, { ...instance, id: 13 }],
    [plane],
  );
  assert.equal(ambiguous.instanceId, undefined);
  assert.match(ambiguous.resolutionError, /uniquely/);
  assert.equal(
    resolveResults([result], [flight], [], [plane])[0].instanceId,
    undefined,
  );
  assert.equal(
    resolveResults([result], [flight], [instance], [])[0].instanceId,
    undefined,
  );
});

test('booking context exposes configured seats and aggregate counts without inventing seat states', async () => {
  const data = {
    '/flight-instances/12': instance,
    '/flights/7': flight,
    '/aircraft': [plane],
    '/aircraft/3/seats': [
      { id: 8, aircraftId: 3, seatNumber: '1A', seatClass: 'ECONOMY' },
    ],
    '/flights/search?departureDate=2027-05-15': [result],
  };
  globalThis.fetch = async (url) => response(data[url.replace('/api/v1', '')]);
  const context = await loadBookingContext(12);
  assert.equal(context.availableSeatCount, 2);
  assert.equal(context.seats[0].seatNumber, '1A');
  assert.equal(context.seats[0].status, undefined);
});

test('seat creation and instance updates target supported endpoints', async () => {
  const calls = [];
  mockWrite(async (url, init) => {
    calls.push([url, init.method]);
    return response({ id: 2 });
  });
  await api.addSeat(3, { seatNumber: '1A', seatClass: 'ECONOMY' });
  await api.update('flight-instances', 12, { status: 'DELAYED' });
  assert.deepEqual(calls, [
    ['/api/v1/aircraft/3/seats', 'POST'],
    ['/api/v1/flight-instances/12', 'PUT'],
  ]);
});

test('admin login uses form credentials and verifies the server admin session', async () => {
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push(url);
    assert.equal(init.credentials, 'same-origin');
    if (url.endsWith('/csrf'))
      return response({ token: 'csrf-login', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/login')) {
      assert.equal(init.headers['X-CSRF-TOKEN'], 'csrf-login');
      assert.equal(
        init.headers['Content-Type'],
        'application/x-www-form-urlencoded',
      );
      assert.deepEqual(Object.fromEntries(new URLSearchParams(init.body)), {
        email: 'tester@example.test',
        password: 'test & password',
      });
      return response({ authenticated: true, email: 'tester@example.test', role: 'ADMIN' });
    }
    assert.ok(url.endsWith('/me'));
    return response({ authenticated: true, email: 'tester@example.test', role: 'ADMIN' });
  };
  assert.equal(
    (await api.login('tester@example.test', 'test & password')).authenticated,
    true,
  );
  assert.deepEqual(calls, [
    '/api/v1/auth/csrf',
    '/api/v1/auth/login',
    '/api/v1/auth/me',
    '/api/v1/auth/csrf',
  ]);
});

test('writes share one CSRF fetch and logout refreshes the next token', async () => {
  let tokens = 0;
  globalThis.fetch = async (url, init) => {
    if (url.endsWith('/csrf'))
      return response({
        token: `token-${++tokens}`,
        headerName: 'X-CSRF-TOKEN',
      });
    assert.equal(init.headers['X-CSRF-TOKEN'], `token-${tokens}`);
    return response({ authenticated: false });
  };
  await Promise.all([api.create('airports', {}), api.create('airlines', {})]);
  assert.equal(tokens, 1);
  await api.logout();
  await api.book('Passenger', 12, 8);
  assert.equal(tokens, 2);
});

test('a forbidden write clears stale CSRF state without retrying the write', async () => {
  let tokens = 0,
    writes = 0;
  globalThis.fetch = async (url) => {
    if (url.endsWith('/csrf'))
      return response({
        token: `token-${++tokens}`,
        headerName: 'X-CSRF-TOKEN',
      });
    if (url.endsWith('/me')) return response({ authenticated: false });
    writes++;
    return response({ message: 'Refresh and try again.' }, 403);
  };
  await assert.rejects(
    api.create('flights', {}),
    (error) => error.status === 403,
  );
  assert.equal(writes, 1);
  await assert.rejects(api.create('flights', {}), /Refresh/);
  assert.equal(tokens, 2);
  assert.equal(writes, 2);
});

test('authentication state accepts only a backend-confirmed ADMIN', async () => {
  for (const state of [
    { authenticated: false },
    { authenticated: true, role: 'VIEWER' },
  ]) {
    globalThis.fetch = async (url) => {
      assert.ok(url.endsWith('/auth/me'));
      return response(state);
    };
    assert.equal(await api.session(), null);
  }
});

test('an expired admin write clears authentication without replaying it', async () => {
  let writes = 0;
  const events = [];
  const previousWindow = globalThis.window;
  globalThis.window = { dispatchEvent: (event) => events.push(event.type) };
  globalThis.fetch = async (url) => {
    if (url.endsWith('/csrf'))
      return response({ token: 'stale', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/me')) return response({ authenticated: false });
    writes++;
    return response({ message: 'Security token expired.' }, 403);
  };
  try {
    await assert.rejects(api.create('flights', {}), /Security token expired/);
    assert.equal(writes, 1);
    assert.deepEqual(events, ['skynode:unauthorized']);
  } finally {
    globalThis.window = previousWindow;
  }
});

test('demo payment requests include session CSRF and return the booking', async () => {
  const calls = [];
  globalThis.fetch = async (url, init) => {
    calls.push([url, init]);
    if (url.endsWith('/csrf')) return response({ token: 'demo-csrf', headerName: 'X-CSRF-TOKEN' });
    if (url.endsWith('/start')) return response({ paymentId: 'demo-payment', amount: 4999 });
    return response({ status: 'PAID_DEMO', booking: { pnr: 'DEMO1234' } });
  };
  const payment = await api.startMockPayment({ fullName: 'Demo Passenger', flightInstanceId: 1, seatId: 2 });
  const result = await api.verifyMockPayment(payment.paymentId, '1234');
  assert.equal(result.booking.pnr, 'DEMO1234');
  assert.deepEqual(calls.slice(1).map(([url, init]) => [url, init.method, init.headers['X-CSRF-TOKEN']]), [
    ['/api/v1/mock-payments/start', 'POST', 'demo-csrf'],
    ['/api/v1/mock-payments/verify', 'POST', 'demo-csrf'],
  ]);
  assert.deepEqual(JSON.parse(calls[1][1].body), { fullName: 'Demo Passenger', flightInstanceId: 1, seatId: 2 });
});
