# SkyNode

Spring Boot Flight Booking System

Backend: Java, Spring Boot
Database: PostgreSQL
Version Control: GitHub


<!-- docker start skynode-postgres
docker ps
docker exec skynode-postgres pg_isready -U postgres
set -a
source .env
set +a -->


read -rsp "Enter PostgreSQL password for skynode_user: " SKYNODE_DB_PASSWORD
echo
export SKYNODE_DB_PASSWORD


<!-- check envs are set -->

if [ -n "$SKYNODE_DB_PASSWORD" ]; then
    echo "SKYNODE_DB_PASSWORD is set"
else
    echo "SKYNODE_DB_PASSWORD is NOT set"
fi