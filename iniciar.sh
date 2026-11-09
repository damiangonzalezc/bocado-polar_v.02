#!/usr/bin/env bash
# Levanta backend (microservicios + gateway) y frontend.
cd "$(dirname "$0")"
(cd backend && ./iniciar-todo.sh) &
cd frontend
[ -f .env ] || cp .env.example .env
[ -d node_modules ] || npm install
npm run dev
