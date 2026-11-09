@echo off
REM Levanta backend (microservicios + gateway) y frontend.
cd /d "%~dp0"
start "Backend" cmd /c "cd backend && iniciar-todo.bat"
cd frontend
if not exist .env copy .env.example .env
if not exist node_modules call npm install
npm run dev
