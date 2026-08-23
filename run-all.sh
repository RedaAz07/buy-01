#!/usr/bin/env bash
set -u

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
RUN_DIR="$ROOT/.run"
LOG_DIR="$ROOT/logs"
mkdir -p "$RUN_DIR" "$LOG_DIR"

NAMES=(registry api-gateway product-service user-service)
declare -A PORT=(
  [registry]=8761
  [api-gateway]=8080
  [product-service]=8082
  [user-service]=8081
)

port_open() { timeout 1 bash -c "</dev/tcp/127.0.0.1/$1" 2>/dev/null; }

wait_port() {
  local name=$1 port=$2 tries=${3:-60} i
  for ((i = 1; i <= tries; i++)); do
    if port_open "$port"; then
      echo "[$name] up on :$port"
      return 0
    fi
    sleep 1
  done
  echo "[$name] NOT up on :$port after ${tries}s (check $LOG_DIR/$name.log)"
  return 1
}

ensure_mongo() {
  if docker ps --format '{{.Names}}' | grep -qx 'product-service'; then
    echo "[mongo] container already running"
    return 0
  fi
  echo "[mongo] starting container..."
  docker compose --env-file "$ROOT/product-service/.env" \
    -f "$ROOT/product-service/docker-compose.yml" up -d
}

start_one() {
  local name=$1 port=${PORT[$1]}
  if port_open "$port"; then
    echo "[$name] already up on :$port"
    return 0
  fi
  echo "[$name] starting..."
  (
    cd "$ROOT/$name" || exit 1
    if [ -f .env ]; then
      set -a
      . ./.env
      set +a
    fi
    exec setsid ./mvnw spring-boot:run >"$LOG_DIR/$name.log" 2>&1
  ) &
  echo $! >"$RUN_DIR/$name.pid"
}

stop_one() {
  local name=$1 port=${PORT[$1]} pf="$RUN_DIR/$1.pid" pid=""
  [ -f "$pf" ] && pid=$(cat "$pf")
  if [ -n "$pid" ] && kill -0 "$pid" 2>/dev/null; then
    kill -- "-$pid" 2>/dev/null || kill "$pid" 2>/dev/null
    rm -f "$pf"
    echo "[$name] stopped"
    return 0
  fi
  if port_open "$port"; then
    fuser -k -TERM "${port}/tcp" >/dev/null 2>&1
    sleep 2
    port_open "$port" && fuser -k "${port}/tcp" >/dev/null 2>&1
    echo "[$name] killed process on :$port"
  else
    echo "[$name] not running"
  fi
}

status_all() {
  local name pid pf
  printf '%-18s %-6s %s\n' SERVICE PORT STATUS
  for name in "${NAMES[@]}"; do
    pf="$RUN_DIR/$name.pid"
    pid=""
    [ -f "$pf" ] && pid=$(cat "$pf")
    if port_open "${PORT[$name]}"; then st=UP; else st=DOWN; fi
    printf '%-18s %-6s %s\n' "$name" ":${PORT[$name]}" "$st${pid:+ (pid $pid)}"
  done
  docker ps --format '{{.Names}}' | grep -qx 'product-service' &&
    printf '%-18s %-6s %s\n' mongo-docker :27017 UP ||
    printf '%-18s %-6s %s\n' mongo-docker :27017 DOWN
}

case "${1:-help}" in
  start)
    ensure_mongo
    start_one registry
    wait_port registry 8761 60
    for n in api-gateway product-service user-service; do start_one "$n"; done
    for n in api-gateway product-service user-service; do wait_port "$n" "${PORT[$n]}" 120; done
    echo "logs: $LOG_DIR/<service>.log"
    ;;
  stop)
    for n in user-service product-service api-gateway registry; do stop_one "$n"; done
    ;;
  restart)
    "$0" stop
    sleep 2
    "$0" start
    ;;
  status)
    status_all
    ;;
  logs)
    [ -f "$LOG_DIR/$2.log" ] && tail -n 100 -F "$LOG_DIR/$2.log" ||
      { echo "no log for '${2:-}' (available: ${NAMES[*]})"; exit 1; }
    ;;
  *)
    echo "usage: ./run-all.sh {start|stop|restart|status|logs <service>}"
    echo "services: ${NAMES[*]}"
    ;;
esac
