#!/usr/bin/env bash
set -e

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
cd "$DIR"

if [ ! -f .env ]; then
  echo "Creating .env from .env.example..."
  cp .env.example .env
fi

# Load .env if present
export $(grep -v '^#' .env | xargs -d '\n')

echo "Starting EchoMinutes Backend on port ${PORT:-8000}..."

if command -v uv >/dev/null 2>&1; then
  uv run --with-requirements requirements.txt uvicorn main:app --host 0.0.0.0 --port "${PORT:-8000}" --reload
else
  if [ ! -d ".venv" ]; then
    python3 -m venv .venv
    .venv/bin/pip install -r requirements.txt
  fi
  .venv/bin/uvicorn main:app --host 0.0.0.0 --port "${PORT:-8000}" --reload
fi
