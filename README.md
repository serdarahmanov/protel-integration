# Protel API Integration

This repository contains the integration between Protel reservations and WordPress loyalty points.

## Components

- `protel-integration/`  
  Spring Boot service that:
  - fetches reservations from Protel,
  - filters direct bookings by `segmentation.distributionChannel`,
  - upserts stays into `Xs0Jq_protel_stays`,
  - sends points award requests to WordPress for eligible checked-out stays.

- `protel-points-bridge.php`  
  WordPress plugin exposing `POST /wp-json/protel/v1/award-points` to award myCred points securely.

## High-Level Flow

1. Spring scheduled job syncs reservations from Protel.
2. Direct bookings are selected using configured distribution channels.
3. Matching stays are inserted/updated in `protel_stays`.
4. A second scheduled job reads `CHECKED-OUT` + not-awarded stays.
5. Spring calls the WordPress bridge endpoint.
6. WordPress awards points (idempotent) and marks stay as awarded.

## Spring Configuration

Main config file:

- `protel-integration/src/main/resources/application.yaml`

Important environment variables:

- `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASS`
- `API_BASIC_USER`, `API_BASIC_PASS`
- `PROTEL_BASE_URL`, `PROTEL_API_KEY`
- `WP_BASE_URL`, `WP_AWARD_ENDPOINT`, `WP_SHARED_SECRET`
- `PROTEL_DIRECT_BOOKING_CHANNELS` (comma-separated, example: `WEB,WEBSITE`)

Direct-booking channels are configured with fallback:

- `${PROTEL_DIRECT_BOOKING_CHANNELS:WEB,WEBSITE}`

## WordPress Bridge Configuration

In `wp-config.php`:

```php
define('PROTEL_SHARED_SECRET', 'YOUR_LONG_RANDOM_SECRET');
```

Spring must send the same value in header:

- `X-Shared-Secret`

## Build and Run (Spring)

From `protel-integration/`:

```powershell
.\mvnw.cmd clean package -DskipTests
```

Run:

```powershell
java -jar .\target\protel-api-<version>.jar
```

## Notes

- Reservation sync cursor is stored in `protel_sync_state` with key `reservations`.
- Points processing uses `protel_stays.points_awarded_at` (no separate points sync key).
- Pagination guard is implemented to stop if Protel repeats the same offset more than 3 times.
