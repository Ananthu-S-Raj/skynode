import test from 'node:test';
import assert from 'node:assert/strict';
import { formatDate, formatTime, statusLabel, today } from '../src/format.js';

test('local schedule times do not invent timezone conversion', () => {
  assert.equal(formatTime('2027-05-15T09:25:00'), '09:25');
  assert.equal(formatTime(null), '—');
  assert.match(formatDate('2027-05-15T09:25:00'), /May 15, 2027/);
});
test('invalid and missing date values degrade safely', () => {
  assert.equal(formatDate('bad-date'), 'bad-date');
  assert.equal(formatDate('2027-02-30T09:00:00'), '2027-02-30T09:00:00');
  assert.equal(formatTime('2027-05-15T25:00:00'), '—');
  assert.equal(formatDate(null), '—');
  assert.equal(statusLabel('CONFIRMED'), 'Confirmed');
  assert.match(today(), /^\d{4}-\d{2}-\d{2}$/);
});
