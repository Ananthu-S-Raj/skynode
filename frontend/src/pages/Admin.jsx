import { useEffect, useState } from 'react';
import { api, loadCatalog } from '../services/api';
import Field from '../components/Field';
import Icon from '../components/Icon';
import Feedback, { EmptyState, Loading } from '../components/Feedback';
import ConfirmationDialog from '../components/ConfirmationDialog';
import DataTable from '../components/DataTable';
import { formatDate, formatTime, statusClass, statusLabel } from '../format';

const sections = [
  {
    key: 'airports',
    label: 'Airports',
    singular: 'airport',
    icon: 'pin',
    description: 'The places your journeys begin and end.',
    fields: ['name', 'city', 'country'],
    columns: ['id', 'name', 'city', 'country'],
    remove: true,
  },
  {
    key: 'airlines',
    label: 'Airlines',
    singular: 'airline',
    icon: 'plane',
    description: 'Manage the airlines in your flight network.',
    fields: ['name', 'code'],
    columns: ['id', 'name', 'code'],
    edit: true,
    remove: true,
  },
  {
    key: 'aircraft',
    label: 'Aircraft',
    singular: 'aircraft',
    icon: 'plane',
    description: 'Keep aircraft details and airline assignments in order.',
    fields: ['model', 'registrationNumber', 'airlineId'],
    columns: ['id', 'model', 'registrationNumber', 'airlineName'],
    edit: true,
    remove: true,
  },
  {
    key: 'seats',
    label: 'Aircraft Seats',
    singular: 'seat',
    icon: 'seat',
    description: 'Configure seats before scheduling new departures.',
    fields: ['seatNumber', 'seatClass'],
    columns: ['id', 'seatNumber', 'seatClass'],
  },
  {
    key: 'flights',
    label: 'Flights',
    singular: 'flight',
    icon: 'globe',
    description: 'Connect an airline, aircraft and route.',
    fields: [
      'flightNumber',
      'airlineId',
      'aircraftId',
      'originAirportId',
      'destinationAirportId',
    ],
    columns: [
      'id',
      'flightNumber',
      'airlineName',
      'originName',
      'destinationName',
      'aircraftRegistration',
    ],
    edit: true,
    remove: true,
  },
  {
    key: 'flight-instances',
    label: 'Flight Instances',
    singular: 'departure',
    icon: 'calendar',
    description: 'Schedule departures and manage their times and status.',
    fields: ['flightId', 'departureDateTime', 'arrivalDateTime', 'status'],
    columns: [
      'id',
      'flightId',
      'departureDateTime',
      'arrivalDateTime',
      'status',
    ],
    edit: true,
  },
];
const labels = {
  id: 'ID',
  name: 'Name',
  city: 'City',
  country: 'Country',
  code: 'Code',
  model: 'Model',
  registrationNumber: 'Registration',
  airlineId: 'Airline',
  airlineName: 'Airline',
  aircraftId: 'Aircraft',
  seatNumber: 'Seat number',
  seatClass: 'Seat class',
  flightNumber: 'Flight number',
  originAirportId: 'Departure airport',
  destinationAirportId: 'Arrival airport',
  originName: 'From',
  destinationName: 'To',
  aircraftRegistration: 'Aircraft registration',
  flightId: 'Flight',
  departureDateTime: 'Departure',
  arrivalDateTime: 'Arrival',
  status: 'Status',
};
const statuses = [
  'SCHEDULED',
  'DELAYED',
  'BOARDING',
  'DEPARTED',
  'ARRIVED',
  'CANCELLED',
];
const emptyCatalog = {
  airports: [],
  airlines: [],
  aircraft: [],
  flights: [],
  'flight-instances': [],
};
function uniqueId(rows, field, value) {
  const matches = rows.filter((row) => row[field] === value);
  return matches.length === 1 ? String(matches[0].id) : '';
}

export default function Admin() {
  const [active, setActive] = useState('airports');
  const [catalog, setCatalog] = useState(emptyCatalog);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [notice, setNotice] = useState('');
  const [seatAircraft, setSeatAircraft] = useState('');
  const [seats, setSeats] = useState([]);
  const [seatsLoading, setSeatsLoading] = useState(false);
  const [showForm, setShowForm] = useState(false);
  const [editing, setEditing] = useState(null);
  const [values, setValues] = useState({});
  const [formError, setFormError] = useState('');
  const [busy, setBusy] = useState(false);
  const [deleting, setDeleting] = useState(null);
  const section = sections.find((item) => item.key === active);
  const rows = active === 'seats' ? seats : catalog[active];

  useEffect(() => {
    const controller = new AbortController();
    loadCatalog(controller.signal)
      .then(setCatalog)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setLoading(false);
      });
    return () => controller.abort();
  }, []);
  useEffect(() => {
    if (!seatAircraft) {
      setSeats([]);
      return;
    }
    const controller = new AbortController();
    setSeatsLoading(true);
    setSeats([]);
    api
      .seats(seatAircraft, controller.signal)
      .then(setSeats)
      .catch((failure) => {
        if (failure.name !== 'AbortError') setError(failure.message);
      })
      .finally(() => {
        if (!controller.signal.aborted) setSeatsLoading(false);
      });
    return () => controller.abort();
  }, [seatAircraft]);

  async function refresh() {
    setLoading(true);
    setError('');
    try {
      const next = await loadCatalog();
      setCatalog(next);
      if (
        seatAircraft &&
        next.aircraft.some((plane) => String(plane.id) === seatAircraft)
      ) {
        setSeats(await api.seats(seatAircraft));
      } else if (seatAircraft) {
        setSeatAircraft('');
        setSeats([]);
      }
    } catch (failure) {
      setError(failure.message);
    } finally {
      setLoading(false);
    }
  }
  function chooseSection(key) {
    setActive(key);
    setShowForm(false);
    setEditing(null);
    setFormError('');
    setNotice('');
  }
  function openForm(row = null) {
    const next = Object.fromEntries(
      section.fields.map((field) => [
        field,
        row?.[field] ?? (field === 'status' ? 'SCHEDULED' : ''),
      ]),
    );
    if (row && active === 'aircraft')
      next.airlineId = uniqueId(catalog.airlines, 'name', row.airlineName);
    if (row && active === 'flights') {
      next.airlineId = uniqueId(catalog.airlines, 'name', row.airlineName);
      next.aircraftId = uniqueId(
        catalog.aircraft,
        'registrationNumber',
        row.aircraftRegistration,
      );
      next.originAirportId = uniqueId(catalog.airports, 'name', row.originName);
      next.destinationAirportId = uniqueId(
        catalog.airports,
        'name',
        row.destinationName,
      );
    }
    setValues(next);
    setEditing(row);
    setShowForm(true);
    setFormError('');
    setNotice('');
  }
  function options(field) {
    let result;
    if (field === 'airlineId')
      result = catalog.airlines.map((row) => ({
        value: row.id,
        label: `${row.name} · ${row.code}`,
      }));
    if (field === 'aircraftId')
      result = catalog.aircraft.map((row) => ({
        value: row.id,
        label: `${row.model} · ${row.registrationNumber}`,
      }));
    if (field.endsWith('AirportId'))
      result = catalog.airports.map((row) => ({
        value: row.id,
        label: `${row.city} · ${row.name} (#${row.id})`,
      }));
    if (field === 'flightId')
      result = catalog.flights.map((row) => ({
        value: row.id,
        label: `${row.flightNumber} · ${row.originName} → ${row.destinationName} (#${row.id})`,
      }));
    if (field === 'status')
      return statuses.map((status) => ({
        value: status,
        label: statusLabel(status),
      }));
    return result
      ? [
          { value: '', label: `Choose ${labels[field].toLowerCase()}` },
          ...result,
        ]
      : undefined;
  }
  async function save(event) {
    event.preventDefault();
    if (busy) return;
    setFormError('');
    if (
      active === 'flights' &&
      values.originAirportId === values.destinationAirportId
    ) {
      setFormError('Departure and arrival airports must be different.');
      return;
    }
    if (
      active === 'flight-instances' &&
      new Date(values.arrivalDateTime) <= new Date(values.departureDateTime)
    ) {
      setFormError('Arrival must be after departure.');
      return;
    }
    if (active === 'seats' && !seatAircraft) {
      setFormError('Choose an aircraft first.');
      return;
    }
    const payload = Object.fromEntries(
      section.fields
        .filter(
          (field) =>
            !(editing && active === 'flight-instances' && field === 'flightId'),
        )
        .map((field) => [
          field,
          field.endsWith('Id')
            ? Number(values[field])
            : String(values[field] ?? '').trim(),
        ]),
    );
    const query =
      active === 'aircraft' ? `?airlineId=${payload.airlineId}` : '';
    if (active === 'aircraft') delete payload.airlineId;
    setBusy(true);
    try {
      if (active === 'seats') await api.addSeat(seatAircraft, payload);
      else if (editing) await api.update(active, editing.id, payload, query);
      else await api.create(active, payload, query);
      setNotice(
        `${section.singular.charAt(0).toUpperCase() + section.singular.slice(1)} ${editing ? 'updated' : 'created'} successfully.`,
      );
      setShowForm(false);
      setEditing(null);
      await refresh();
    } catch (failure) {
      setFormError(failure.message);
    } finally {
      setBusy(false);
    }
  }
  async function remove() {
    setBusy(true);
    setError('');
    try {
      await api.remove(active, deleting.id);
      setNotice(
        `${section.singular.charAt(0).toUpperCase() + section.singular.slice(1)} deleted.`,
      );
      if (editing?.id === deleting.id) {
        setShowForm(false);
        setEditing(null);
      }
      setDeleting(null);
      await refresh();
    } catch (failure) {
      setError(failure.message);
      setDeleting(null);
    } finally {
      setBusy(false);
    }
  }
  const columns = section.columns.map((key) => ({
    key,
    label: labels[key],
    render:
      key === 'status'
        ? (row) => (
            <span className={`badge ${statusClass(row.status)}`}>
              {statusLabel(row.status)}
            </span>
          )
        : key.endsWith('DateTime')
          ? (row) => (
              <>
                <strong className="table-time">{formatTime(row[key])}</strong>
                <small className="table-date">
                  {formatDate(row[key], true)}
                </small>
              </>
            )
          : key === 'flightId'
            ? (row) => (
                <>
                  {catalog.flights.find((flight) => flight.id === row.flightId)
                    ?.flightNumber || `#${row.flightId}`}
                </>
              )
            : undefined,
  }));
  return (
    <>
      <section className="page-hero">
        <div className="container hero-copy admin-hero-copy">
        <div>
          <span className="eyebrow">SKYNODE WORKSPACE</span>
          <h1>A well-connected journey.</h1>
          <p>Manage the details that keep your flight network moving.</p>
        </div>
        <button
          className="button button-secondary"
          type="button"
          onClick={refresh}
          disabled={loading || busy}
        >
          <Icon name="refresh" size={17} />
          Refresh
        </button>
        </div>
      </section>
      <div className="container page admin-page hero-content">
      <div className="admin-access-note">
        <Icon name="info" size={16} />
        Signed-in admin workspace. Management changes require an active admin
        session.
      </div>
      <div className="admin-layout">
        <aside className="admin-sidebar">
          <span className="eyebrow">MANAGE</span>
          <nav aria-label="Admin resources">
            {sections.map((item) => (
              <button
                type="button"
                key={item.key}
                className={
                  active === item.key ? 'admin-nav active' : 'admin-nav'
                }
                onClick={() => chooseSection(item.key)}
                disabled={busy}
                aria-current={active === item.key ? 'page' : undefined}
              >
                <Icon name={item.icon} size={18} />
                {item.label}
                <span>
                  {loading
                    ? '—'
                    : item.key === 'seats'
                      ? seatAircraft
                        ? seats.length
                        : '—'
                      : catalog[item.key].length}
                </span>
              </button>
            ))}
          </nav>
          <div className="sidebar-tip">
            <Icon name="info" size={18} />
            <p>
              Start with airports and airlines. Add aircraft and seats before
              scheduling flights.
            </p>
          </div>
        </aside>
        <section className="admin-content">
          <div className="resource-heading">
            <div>
              <h2>{section.label}</h2>
              <p>{section.description}</p>
            </div>
            <button
              className="button button-primary"
              type="button"
              onClick={() => openForm()}
              disabled={
                loading || busy || (active === 'seats' && !seatAircraft)
              }
            >
              <Icon name="plus" size={17} />
              Add {section.singular}
            </button>
          </div>
          <Feedback onRetry={refresh}>{error}</Feedback>
          <Feedback kind="success">{notice}</Feedback>
          {active === 'seats' && (
            <div className="panel aircraft-filter">
              <Field
                label="Aircraft"
                options={[
                  { value: '', label: 'Choose an aircraft to configure' },
                  ...catalog.aircraft.map((row) => ({
                    value: row.id,
                    label: `${row.model} · ${row.registrationNumber}`,
                  })),
                ]}
                value={seatAircraft}
                onChange={(event) => {
                  setSeatAircraft(event.target.value);
                  setShowForm(false);
                  setNotice('');
                }}
                disabled={busy}
              />
            </div>
          )}
          {showForm && (
            <form className="panel resource-form" onSubmit={save}>
              <div className="panel-heading">
                <h3>
                  {editing
                    ? `Edit ${section.singular} #${editing.id}`
                    : `Add ${section.singular}`}
                </h3>
                <button
                  className="icon-button"
                  type="button"
                  aria-label="Close form"
                  disabled={busy}
                  onClick={() => setShowForm(false)}
                >
                  <Icon name="close" size={18} />
                </button>
              </div>
              {editing && ['aircraft', 'flights'].includes(active) && (
                <p className="form-note">
                  Confirm the relationship selections before saving. If a name
                  matches multiple records, choose the intended record
                  explicitly.
                </p>
              )}
              {editing && active === 'flights' && (
                <p className="form-note">
                  An aircraft cannot be changed once departures have been
                  scheduled.
                </p>
              )}
              <div className="admin-form-grid">
                {section.fields.map((field) => (
                  <Field
                    key={field}
                    label={labels[field]}
                    name={field}
                    required={field !== 'seatClass'}
                    type={
                      field.endsWith('DateTime') ? 'datetime-local' : 'text'
                    }
                    step={field.endsWith('DateTime') ? '1' : undefined}
                    maxLength={field === 'seatClass' ? 20 : 255}
                    options={options(field)}
                    value={values[field] ?? ''}
                    onChange={(event) => {
                      setValues({ ...values, [field]: event.target.value });
                      setFormError('');
                    }}
                    disabled={
                      busy ||
                      (Boolean(editing) &&
                        active === 'flight-instances' &&
                        field === 'flightId')
                    }
                  />
                ))}
              </div>
              <Feedback>{formError}</Feedback>
              <div className="form-actions">
                <button
                  type="button"
                  className="button button-secondary"
                  disabled={busy}
                  onClick={() => setShowForm(false)}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="button button-primary"
                  disabled={busy}
                >
                  {busy ? (
                    <>
                      <span className="spinner" />
                      Saving…
                    </>
                  ) : editing ? (
                    'Save changes'
                  ) : (
                    `Create ${section.singular}`
                  )}
                </button>
              </div>
            </form>
          )}
          <div className="panel resource-table">
            {loading || seatsLoading ? (
              <Loading>Loading {section.label.toLowerCase()}…</Loading>
            ) : active === 'seats' && !seatAircraft ? (
              <EmptyState icon="seat" title="Choose an aircraft first">
                Select an aircraft above to view its configured seats.
              </EmptyState>
            ) : rows.length ? (
              <>
                <div className="table-caption">
                  {rows.length} {rows.length === 1 ? 'record' : 'records'}
                </div>
                <DataTable
                  columns={columns}
                  rows={rows}
                  actions={
                    section.edit || section.remove
                      ? (row) => (
                          <>
                            {section.edit && (
                              <button
                                type="button"
                                className="table-button"
                                onClick={() => openForm(row)}
                                disabled={busy}
                              >
                                Edit
                              </button>
                            )}
                            {section.remove && (
                              <button
                                type="button"
                                className="table-button destructive"
                                onClick={() => setDeleting(row)}
                                disabled={busy}
                              >
                                Delete
                              </button>
                            )}
                          </>
                        )
                      : undefined
                  }
                />
              </>
            ) : (
              <EmptyState
                icon={section.icon}
                title={`No ${section.label.toLowerCase()} yet`}
              >
                Add your first {section.singular} to build your flight network.
              </EmptyState>
            )}
          </div>
        </section>
      </div>
      {deleting && (
        <ConfirmationDialog
          title={`Delete this ${section.singular}?`}
          confirmLabel={`Delete ${section.singular}`}
          busy={busy}
          onConfirm={remove}
          onCancel={() => setDeleting(null)}
        >
          <p>
            This will delete record <strong>#{deleting.id}</strong>
            {deleting.name ? ` (${deleting.name})` : ''}. Related records may
            prevent deletion. This action cannot be undone.
          </p>
        </ConfirmationDialog>
      )}
      </div>
    </>
  );
}
