# Prakash Bankers — Client Laptop Setup Guide

Everything runs **locally** on the client's computer. No internet hosting, no HTTPS,
no CORS configuration needed. This guide is for setting the app up once on the
client's Windows laptop.

---

## 1. What gets installed (prerequisites)

| Software | Version | Why |
|----------|---------|-----|
| **Java JDK** | 21 or newer (Temurin / Microsoft / Oracle) | Runs the backend |
| **Node.js** | 22 LTS or 24 (npm included) | Builds/runs the frontend |
| **MySQL** | 8.0 | Stores the data |

Optional:

| Software | Why |
|----------|-----|
| **Docker Desktop** | Alternative way to run MySQL (`docker compose up -d`) instead of installing MySQL |

> The backend uses the bundled Maven wrapper (`mvnw.cmd`), so Maven does **not**
> need to be installed separately.

### Installing Java
1. Download a JDK 21+ (e.g. Eclipse Temurin 21, or Microsoft Build of OpenJDK).
2. Install, then set an environment variable:
   - **JAVA_HOME** = the JDK folder (e.g. `C:\Program Files\Eclipse Adoptium\jdk-21...`)
   - Add `%JAVA_HOME%\bin` to **Path**.
3. Verify in a new PowerShell: `java -version`

### Installing Node.js
1. Download the Windows Installer (LTS) from nodejs.org and install.
2. Verify: `node -v` and `npm -v`

### Installing MySQL (option A — local, recommended)
1. Install **MySQL Server 8.0** (MySQL Installer for Windows).
2. Keep the Windows service running (services: `MySQL80`).
3. Note the `root` password you set during installation — you will need it in step 3.

### Installing MySQL (option B — Docker)
```powershell
docker compose up -d
```
This starts MySQL 8 on port 3306 with root password `admin` and database
`prakash_bankers` (see `docker-compose.yml`).

---

## 2. Copy the project onto the laptop

Copy the whole project folder (or `git clone` the repo). A path **without spaces**
is safest, for example:

```
C:\prakash-bankers
```

If PowerShell refuses to run the scripts, unblock them once:
```powershell
cd C:\prakash-bankers
Get-ChildItem -Recurse -Filter *.ps1 | Unblock-File
```

---

## 3. Database settings

The app reads settings from `backend\src\main\resources\application.yml`.
Defaults:

```yaml
url:      jdbc:mysql://localhost:3306/prakash_bankers?createDatabaseIfNotExist=true...
username: root
password: Admin@123      # <-- IMPORTANT: must match the MySQL root password
```

The database `prakash_bankers` is **created automatically** on first start
(`createDatabaseIfNotExist=true`). Tables are also created automatically.

**The only thing that must match is the password.** Choose one of:

- **Option 1 (recommended):** set the MySQL `root` password to `Admin@123`, **or**
- **Option 2:** edit `password:` in `application.yml` to the real MySQL password, **or**
- **Option 3:** set an environment variable before starting the backend:
  ```powershell
  $env:DB_PASSWORD = "YourRealMySqlPassword"
  ```
  (Other overridable settings: `DB_USERNAME`, `DB_URL`, `SERVER_PORT`.)

> Docker option: root password is `admin`, so use Option 2 or set `DB_PASSWORD=admin`.

---

## 4. Start the application

From the project root (`C:\prakash-bankers`):

```powershell
.\start-app.ps1
```

This starts the backend on **8082**, waits until it is healthy, then starts the
frontend on **8081**. The **first** backend start downloads dependencies and can
take a few minutes — wait for "Started PrakashBankersApplication".

Then open the app in a browser:

```
http://localhost:8081
```

Log in with:

- **Username:** `admin`
- **Password:** `admin123`

> Do **not** open `http://localhost:8082` directly — that is the API, only used
> internally by the app.

### Starting manually (two terminals)
```powershell
# Terminal 1 — backend
cd C:\prakash-bankers\backend
.\run-backend.ps1

# Terminal 2 — frontend
cd C:\prakash-bankers\frontend
.\run-frontend.ps1
```

---

### Starting from VS Code (one shortcut — best for daily use)

The project ships VS Code tasks in `.vscode/tasks.json`:

1. Open the project folder in VS Code (`File → Open Folder → C:\prakash-bankers`).
2. Press **Ctrl+Shift+B** (or **Terminal → Run Task… → "Start App"**).
3. Both servers start in two VS Code terminals:
   - `Backend: run (Spring Boot :8082)` — wait for *"Started PrakashBankersApplication"*
   - `Frontend: run (Vite :8081)` — wait for *"Local: http://localhost:8081/"*
4. Open **http://localhost:8081** in the browser.
5. To stop everything: **Terminal → Run Task… → "Stop App (free ports 8081 + 8082)"**
   (or click the trash/terminate icon on each terminal).

> MySQL must already be running (Windows service `MySQL80`, or `docker compose up -d`).
> If VS Code shows *"cannot be loaded because running scripts is disabled"*, run this
> once in a terminal: `Get-ChildItem -Recurse -Filter *.ps1 | Unblock-File`

---

## 5. First-run data (what the client starts with)

On an empty database the app seeds only the essentials:

- Login user: **admin / admin123**
- Materials: **Gold, Silver, Platinum**
- Shop settings: name "Prakash Bankers", address "Main Road, Salem", phone "9876543210"

There are **no sample customers, loans, banks or lenders** — the ledger starts
empty. Change the shop name / address / phone under **Settings** after first login.

### Changing the login password

Log in, go to **Settings → Change Password**, enter the current password, the new
password (minimum 6 characters) and confirm it. That's it — use the new password
the next time you log in. There is no need to touch the database.

> If you ever forget the password, it is stored as a BCrypt hash in the `users`
> table, so it must be reset via SQL with a generated hash (the app has a
> built-in changer, so this should not be needed).

---

## 6. Day-to-day use & data safety

- **Data is kept** between restarts (database `ddl-auto: update`).
- To back up everything, dump the database:
  ```powershell
  & "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" -uroot -p prakash_bankers > backup.sql
  ```
- Uploaded files (ID proofs, material photos, bills) are stored in
  `backend\uploads\` — include this folder in backups.
- To restore: `mysql -uroot -p prakash_bankers < backup.sql`

---

## 7. Troubleshooting

| Symptom | Fix |
|---------|-----|
| Frontend shows blank pages / no data | Backend not running. Start it first and wait for "Started PrakashBankersApplication". |
| `ECONNREFUSED` in the Vite console | Same as above — backend is not up yet. |
| Backend fails at startup (DB error) | MySQL not running, or the password in `application.yml` does not match the MySQL root password (step 3). |
| `Port 8081/8082 already in use` | Another program owns the port. Stop it, or change the port. |
| PowerShell script will not run | `Get-ChildItem -Recurse -Filter *.ps1 \| Unblock-File`, then retry. |
| Lombok errors in Eclipse/STS | Not needed — always run the backend through `run-backend.ps1` (Maven handles Lombok). |
| Need to run without Docker | Use a local MySQL 8 service instead (option A in step 1). |

---

## 8. Lombok (do I need to install it?)

**To run the app: no.** The backend is always built through Maven
(`run-backend.ps1` → `mvnw.cmd spring-boot:run`), and the Maven build declares
Lombok as an annotation processor (`pom.xml` → `maven-compiler-plugin` →
`annotationProcessorPaths`, version 1.18.46). Maven downloads Lombok from Maven
Central on the first build (needs internet once) and generates all
getters/setters/builders automatically. Nothing to install on the client's laptop.

Lombok is `provided` and is **excluded from the packaged jar**, so the running
application does not depend on Lombok at all.

It only matters **if you open the source code in an IDE**, because the IDE needs
its own Lombok support to understand the generated methods:

| IDE | What to do |
|-----|------------|
| **VS Code** | Already configured in this project: `.vscode/settings.json` enables `java.jdt.ls.lombokSupport`, and `.vscode/extensions.json` recommends the Java pack + Lombok extension. Accept the recommendations. |
| **Eclipse / STS** | Needs Lombok added to the IDE itself (a `-javaagent` line in `SpringToolsForEclipse.ini`). This is machine-specific and is **not** part of the project. Alternatively, ignore IDE errors and always build via `run-backend.ps1`. |
| **IntelliJ IDEA** | Lombok is bundled — just enable *Settings → Build → Compiler → Annotation Processors → Enable annotation processing*. |

Red errors like *"The method builder() is undefined"* in an IDE mean the IDE is
missing Lombok support — the Maven build is still fine. The version is pinned to
**1.18.46** on purpose (it supports recent JDKs); leave it as is.

> On very new JDKs (24/25) you may see harmless `sun.misc.Unsafe` warning lines
> from Lombok on the console. They are safe to ignore.

---

## 9. Ports reference

| Service | URL / Port |
|---------|-----------|
| Frontend (open this) | http://localhost:8081 |
| Backend API (internal) | http://localhost:8082 |
| MySQL | localhost:3306 |
