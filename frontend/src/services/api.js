const API_BASE = (import.meta.env?.VITE_API_BASE_URL || '/api/v1').replace(
  /\/$/,
  '',
);

export class ApiError extends Error {
  constructor(message, status = 0) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

let csrfRequest;
let sessionCheck;
export function resetCsrfToken() {
  csrfRequest = undefined;
}

async function csrfHeaders() {
  if (!csrfRequest) {
    csrfRequest = request('/auth/csrf').catch((error) => {
      resetCsrfToken();
      throw error;
    });
  }
  const csrf = await csrfRequest;
  if (!csrf?.token || !csrf?.headerName) {
    resetCsrfToken();
    throw new ApiError(
      'Unable to initialize request security. Refresh and try again.',
    );
  }
  return { [csrf.headerName]: csrf.token };
}

export async function request(path, { body, form = false, ...options } = {}) {
  const method = options.method || 'GET';
  const headers =
    body === undefined
      ? {}
      : {
          'Content-Type': form
            ? 'application/x-www-form-urlencoded'
            : 'application/json',
        };
  if (!['GET', 'HEAD', 'OPTIONS'].includes(method)) {
    Object.assign(headers, await csrfHeaders());
  }
  let response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      credentials: 'same-origin',
      headers,
      body:
        body === undefined
          ? undefined
          : form
            ? new URLSearchParams(body).toString()
            : JSON.stringify(body),
    });
  } catch (error) {
    if (error.name === 'AbortError') throw error;
    throw new ApiError(
      'Unable to reach SkyNode. Check the backend connection and try again.',
    );
  }
  const text = await response.text();
  let data;
  try {
    data = text ? JSON.parse(text) : null;
  } catch {
    data = null;
  }
  if (!response.ok) {
    if (response.status === 403) resetCsrfToken();
    if (
      response.status === 401 &&
      !['/auth/me', '/auth/session', '/auth/login'].includes(path) &&
      typeof window !== 'undefined'
    ) {
      window.dispatchEvent(new Event('skynode:unauthorized'));
    }
    if (
      response.status === 403 &&
      /^\/(airports|airlines|aircraft|flights|flight-instances)(\/|$)/.test(path)
    ) {
      // An expired session can fail CSRF before authorization. Check it without retrying the write.
      if (!sessionCheck) {
        sessionCheck = api.session()
          .then((session) => {
            if (!session && typeof window !== 'undefined')
              window.dispatchEvent(new Event('skynode:unauthorized'));
          })
          .catch(() => {})
          .finally(() => { sessionCheck = undefined; });
      }
      await sessionCheck;
    }
    throw new ApiError(
      data?.message ||
        `The request could not be completed (HTTP ${response.status}).`,
      response.status,
    );
  }
  if (text && data === null)
    throw new ApiError(
      'SkyNode returned an unexpected response. Please try again.',
    );
  return data;
}

export const api = {
  session: async (signal) => {
    const state = await request('/auth/me', { signal });
    return state?.authenticated && state.role === 'ADMIN' ? state : null;
  },
  login: async (email, password) => {
    await request('/auth/login', {
      method: 'POST',
      form: true,
      body: { email, password },
    });
    resetCsrfToken();
    const session = await api.session();
    if (!session) throw new ApiError('Administrator access is required.', 403);
    await csrfHeaders();
    return session;
  },
  logout: async () => {
    const result = await request('/auth/logout', { method: 'POST' });
    resetCsrfToken();
    // Logout has succeeded even if this housekeeping read fails; a later write will fetch again.
    await csrfHeaders().catch(() => {});
    return result;
  },
  list: (resource, signal) => request(`/${resource}`, { signal }),
  get: (resource, id, signal) =>
    request(`/${resource}/${encodeURIComponent(id)}`, { signal }),
  create: (resource, body, query = '') =>
    request(`/${resource}${query}`, { method: 'POST', body }),
  update: (resource, id, body, query = '') =>
    request(`/${resource}/${encodeURIComponent(id)}${query}`, {
      method: 'PUT',
      body,
    }),
  remove: (resource, id) =>
    request(`/${resource}/${encodeURIComponent(id)}`, { method: 'DELETE' }),
  seats: (aircraftId, signal) =>
    request(`/aircraft/${encodeURIComponent(aircraftId)}/seats`, { signal }),
  addSeat: (aircraftId, body) =>
    request(`/aircraft/${encodeURIComponent(aircraftId)}/seats`, {
      method: 'POST',
      body,
    }),
  search: ({ from, to, date }, signal) => {
    const query = new URLSearchParams();
    if (from) query.set('originAirportId', from);
    if (to) query.set('destinationAirportId', to);
    if (date) query.set('departureDate', date);
    return request(`/flights/search?${query}`, { signal });
  },
  startMockPayment: (details) =>
    request('/mock-payments/start', { method: 'POST', body: details }),
  verifyMockPayment: (paymentId, otp) =>
    request('/mock-payments/verify', { method: 'POST', body: { paymentId, otp } }),
  book: (passengerName, flightInstanceId, seatId) =>
    request('/bookings', {
      method: 'POST',
      body: { passengerName, flightInstanceId, seatId },
    }),
  booking: (pnr, signal) =>
    request(`/bookings/pnr/${encodeURIComponent(pnr)}`, { signal }),
  cancel: (id) =>
    request(`/bookings/${encodeURIComponent(id)}/cancel`, { method: 'PUT' }),
};

export function matchesSearchResult(result, flight, instance) {
  return (
    result.flightNumber === flight.flightNumber &&
    result.airlineName === flight.airlineName &&
    result.originName === flight.originName &&
    result.destinationName === flight.destinationName &&
    result.departureTime === instance.departureDateTime &&
    result.arrivalTime === instance.arrivalDateTime &&
    result.status === instance.status
  );
}

// Search responses omit instance IDs. Resolve only unambiguous matches; never guess an ID.
export function resolveResults(results, flights, instances, aircraft) {
  return results.map((result) => {
    const candidates = instances.flatMap((instance) =>
      flights
        .filter(
          (flight) =>
            flight.id === instance.flightId &&
            matchesSearchResult(result, flight, instance),
        )
        .map((flight) => ({ instance, flight })),
    );
    if (candidates.length !== 1)
      return {
        ...result,
        resolutionError:
          'This departure could not be identified uniquely. Refresh or check the schedule in Admin.',
      };
    const { instance, flight } = candidates[0];
    const planes = aircraft.filter(
      (plane) => plane.registrationNumber === flight.aircraftRegistration,
    );
    if (planes.length !== 1)
      return {
        ...result,
        resolutionError:
          'The aircraft for this departure could not be identified.',
      };
    return {
      ...result,
      instanceId: instance.id,
      aircraftId: planes[0].id,
      aircraftModel: planes[0].model,
    };
  });
}

export async function loadResults(search, signal) {
  const [results, flights, instances, aircraft] = await Promise.all([
    api.search(search, signal),
    api.list('flights', signal),
    api.list('flight-instances', signal),
    api.list('aircraft', signal),
  ]);
  return resolveResults(results, flights, instances, aircraft);
}

export async function loadBookingContext(instanceId, signal) {
  const instance = await api.get('flight-instances', instanceId, signal);
  const [flight, aircraft] = await Promise.all([
    api.get('flights', instance.flightId, signal),
    api.list('aircraft', signal),
  ]);
  const planes = aircraft.filter(
    (plane) => plane.registrationNumber === flight.aircraftRegistration,
  );
  if (planes.length !== 1)
    throw new ApiError(
      'The aircraft for this departure could not be identified. Please choose the flight again.',
    );
  const plane = planes[0];
  const [seats, results] = await Promise.all([
    api.seats(plane.id, signal),
    api.search({ date: instance.departureDateTime.slice(0, 10) }, signal),
  ]);
  const matches = results.filter((result) =>
    matchesSearchResult(result, flight, instance),
  );
  return {
    instance,
    flight,
    plane,
    seats,
    availableSeatCount:
      matches.length === 1 ? matches[0].availableSeatCount : null,
  };
}

export const catalogResources = [
  'airports',
  'airlines',
  'aircraft',
  'flights',
  'flight-instances',
];
export async function loadCatalog(signal) {
  const lists = await Promise.all(
    catalogResources.map((resource) => api.list(resource, signal)),
  );
  return Object.fromEntries(
    catalogResources.map((resource, index) => [resource, lists[index]]),
  );
}
