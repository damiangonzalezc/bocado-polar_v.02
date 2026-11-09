#!/usr/bin/env bash
# BOCADO Polar - levanta los 13 microservicios y el gateway (macOS / Linux).
# Requisitos: MySQL de XAMPP encendido (3306, root sin clave) y Java 17+.
set -e
cd "$(dirname "$0")"
./mvnw -q -DskipTests install
mkdir -p logs
for s in ms-config ms-auth ms-clientes ms-catalogo ms-inventario ms-pedidos ms-pagos ms-marketing ms-rutas ms-reparto ms-frio ms-reportes ms-notificaciones api-gateway; do
  nohup java -jar "$s/target/$s-1.0.0.jar" > "logs/$s.log" 2>&1 &
  echo "$s iniciado (log en logs/$s.log)"
  sleep 6
done
echo "API para el front: http://localhost:8080/api/v1   ·   Para detener: pkill -f 'bocado|ms-|api-gateway'"
