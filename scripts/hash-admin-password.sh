#!/usr/bin/env bash
set -eu
cd "$(dirname "$0")/.."
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/admin-runtime-classpath >&2
trap 'unset admin_password admin_confirmation' EXIT
read -r -s -p 'Admin password (hidden): ' admin_password </dev/tty
printf '\n' >/dev/tty
read -r -s -p 'Confirm password (hidden): ' admin_confirmation </dev/tty
printf '\n' >/dev/tty
if [[ "$admin_password" != "$admin_confirmation" ]]; then
    printf 'Passwords do not match.\n' >&2
    exit 1
fi
printf '%s' "$admin_password" | java --class-path "$(cat target/admin-runtime-classpath)" scripts/SetupAdmin.java --hash-stdin
