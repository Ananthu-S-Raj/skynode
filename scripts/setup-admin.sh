#!/bin/sh
set -eu
cd "$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)"
./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/admin-runtime-classpath
exec java --class-path "$(cat target/admin-runtime-classpath)" scripts/SetupAdmin.java
