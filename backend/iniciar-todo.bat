@echo off
REM ============================================================
REM  BOCADO Polar - levanta los 13 microservicios y el gateway.
REM  Requisitos: XAMPP con MySQL encendido (puerto 3306, root sin clave)
REM  y Java 17 o superior. Cada servicio abre su propia ventana.
REM  Primero ms-config (los demás leen sus reglas de negocio).
REM ============================================================
cd /d %~dp0
call mvnw.cmd -q -DskipTests install
if errorlevel 1 (
  echo Error al compilar. Revisa la salida de Maven.
  pause
  exit /b 1
)
for %%S in (ms-config ms-auth ms-clientes ms-catalogo ms-inventario ms-pedidos ms-pagos ms-marketing ms-rutas ms-reparto ms-frio ms-reportes ms-notificaciones api-gateway) do (
  start "%%S" cmd /k "java -jar %%S\target\%%S-1.0.0.jar"
  timeout /t 6 /nobreak >nul
)
echo.
echo Listo. API para el front: http://localhost:8080/api/v1
echo Swagger de cada servicio: http://localhost:808X/doc/swagger-ui.html (X = 1..9) y 8090-8093
pause
