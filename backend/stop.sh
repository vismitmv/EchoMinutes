#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

PID_FILE="$DIR/server.pid"
TUNNEL_PID_FILE="$DIR/tunnel.pid"

STOPPED=false

if [ -f "$PID_FILE" ]; then
  PID=$(cat "$PID_FILE" 2>/dev/null || true)
  if [ -n "$PID" ]; then
    echo "[EchoMinutes] Stopping server (PID $PID)..."
    kill "$PID" 2>/dev/null || true
    # Also kill any uvicorn children
    pkill -P "$PID" 2>/dev/null || true
    rm -f "$PID_FILE"
    STOPPED=true
  fi
fi

if [ -f "$TUNNEL_PID_FILE" ]; then
  TPID=$(cat "$TUNNEL_PID_FILE" 2>/dev/null || true)
  if [ -n "$TPID" ]; then
    echo "[EchoMinutes] Stopping Cloudflare Tunnel (PID $TPID)..."
    kill "$TPID" 2>/dev/null || true
    rm -f "$TUNNEL_PID_FILE"
    STOPPED=true
  fi
fi

# Fallback cleanup for uvicorn processes started in this dir
pkill -f "uvicorn main:app" 2>/dev/null || true

echo "[EchoMinutes] Service stopped successfully."
