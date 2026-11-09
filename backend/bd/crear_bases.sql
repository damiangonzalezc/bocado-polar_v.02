-- BOCADO Polar: una base de datos por microservicio (patrón "database per service").
-- OPCIONAL: cada servicio crea su base sola (createDatabaseIfNotExist=true) y Hibernate crea las tablas.
-- Si prefieres crearlas a mano, ejecuta este script en phpMyAdmin (XAMPP) → pestaña SQL.
CREATE DATABASE IF NOT EXISTS bocado_auth           CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_clientes       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_catalogo       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_inventario     CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_pedidos        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_pagos          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_marketing      CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_rutas          CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_reparto        CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_frio           CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_config         CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_reportes       CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS bocado_notificaciones CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Para empezar de cero (borra todos los datos y vuelve a cargar los de ejemplo al reiniciar):
-- DROP DATABASE bocado_auth; DROP DATABASE bocado_clientes; ... (una por servicio)
