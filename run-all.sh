#!/usr/bin/env bash

set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/docs/.run"
LOG_DIR="$ROOT/docs/logs"

mkdir -p "$RUN_DIR" "$LOG_DIR"

SERVICES=(
  registry
  api-gateway
  product-service
  user-service
  media-service
)

declare -A PORT=(
  [registry]=8761
  [api-gateway]=8443
  [product-service]=8082
  [user-service]=8081
  [media-service]=8083
)

declare -A DIR=(
  [registry]="Backend/registry"
  [api-gateway]="Backend/api-gateway"
  [product-service]="Backend/product-service"
  [user-service]="Backend/user-service"
  [media-service]="Backend/media-Service"
)

port_open() {
  timeout 1 bash -c "</dev/tcp/127.0.0.1/$1" 2>/dev/null
}

start_service() {
  local name="$1"
  local dir="${DIR[$name]}"
  local log="$LOG_DIR/$name.log"
  local pid_file="$RUN_DIR/$name.pid"

  if port_open "${PORT[$name]}"; then
    echo "[$name] already running on :${PORT[$name]}"
    return 0
  fi

  echo "[$name] starting..."

  (
    cd "$ROOT/$dir" || exit 1

    # Load .env if it exists
    if [ -f .env ]; then
      set -a
      source .env
      set +a
    fi

    echo ""
    echo "=================================================="
    echo "[$name] started at $(date)"
    echo "=================================================="
    echo ""

    exec ./mvnw spring-boot:run
  ) >"$log" 2>&1 &

  echo $! >"$pid_file"

  echo "[$name] started - log: $log"
}

stop_service() {
  local name="$1"
  local pid_file="$RUN_DIR/$name.pid"

  if [ ! -f "$pid_file" ]; then
    echo "[$name] not running"
    return 0
  fi

  local pid
  pid=$(cat "$pid_file")

  if kill -0 "$pid" 2>/dev/null; then
    echo "[$name] stopping..."
    kill "$pid" 2>/dev/null

    # Give Spring Boot a few seconds to shut down
    for _ in {1..10}; do
      if ! kill -0 "$pid" 2>/dev/null; then
        break
      fi
      sleep 1
    done

    # Force kill if still running
    if kill -0 "$pid" 2>/dev/null; then
      echo "[$name] forcing shutdown..."
      kill -9 "$pid" 2>/dev/null
    fi

    echo "[$name] stopped"
  else
    echo "[$name] already stopped"
  fi

  rm -f "$pid_file"
}

wait_for_service() {
  local name="$1"
  local port="${PORT[$name]}"
  local timeout="${2:-60}"

  echo "[$name] waiting for :$port..."

  for ((i=1; i<=timeout; i++)); do
    if port_open "$port"; then
      echo "[$name] UP on :$port"
      return 0
    fi

    sleep 1
  done

  echo "[$name] FAILED to start on :$port"
  echo "[$name] check: $LOG_DIR/$name.log"

  return 1
}

status() {
  echo ""
  echo "Backend services"
  echo "----------------"

  for name in "${SERVICES[@]}"; do
    local pid_file="$RUN_DIR/$name.pid"
    local state="DOWN"

    if port_open "${PORT[$name]}"; then
      state="UP"
    fi

    printf "%-20s :%-5s %s\n" \
      "$name" \
      "${PORT[$name]}" \
      "$state"
  done

  echo ""
}

start_all() {
  echo ""
  echo "Starting backend services..."
  echo ""

  # Registry first
  start_service registry

  if ! wait_for_service registry 60; then
    echo ""
    echo "Registry failed to start."
    echo "Check: $LOG_DIR/registry.log"
    exit 1
  fi

  # Start remaining services
  for name in api-gateway product-service user-service media-service; do
    start_service "$name"
  done

  echo ""
  echo "Backend services started."
  echo ""
  echo "Logs:"
  echo "  $LOG_DIR/<service>.log"
  echo ""
}

stop_all() {
  echo ""
  echo "Stopping backend services..."
  echo ""

  # Stop in reverse order
  for name in media-service user-service product-service api-gateway registry; do
    stop_service "$name"
  done

  echo ""
  echo "All backend services stopped."
}

restart_all() {
  stop_all
  sleep 2
  start_all
}

show_logs() {
  local name="${1:-}"

  if [ -z "$name" ]; then
    echo "Usage: $0 logs <service>"
    echo ""
    echo "Available services:"
    printf '  %s\n' "${SERVICES[@]}"
    exit 1
  fi

  if [ ! -f "$LOG_DIR/$name.log" ]; then
    echo "No log found for: $name"
    exit 1
  fi

  tail -n 100 -F "$LOG_DIR/$name.log"
}

case "${1:-help}" in

  start)
    start_all
    ;;

  stop)
    stop_all
    ;;

  restart)
    restart_all
    ;;

  status)
    status
    ;;

  logs)
    show_logs "$2"
    ;;

  *)
    echo ""
    echo "Usage:"
    echo "  $0 start"
    echo "  $0 stop"
    echo "  $0 restart"
    echo "  $0 status"
    echo "  $0 logs <service>"
    echo ""
    echo "Services:"
    printf '  %s\n' "${SERVICES[@]}"
    echo ""
    ;;

esac
