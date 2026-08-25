#!/usr/bin/env bash
# ==============================================================================
# EchoMinutes Continuous Background Service Runner
# ==============================================================================
DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

export PATH="$HOME/.local/bin:$PATH"

# Ensure .env exists
if [ ! -f .env ]; then
  echo "[EchoMinutes] Creating .env from .env.example..."
  cp .env.example .env
fi

# Load variables safely from .env
set -a
[ -f .env ] && . .env
set +a

PORT="${PORT:-8765}"
LOG_FILE="$DIR/server.log"
PID_FILE="$DIR/server.pid"
TUNNEL_PID_FILE="$DIR/tunnel.pid"

# Stop existing processes if running
if [ -f "$PID_FILE" ]; then
  OLD_PID=$(cat "$PID_FILE" 2>/dev/null || true)
  if [ -n "$OLD_PID" ] && kill -0 "$OLD_PID" 2>/dev/null; then
    echo "[EchoMinutes] Stopping previous server instance (PID $OLD_PID)..."
    kill "$OLD_PID" 2>/dev/null || true
    sleep 1
  fi
  rm -f "$PID_FILE"
fi

if [ -f "$TUNNEL_PID_FILE" ]; then
  OLD_TPID=$(cat "$TUNNEL_PID_FILE" 2>/dev/null || true)
  if [ -n "$OLD_TPID" ] && kill -0 "$OLD_TPID" 2>/dev/null; then
    echo "[EchoMinutes] Stopping previous tunnel instance (PID $OLD_TPID)..."
    kill "$OLD_TPID" 2>/dev/null || true
    sleep 1
  fi
  rm -f "$TUNNEL_PID_FILE"
fi

# Optional: Start Cloudflare Tunnel if TUNNEL_TOKEN is set in .env
if [ -n "$TUNNEL_TOKEN" ]; then
  echo "[EchoMinutes] Starting Cloudflare Tunnel in background..."
  nohup cloudflared tunnel run --token "$TUNNEL_TOKEN" >> "$DIR/tunnel.log" 2>&1 &
  echo $! > "$TUNNEL_PID_FILE"
  echo "[EchoMinutes] Cloudflare Tunnel started (PID $(cat "$TUNNEL_PID_FILE"))."
fi

echo "[EchoMinutes] Starting continuous server monitor on port $PORT..."
echo "[EchoMinutes] Logs are streamed to $LOG_FILE"

nohup bash -c "
  export PATH=\"\$HOME/.local/bin:\$PATH\"
  cd \"$DIR\"
  while true; do
    echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] Starting FastAPI server on port $PORT...\" >> \"$LOG_FILE\"
    if command -v uv >/dev/null 2>&1; then
      uv run --with-requirements requirements.txt uvicorn main:app --host 0.0.0.0 --port $PORT >> \"$LOG_FILE\" 2>&1
    else
      if [ ! -d .venv ]; then
        python3 -m venv .venv >> \"$LOG_FILE\" 2>&1
        .venv/bin/pip install -r requirements.txt >> \"$LOG_FILE\" 2>&1
      fi
      .venv/bin/uvicorn main:app --host 0.0.0.0 --port $PORT >> \"$LOG_FILE\" 2>&1
    fi
    EXIT_CODE=\$?
    echo \"[\$(date '+%Y-%m-%d %H:%M:%S')] Server exited with code \$EXIT_CODE. Restarting in 3 seconds...\" >> \"$LOG_FILE\"
    sleep 3
  done
" >> "$LOG_FILE" 2>&1 &

SERVER_PID=$!
echo $SERVER_PID > "$PID_FILE"

echo "=================================================================="
echo "  🚀 EchoMinutes Backend is now running continuously in background!"
echo "  - Process PID: $SERVER_PID"
echo "  - Local Dashboard: http://localhost:$PORT"
echo "  - Server Log: $LOG_FILE"
echo "  - To stop: ./stop.sh"
echo "=================================================================="
