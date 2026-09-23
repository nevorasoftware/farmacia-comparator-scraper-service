# farmacia-comparator-scraper-service

Microservicio autónomo encargado de:
- Extracción de datos en vivo desde Farmacias San Nicolás, CEFAFA (con HMAC), Camila, Económicas y SRS.
- Normalización determinística e inferencia asistida por Gemini AI.
- Detección de cambios de contenido mediante SHA-256 para minimizar costos de IA.
- Scheduler programado en zona horaria `America/El_Salvador` (UTC-6) de 08:00 AM a 05:00 PM.
- Aislamiento de fallos entre scrapers (un fallo no detiene la sincronización general).

## Variables de Entorno
- `SPRING_DATASOURCE_URL`: URL JDBC de PostgreSQL
- `SCRAPING_ENABLED`: true / false
- `SCRAPING_CRON`: `0 0 8-17 * * *` (o `0 */30 * * * *` para pruebas)
- `SCRAPING_TIMEZONE`: `America/El_Salvador`
- `AI_ENABLED`: true / false
- `GEMINI_API_KEY`: Clave de Google AI Studio (opcional, fallback heurístico determinístico activo)
- `SERVER_PORT`: 8081
