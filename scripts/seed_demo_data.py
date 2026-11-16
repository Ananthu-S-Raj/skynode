#!/usr/bin/env python3
"""Seed SkyNode through its existing HTTP API. Run manually against a trusted local server."""
import argparse
import collections
import datetime as dt
import getpass
import json
import sys
import urllib.error
import urllib.parse
import urllib.request
import http.cookiejar

AIRPORTS = [
    ("Cochin International Airport", "Kochi", "India"),
    ("Chennai International Airport", "Chennai", "India"),
    ("Kempegowda International Airport", "Bengaluru", "India"),
    ("Indira Gandhi International Airport", "New Delhi", "India"),
    ("Chhatrapati Shivaji Maharaj International Airport", "Mumbai", "India"),
    ("Rajiv Gandhi International Airport", "Hyderabad", "India"),
    ("Trivandrum International Airport", "Thiruvananthapuram", "India"),
    ("Calicut International Airport", "Kozhikode", "India"),
    ("Goa International Airport", "Goa", "India"),
    ("Netaji Subhas Chandra Bose International Airport", "Kolkata", "India"),
    ("Dubai International Airport", "Dubai", "UAE"),
    ("Hamad International Airport", "Doha", "Qatar"),
    ("Singapore Changi Airport", "Singapore", "Singapore"),
    ("Heathrow Airport", "London", "United Kingdom"),
    ("John F. Kennedy International Airport", "New York", "United States"),
]
AIRLINES = [("SkyNode Air", "SND"), ("Horizon Airways", "HRZ"),
            ("Coastal Wings", "CWL"), ("Meridian Airlines", "MDN"),
            ("BlueRoute Aviation", "BRV")]
MODELS = ("Airbus A320", "Boeing 737-800", "Airbus A321", "Airbus A320neo",
          "Boeing 787-8", "Airbus A330-200", "Airbus A320", "Boeing 737 MAX 8",
          "Airbus A350-900", "Boeing 787-9")
# Airline index, origin city, destination city, local departure hour, duration hours.
ROUTES = [
    (0, "Kochi", "Chennai", 7, 1.5), (1, "Chennai", "Kochi", 16, 1.5),
    (2, "Kochi", "Bengaluru", 9, 1.25), (3, "Bengaluru", "Kochi", 18, 1.25),
    (4, "Chennai", "Bengaluru", 11, 1.25), (0, "Bengaluru", "Hyderabad", 8, 1.25),
    (1, "Mumbai", "New Delhi", 6, 2.25), (2, "New Delhi", "Mumbai", 18, 2.25),
    (3, "Hyderabad", "Chennai", 14, 1.5), (4, "Thiruvananthapuram", "Mumbai", 10, 2.5),
    (0, "Kozhikode", "Bengaluru", 12, 1.25), (1, "Kolkata", "New Delhi", 15, 2.25),
    (2, "Kochi", "Dubai", 20, 4), (3, "Dubai", "Kochi", 13, 4),
    (4, "Chennai", "Dubai", 21, 4.25), (0, "Bengaluru", "Singapore", 22, 4.5),
    (1, "New Delhi", "London", 4, 9.5), (2, "Mumbai", "Doha", 19, 4),
    (3, "Dubai", "London", 9, 7.5), (4, "London", "New York", 11, 8),
    (0, "Singapore", "Chennai", 7, 4), (1, "Doha", "Kochi", 14, 4),
    (2, "Goa", "Mumbai", 12, 1.25), (3, "Mumbai", "Goa", 9, 1.25),
    (4, "Kochi", "Hyderabad", 17, 1.75), (0, "Bengaluru", "Mumbai", 13, 2),
    (1, "New Delhi", "Bengaluru", 10, 2.75), (2, "Chennai", "Kolkata", 8, 2.25),
    (3, "Hyderabad", "Kochi", 19, 1.75), (4, "Mumbai", "Singapore", 23, 5.5),
]

class Api:
    def __init__(self, base):
        self.base = base.rstrip('/') + '/api/v1'
        self.opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
        self.csrf = None

    def call(self, method, path, body=None, form=False):
        headers = {"Accept": "application/json"}
        if method != "GET":
            if self.csrf is None:
                self.csrf = self.call("GET", "/auth/csrf")
            headers[self.csrf["headerName"]] = self.csrf["token"]
        data = None
        if body is not None:
            headers["Content-Type"] = "application/x-www-form-urlencoded" if form else "application/json"
            data = (urllib.parse.urlencode(body) if form else json.dumps(body)).encode()
        request = urllib.request.Request(self.base + path, data=data, headers=headers, method=method)
        try:
            with self.opener.open(request, timeout=15) as response:
                raw = response.read()
                return json.loads(raw) if raw else None
        except urllib.error.HTTPError as error:
            detail = error.read().decode(errors="replace")
            try:
                detail = json.loads(detail).get("message", "Request failed")
            except ValueError:
                detail = "Request failed"
            raise RuntimeError(f"{method} {path}: HTTP {error.code}: {detail}") from None

    def login(self, email, password):
        self.call("POST", "/auth/login", {"email": email, "password": password}, form=True)
        self.csrf = None
        state = self.call("GET", "/auth/me")
        if not state.get("authenticated") or state.get("role") != "ADMIN":
            raise RuntimeError("Administrator session was not established")


def unique(rows, key, value):
    matches = [row for row in rows if key(row) == value]
    if len(matches) > 1:
        raise RuntimeError(f"Ambiguous existing record for {value}; no changes made to that record")
    return matches[0] if matches else None


def ensure(api, counts, kind, rows, key, value, path, body):
    found = unique(rows, key, value)
    if found:
        counts[kind]["skipped"] += 1
        return found
    created = api.call("POST", path, body)
    rows.append(created)
    counts[kind]["created"] += 1
    return created


def seed(api, start):
    counts = collections.defaultdict(lambda: collections.Counter())
    airport_rows = api.call("GET", "/airports")
    airline_rows = api.call("GET", "/airlines")
    aircraft_rows = api.call("GET", "/aircraft")
    flight_rows = api.call("GET", "/flights")
    instance_rows = api.call("GET", "/flight-instances")
    airports = {}
    airlines = {}
    aircraft = {}
    for name, city, country in AIRPORTS:
        row = ensure(api, counts, "airports", airport_rows,
                     lambda x: (x["name"].casefold(), x["city"].casefold(), x["country"].casefold()),
                     (name.casefold(), city.casefold(), country.casefold()),
                     "/airports", {"name": name, "city": city, "country": country})
        airports[city] = row
    for name, code in AIRLINES:
        row = ensure(api, counts, "airlines", airline_rows, lambda x: x["code"].casefold(),
                     code.casefold(), "/airlines", {"name": name, "code": code})
        if row["name"] != name:
            raise RuntimeError(f"Airline code {code} belongs to a different name; stop safely")
        airlines[name] = row
    for index, model in enumerate(MODELS):
        name, _ = AIRLINES[index % len(AIRLINES)]
        registration = f"DEMO-SN-{index + 1:02d}"
        row = ensure(api, counts, "aircraft", aircraft_rows,
                     lambda x: x["registrationNumber"].casefold(), registration.casefold(),
                     "/aircraft?airlineId=" + str(airlines[name]["id"]),
                     {"model": model, "registrationNumber": registration})
        if row["model"] != model or row["airlineName"] != name:
            raise RuntimeError(f"Aircraft {registration} conflicts with the demo definition")
        aircraft[index] = row
        seats = api.call("GET", f"/aircraft/{row['id']}/seats")
        for seat_row in range(1, 7):
            for letter in "ABCD":
                number = f"{seat_row}{letter}"
                ensure(api, counts, "seats", seats, lambda x: x["seatNumber"].casefold(),
                       number.casefold(), f"/aircraft/{row['id']}/seats",
                       {"seatNumber": number, "seatClass": "BUSINESS" if seat_row == 1 else "ECONOMY"})
    for index, (carrier, origin, destination, hour, duration) in enumerate(ROUTES):
        name, code = AIRLINES[carrier]
        plane = aircraft[carrier + (index // 5 % 2) * 5]
        number = f"{code}{100 + index}"
        existing = unique(flight_rows, lambda x: x["flightNumber"].casefold(), number.casefold())
        if existing:
            if (existing["airlineName"] != name or existing["originName"] != airports[origin]["name"]
                or existing["destinationName"] != airports[destination]["name"]
                or existing["aircraftRegistration"] != plane["registrationNumber"]):
                raise RuntimeError(f"Flight {number} conflicts with the demo definition")
            counts["flights"]["skipped"] += 1
            flight = existing
        else:
            flight = api.call("POST", "/flights", {"flightNumber": number,
                "airlineId": airlines[name]["id"], "aircraftId": plane["id"],
                "originAirportId": airports[origin]["id"], "destinationAirportId": airports[destination]["id"]})
            flight_rows.append(flight)
            counts["flights"]["created"] += 1
        # Two departures per route, spread through the next fourteen days.
        for offset in (index % 7 + 1, index % 7 + 8):
            departure = dt.datetime.combine(start + dt.timedelta(days=offset), dt.time(hour, 0))
            arrival = departure + dt.timedelta(minutes=round(duration * 60))
            departure_text = departure.isoformat(timespec="seconds")
            arrival_text = arrival.isoformat(timespec="seconds")
            key = (flight["id"], departure_text)
            found = unique(instance_rows, lambda x: (x["flightId"], x["departureDateTime"]), key)
            if found:
                if found["arrivalDateTime"] != arrival_text:
                    raise RuntimeError(f"Schedule for {number} at {departure_text} conflicts")
                counts["instances"]["skipped"] += 1
            else:
                instance_rows.append(api.call("POST", "/flight-instances", {
                    "flightId": flight["id"], "departureDateTime": departure_text,
                    "arrivalDateTime": arrival_text, "status": "SCHEDULED"}))
                counts["instances"]["created"] += 1
    return counts, airports


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--base-url", default="http://localhost:8080", help="Trusted local SkyNode origin")
    parser.add_argument("--email", default="skynode@admin.com")
    parser.add_argument("--date", type=dt.date.fromisoformat, default=dt.date.today(),
                        help="Anchor date for repeatable schedules (YYYY-MM-DD); defaults to today")
    args = parser.parse_args()
    parsed = urllib.parse.urlparse(args.base_url)
    if parsed.scheme != "http" or parsed.hostname not in ("localhost", "127.0.0.1"):
        parser.error("Only a local HTTP SkyNode origin is allowed")
    api = Api(args.base_url)
    try:
        api.login(args.email, getpass.getpass("SkyNode admin password: "))
        counts, airports = seed(api, args.date)
        for kind in ("airports", "airlines", "aircraft", "seats", "flights", "instances"):
            print(f"{kind}: created {counts[kind]['created']}, skipped {counts[kind]['skipped']}")
        print(f"Anchor date: {args.date.isoformat()}. Rerun with --date {args.date.isoformat()} for exact idempotency.")
        kochi = airports["Kochi"]["id"]
        results = api.call("GET", f"/flights/search?originAirportId={kochi}&departureDate={(args.date + dt.timedelta(days=1)).isoformat()}")
        print(f"Search check: {len(results)} Kochi departures on first seeded date")
    except (RuntimeError, urllib.error.URLError, TimeoutError) as error:
        print(f"Seed stopped: {error}", file=sys.stderr)
        return 1
    return 0

if __name__ == "__main__":
    raise SystemExit(main())
