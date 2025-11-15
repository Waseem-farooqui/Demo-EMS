#!/bin/bash
# Wait for MySQL to be ready before starting the application

set -e

host="$1"
port="$2"
shift 2

echo "Waiting for MySQL at $host:$port..."

until nc -z "$host" "$port" 2>/dev/null; do
  >&2 echo "MySQL is unavailable - sleeping"
  sleep 2
done

>&2 echo "MySQL is up - executing command"
exec "$@"

