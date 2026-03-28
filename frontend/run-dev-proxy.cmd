@echo off
setlocal
set VITE_PROXY_TARGET=http://127.0.0.1:18080
npm run dev -- --host 127.0.0.1 --port 4173
