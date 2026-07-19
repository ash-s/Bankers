# Prakash Bankers — Pledge & Repledge Ledger

Modern SPA for pawn/repledge shop operations: customer pledges, interest engine, repledge arbitrage, lender borrowings, and in-hand cash tracking.

## Stack

| Layer | Technology |
|-------|------------|
| Frontend | React 19 + TypeScript + Vite + Ant Design |
| Backend | Java 21 + Spring Boot 4 + Spring Security (JWT) |
| Database | MySQL 8 |

## Ports (important)

| Service | URL | Notes |
|---------|-----|-------|
| **Frontend (open in browser)** | **http://localhost:8081** | React app |
| Backend API | http://localhost:8082 | Used automatically; do not open directly |
| MySQL | localhost:3306 | Database |

## Quick Start (Windows)

### 1. Start MySQL

```powershell
docker compose up -d
```

Or use local MySQL with database `prakash_bankers`.

Default DB credentials: **root / admin** (matches `application.yml` and `docker-compose.yml`).
For a different local password, set `DB_PASSWORD` before starting the backend.

### 2. Start everything (easiest)

```powershell
cd C:\Users\LDS\prakash-bankers
.\start-app.ps1
```

The script waits until the backend is actually healthy before starting the frontend.
Open **http://localhost:8081** and log in with **admin / admin123**.

### 3. Or start manually

**Terminal 1 — Backend:**
```powershell
cd backend
.\run-backend.ps1
```

**Terminal 2 — Frontend:**
```powershell
cd frontend
npm install
.\run-frontend.ps1
```

Open **http://localhost:8081** in your browser.

## Data persistence

The database **keeps your data** between restarts (`ddl-auto: update`).

On the **first run** with an empty database, sample data is seeded automatically (admin user, one customer, banks, lenders, etc.).

If you previously used `ddl-auto: create`, that wiped the database on every restart — that is now fixed.

## Troubleshooting

### No data / blank pages in frontend

1. **Backend must be running** on port 8082 before the frontend can load data.
2. Open the app at **http://localhost:8081** (not 8082).
3. Log in with **admin / admin123**.
4. Check MySQL is running: `docker compose ps` or MySQL service in Windows.
5. If a session from yesterday is expired, the app now clears it and returns to login automatically.

### Vite proxy errors (`ECONNREFUSED`)

The backend is not running. Start `backend\run-backend.ps1` first and wait until you see "Started PrakashBankersApplication".

### MySQL connection failed

- Ensure MySQL is running on port 3306
- Password in `application.yml` must match your MySQL root password (default: `admin`)
- Create database if needed: `CREATE DATABASE prakash_bankers;`

### Port 8081 already in use

Another app is using 8081. Stop it, or change `frontend/vite.config.ts` `server.port` and add the new URL to `app.cors-origins` in `application.yml`.

### Eclipse/STS: Lombok `builder()` errors

Use Maven to run: `backend\run-backend.ps1` — the code compiles fine via Maven.

## Default Login

- **Username:** admin
- **Password:** admin123

## Project Structure

```
prakash-bankers/
├── backend/          Spring Boot API (port 8082)
├── frontend/         React SPA (port 8081)
├── start-app.ps1     Start both services
├── docker-compose.yml
└── README.md
```
