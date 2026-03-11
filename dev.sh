#!/usr/bin/env bash
# Use Java 21 from Homebrew if available
if [[ -d "/opt/homebrew/opt/openjdk@21/bin" ]]; then
  export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
  export JAVA_HOME="/opt/homebrew/opt/openjdk@21"
fi
# Add Homebrew docker to PATH
export PATH="/opt/homebrew/bin:$PATH"
# ─────────────────────────────────────────────────────────────────────────────
#  ResumeAI Studio — Local Development Launcher
#  Usage:
#    ./dev.sh              Start everything
#    ./dev.sh infra        Start only infrastructure (DB, Redis, LocalStack)
#    ./dev.sh backend      Start only backend services
#    ./dev.sh frontend     Start only frontend
#    ./dev.sh stop         Stop all running services
#    ./dev.sh logs         Tail logs from all background services
#    ./dev.sh status       Show status of all services
# ─────────────────────────────────────────────────────────────────────────────
set -euo pipefail

# ─── Colours ─────────────────────────────────────────────────────────────────
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
CYAN='\033[0;36m'; BOLD='\033[1m'; DIM='\033[2m'; RESET='\033[0m'

# ─── Paths ────────────────────────────────────────────────────────────────────
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
LOG_DIR="$SCRIPT_DIR/.logs"
PID_FILE="$SCRIPT_DIR/.dev-pids"
ENV_FILE="$SCRIPT_DIR/.env"

# ─── Ports ────────────────────────────────────────────────────────────────────
PORT_FRONTEND=3000
PORT_RESUME=8081
PORT_AI=8082
PORT_LATEX=8083
PORT_AUTH=8084
PORT_POSTGRES=5432
PORT_REDIS=6379

# ─── Helpers ─────────────────────────────────────────────────────────────────
log()     { echo -e "${BOLD}${CYAN}▶ $*${RESET}"; }
success() { echo -e "${GREEN}✔ $*${RESET}"; }
warn()    { echo -e "${YELLOW}⚠ $*${RESET}"; }
error()   { echo -e "${RED}✘ $*${RESET}" >&2; }
dim()     { echo -e "${DIM}$*${RESET}"; }
header()  { echo -e "\n${BOLD}${CYAN}━━━ $* ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${RESET}"; }

# ─── Banner ───────────────────────────────────────────────────────────────────
banner() {
  echo -e "${BOLD}${CYAN}"
  echo "  ██████╗ ███████╗███████╗██╗   ██╗███╗   ███╗███████╗ █████╗ ██╗"
  echo "  ██╔══██╗██╔════╝██╔════╝██║   ██║████╗ ████║██╔════╝██╔══██╗██║"
  echo "  ██████╔╝█████╗  ███████╗██║   ██║██╔████╔██║█████╗  ███████║██║"
  echo "  ██╔══██╗██╔══╝  ╚════██║██║   ██║██║╚██╔╝██║██╔══╝  ██╔══██║██║"
  echo "  ██║  ██║███████╗███████║╚██████╔╝██║ ╚═╝ ██║███████╗██║  ██║██║"
  echo "  ╚═╝  ╚═╝╚══════╝╚══════╝ ╚═════╝ ╚═╝     ╚═╝╚══════╝╚═╝  ╚═╝╚═╝"
  echo -e "${RESET}${BOLD}                   AI Studio — Local Dev Launcher${RESET}"
  echo ""
}

# ─── Prereq check ────────────────────────────────────────────────────────────
check_prereqs() {
  header "Checking Prerequisites"
  local missing=0

  check_cmd() {
    if command -v "$1" &>/dev/null; then
      success "$1 found ($(command -v "$1"))"
    else
      error "$1 not found — please install it"
      missing=$((missing + 1))
    fi
  }

  check_cmd docker
  check_cmd java
  check_cmd node
  check_cmd npm

  # Java version check (need 21+)
  if command -v java &>/dev/null; then
    JAVA_VER=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | cut -d'.' -f1)
    if [[ "$JAVA_VER" -lt 21 ]]; then
      error "Java 21+ required (found $JAVA_VER)"
      missing=$((missing + 1))
    else
      success "Java $JAVA_VER ✓"
    fi
  fi

  # Docker daemon running?
  if command -v docker &>/dev/null; then
    if ! docker info &>/dev/null; then
      error "Docker daemon is not running — please start Docker Desktop"
      missing=$((missing + 1))
    else
      success "Docker daemon running"
    fi
  fi

  # .env file
  if [[ -f "$ENV_FILE" ]]; then
    success ".env file found"
    # shellcheck source=/dev/null
    source "$ENV_FILE"
    if [[ -z "${ANTHROPIC_API_KEY:-}" || "$ANTHROPIC_API_KEY" == sk-ant-xxx* ]]; then
      warn "ANTHROPIC_API_KEY looks like a placeholder — AI features won't work"
    fi
  else
    warn ".env file not found — copying from .env.example"
    if [[ -f "$SCRIPT_DIR/.env.example" ]]; then
      cp "$SCRIPT_DIR/.env.example" "$ENV_FILE"
      warn "Edit .env and add your ANTHROPIC_API_KEY, then re-run."
      exit 1
    else
      error ".env.example also missing!"
      missing=$((missing + 1))
    fi
  fi

  if [[ $missing -gt 0 ]]; then
    error "$missing prerequisite(s) missing. Aborting."
    exit 1
  fi

  echo ""
}

# ─── Port availability ────────────────────────────────────────────────────────
port_in_use() { lsof -ti:"$1" &>/dev/null; }

check_ports() {
  header "Checking Ports"
  local conflict=0
  for port in $PORT_FRONTEND $PORT_RESUME $PORT_AI $PORT_LATEX $PORT_AUTH; do
    if port_in_use "$port"; then
      warn "Port $port already in use (run './dev.sh stop' first or kill manually)"
      conflict=$((conflict + 1))
    else
      success "Port $port free"
    fi
  done
  if [[ $conflict -gt 0 ]]; then
    echo ""
    warn "$conflict port conflict(s) detected."
    read -r -p "Continue anyway? [y/N] " yn
    [[ "$yn" =~ ^[Yy]$ ]] || exit 1
  fi
  echo ""
}

# ─── Infra (Docker Compose) ───────────────────────────────────────────────────
start_infra() {
  header "Starting Infrastructure"
  log "Pulling / starting Postgres, Redis, LocalStack…"

  cd "$SCRIPT_DIR"
  docker compose up -d postgres redis localstack 2>&1 | \
    while IFS= read -r line; do dim "  docker | $line"; done

  log "Waiting for Postgres to be healthy…"
  local attempts=0
  until docker compose exec -T postgres pg_isready -U resumeai &>/dev/null; do
    sleep 2
    attempts=$((attempts + 1))
    if [[ $attempts -ge 30 ]]; then
      error "Postgres did not become healthy in 60s"
      exit 1
    fi
    echo -n "."
  done
  echo ""
  success "Postgres ready"

  log "Waiting for Redis…"
  attempts=0
  until docker compose exec -T redis redis-cli ping &>/dev/null; do
    sleep 1
    attempts=$((attempts + 1))
    [[ $attempts -ge 20 ]] && { error "Redis timeout"; exit 1; }
  done
  success "Redis ready"

  log "Waiting for LocalStack…"
  attempts=0
  until curl -sf http://localhost:4566/_localstack/health &>/dev/null; do
    sleep 2
    attempts=$((attempts + 1))
    [[ $attempts -ge 30 ]] && { warn "LocalStack not ready — S3/SQS may not work"; break; }
  done
  success "LocalStack ready (or skipped)"
  echo ""
}

# ─── Gradle wrapper bootstrap ─────────────────────────────────────────────────
ensure_gradle_wrapper() {
  if [[ ! -f "$BACKEND_DIR/gradlew" ]]; then
    log "Gradle wrapper not found — generating…"
    cd "$BACKEND_DIR"
    gradle wrapper --gradle-version 8.7 2>/dev/null || {
      warn "Could not auto-generate gradlew. Install Gradle 8+ or add gradlew manually."
    }
  fi
}

# ─── Build all backend modules ────────────────────────────────────────────────
build_backend() {
  header "Building Backend"
  ensure_gradle_wrapper
  cd "$BACKEND_DIR"
  log "Running Gradle build (skipping tests for speed)…"
  ./gradlew build -x test --parallel 2>&1 | \
    while IFS= read -r line; do dim "  gradle | $line"; done
  success "Backend build complete"
  echo ""
}

# ─── Start a single Spring Boot service in background ────────────────────────
start_service() {
  local name="$1"   # human label
  local module="$2" # Gradle module name
  local port="$3"
  local log_file="$LOG_DIR/${module}.log"

  mkdir -p "$LOG_DIR"

  if port_in_use "$port"; then
    warn "$name already running on port $port — skipping"
    return 0
  fi

  log "Starting $name on port ${port}..."

  # Load env vars for the service
  # shellcheck source=/dev/null
  source "$ENV_FILE"

  SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5432/resumeai" \
  SPRING_DATASOURCE_USERNAME="resumeai" \
  SPRING_DATASOURCE_PASSWORD="${POSTGRES_PASSWORD:-resumeai_secret}" \
  SPRING_REDIS_HOST="localhost" \
  AWS_ENDPOINT_OVERRIDE="http://localhost:4566" \
  AWS_REGION="${AWS_REGION:-us-east-1}" \
  AWS_ACCESS_KEY_ID="test" \
  AWS_SECRET_ACCESS_KEY="test" \
  ANTHROPIC_API_KEY="${ANTHROPIC_API_KEY:-}" \
  JWT_SECRET="${JWT_SECRET:-dev_secret_key_32_chars_minimum__}" \
  nohup "$BACKEND_DIR/gradlew" -p "$BACKEND_DIR" \
    "${module}:bootRun" \
    --no-daemon \
    > "$log_file" 2>&1 &

  local pid=$!
  echo "$name:$pid:$port" >> "$PID_FILE"

  # Wait up to 45s for the service to start
  local attempts=0
  local started=false
  while [[ $attempts -lt 45 ]]; do
    sleep 1
    attempts=$((attempts + 1))
    echo -n "."
    if port_in_use "$port"; then
      started=true
      break
    fi
    # Check if process died
    if ! kill -0 "$pid" 2>/dev/null; then
      echo ""
      error "$name process died. Check $log_file"
      return 1
    fi
  done
  echo ""

  if $started; then
    success "$name started  →  http://localhost:$port  (pid $pid)"
    dim "    logs: $log_file"
  else
    warn "$name may still be starting — check $log_file"
  fi
}

# ─── Backend services ─────────────────────────────────────────────────────────
start_backend() {
  header "Starting Backend Services"
  rm -f "$PID_FILE"
  touch "$PID_FILE"

  start_service "Auth Service"         "auth-service"         "$PORT_AUTH"
  start_service "AI Orchestrator"      "ai-orchestrator-service" "$PORT_AI"
  start_service "LaTeX Compiler"       "latex-compiler-service"  "$PORT_LATEX"
  start_service "Resume Service"       "resume-service"       "$PORT_RESUME"

  echo ""
  success "All backend services started"
  echo ""
}

# ─── Frontend ─────────────────────────────────────────────────────────────────
start_frontend() {
  header "Starting Frontend"

  cd "$FRONTEND_DIR"

  # Install deps if node_modules missing or package.json newer
  if [[ ! -d node_modules ]] || [[ package.json -nt node_modules ]]; then
    log "Installing npm dependencies…"
    npm install 2>&1 | while IFS= read -r line; do dim "  npm | $line"; done
    success "npm install done"
  fi

  local log_file="$LOG_DIR/frontend.log"
  mkdir -p "$LOG_DIR"

  log "Starting Vite dev server on port ${PORT_FRONTEND}..."
  nohup npm run dev > "$log_file" 2>&1 &
  local pid=$!
  echo "Frontend:$pid:$PORT_FRONTEND" >> "$PID_FILE"

  local attempts=0
  until port_in_use "$PORT_FRONTEND"; do
    sleep 1
    attempts=$((attempts + 1))
    echo -n "."
    [[ $attempts -ge 30 ]] && { echo ""; warn "Frontend may still be starting…"; break; }
  done
  echo ""
  success "Frontend running  →  http://localhost:$PORT_FRONTEND  (pid $pid)"
  dim "    logs: $log_file"
  echo ""
}

# ─── Print access URLs ────────────────────────────────────────────────────────
print_urls() {
  header "Services Running"
  echo -e "  ${BOLD}${GREEN}Frontend${RESET}            →  ${CYAN}http://localhost:$PORT_FRONTEND${RESET}"
  echo -e "  ${BOLD}Resume Service${RESET}    →  ${CYAN}http://localhost:$PORT_RESUME/swagger-ui.html${RESET}"
  echo -e "  ${BOLD}AI Orchestrator${RESET}   →  ${CYAN}http://localhost:$PORT_AI/swagger-ui.html${RESET}"
  echo -e "  ${BOLD}LaTeX Compiler${RESET}    →  ${CYAN}http://localhost:$PORT_LATEX/swagger-ui.html${RESET}"
  echo -e "  ${BOLD}Auth Service${RESET}      →  ${CYAN}http://localhost:$PORT_AUTH/swagger-ui.html${RESET}"
  echo -e "  ${BOLD}LocalStack${RESET}        →  ${CYAN}http://localhost:4566${RESET}"
  echo ""
  echo -e "  ${DIM}Logs dir:  $LOG_DIR/${RESET}"
  echo -e "  ${DIM}PID file:  $PID_FILE${RESET}"
  echo ""
  echo -e "  Press ${BOLD}Ctrl+C${RESET} to stop all services."
  echo ""
}

# ─── Stop all services ────────────────────────────────────────────────────────
stop_all() {
  header "Stopping Services"

  # Kill background Java/Node processes from PID file
  if [[ -f "$PID_FILE" ]]; then
    while IFS=: read -r name pid port; do
      if [[ -n "$pid" ]] && kill -0 "$pid" 2>/dev/null; then
        log "Stopping $name (pid $pid)…"
        kill "$pid" 2>/dev/null && success "$name stopped" || warn "Could not stop $pid"
      fi
    done < "$PID_FILE"
    rm -f "$PID_FILE"
  else
    warn "No PID file found — killing by port scan"
    for port in $PORT_FRONTEND $PORT_RESUME $PORT_AI $PORT_LATEX $PORT_AUTH; do
      if port_in_use "$port"; then
        local pid
        pid=$(lsof -ti:"$port")
        log "Killing port $port (pid $pid)…"
        kill "$pid" 2>/dev/null && success "Port $port freed"
      fi
    done
  fi

  # Stop Docker services
  log "Stopping Docker infrastructure…"
  cd "$SCRIPT_DIR"
  docker compose stop postgres redis localstack 2>/dev/null && success "Docker services stopped"
  echo ""
  success "All services stopped."
}

# ─── Tail logs ────────────────────────────────────────────────────────────────
tail_logs() {
  if [[ ! -d "$LOG_DIR" ]]; then
    error "No log directory found. Start the app first."
    exit 1
  fi
  log "Tailing all logs (Ctrl+C to stop)…"
  echo ""
  tail -f "$LOG_DIR"/*.log 2>/dev/null || warn "No log files found in $LOG_DIR"
}

# ─── Status ───────────────────────────────────────────────────────────────────
show_status() {
  header "Service Status"
  check_port_status() {
    local label="$1" port="$2"
    if port_in_use "$port"; then
      echo -e "  ${GREEN}●${RESET} ${BOLD}$label${RESET}  (port $port)"
    else
      echo -e "  ${RED}○${RESET} ${DIM}$label  (port $port — not running)${RESET}"
    fi
  }

  check_port_status "Frontend"         "$PORT_FRONTEND"
  check_port_status "Resume Service"   "$PORT_RESUME"
  check_port_status "AI Orchestrator"  "$PORT_AI"
  check_port_status "LaTeX Compiler"   "$PORT_LATEX"
  check_port_status "Auth Service"     "$PORT_AUTH"
  check_port_status "Postgres"         "$PORT_POSTGRES"
  check_port_status "Redis"            "$PORT_REDIS"
  echo ""
}

# ─── Trap Ctrl+C ─────────────────────────────────────────────────────────────
trap_cleanup() {
  echo ""
  warn "Interrupt received — shutting down…"
  stop_all
  exit 0
}

# ─── Main ─────────────────────────────────────────────────────────────────────
CMD="${1:-all}"

case "$CMD" in
  stop)
    banner
    stop_all
    ;;
  logs)
    tail_logs
    ;;
  status)
    banner
    show_status
    ;;
  infra)
    banner
    check_prereqs
    start_infra
    success "Infrastructure ready. Run './dev.sh backend' and './dev.sh frontend' next."
    ;;
  backend)
    banner
    check_prereqs
    build_backend
    start_backend
    print_urls
    # Keep alive and stream logs
    trap trap_cleanup INT TERM
    tail -f "$LOG_DIR"/resume-service.log "$LOG_DIR"/ai-orchestrator-service.log 2>/dev/null &
    wait
    ;;
  frontend)
    banner
    check_prereqs
    start_frontend
    print_urls
    trap trap_cleanup INT TERM
    tail -f "$LOG_DIR/frontend.log" 2>/dev/null &
    wait
    ;;
  all|*)
    banner
    check_prereqs
    check_ports
    start_infra
    build_backend
    start_backend
    start_frontend
    print_urls
    # Stream logs until Ctrl+C
    trap trap_cleanup INT TERM
    log "Streaming logs (Ctrl+C to stop all services)…"
    echo ""
    tail -f \
      "$LOG_DIR/frontend.log" \
      "$LOG_DIR/resume-service.log" \
      "$LOG_DIR/ai-orchestrator-service.log" \
      2>/dev/null &
    TAIL_PID=$!
    wait $TAIL_PID
    ;;
esac
