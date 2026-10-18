# 🚦 Vegam City — Smart Traffic Management & Emergency Vehicle Routing

A high-performance, real-time traffic control desk simulation and emergency routing engine for the City of Vegam.

---

## ⚡ Key Highlights

- **Pure Java Backend (Zero Frameworks)**: Built strictly with standard Java SE (`com.sun.net.httpserver.HttpServer`, `java.net.http.HttpClient`, `java.sql.*`). No Spring Boot, no external dependencies required to compile or run.
- **Frontend**: Clean, lightweight HTML5, modern responsive CSS3, and vanilla JavaScript (small amount, zero heavy libraries).
- **Database (MySQL)**: Full relational schema (`schema_mysql.sql`) with tables for junctions, roads, vehicles, priority requests, traffic events, and audit logs. Supports live SQL export.
- **Cloud Storage (Supabase)**: Ready for Supabase PostgreSQL (`schema_supabase.sql`) and Supabase Cloud Storage bucket (`traffic-backups`) with automated snapshot backup via native Java `HttpClient`.
- **Core Algorithms & Data Structures**:
  - Hand-written **Dijkstra's Shortest Path Algorithm** on directed, weighted graph.
  - Hand-written **Priority Queue (Min-Heap)** for emergency routing requests (ordered by `Severity`: CRITICAL > HIGH > NORMAL, then FIFO arrival order).
  - Strict **FIFO Queue (`ArrayDeque`)** for traffic events (closures, reopenings, congestion).
  - Dynamic **mid-journey re-routing**: routes are automatically re-calculated from a vehicle's last confirmed junction whenever closures/reopenings happen.

---

## 📁 Project Structure

```
SmartTrafficManagement/
├── SmartTrafficSystem.java    # Pure Java Backend + REST API + CLI
├── public/
│   ├── index.html             # Command Center Dashboard (HTML5)
│   ├── style.css              # Modern Responsive Dark Theme (CSS3)
│   └── app.js                 # Vanilla JS Controller & Canvas Map
├── database/
│   ├── schema_mysql.sql       # MySQL Database Schema & Initial Data
│   ├── schema_supabase.sql    # Supabase PostgreSQL + Storage Bucket Schema
│   └── db.properties          # Configuration for MySQL & Supabase
├── backups/                   # Local snapshot backups directory
├── run.bat                    # Windows Batch Launcher
├── run.ps1                    # Windows PowerShell Launcher
└── README.md                  # Documentation
```

---

## 🚀 How to Run

### Step 1: Compile & Start the Backend

Open your terminal or command prompt in the `SmartTrafficManagement` folder:

```powershell
javac SmartTrafficSystem.java
java SmartTrafficSystem
```
*Or simply run `.\run.ps1` or double-click `run.bat`.*

### Step 2: Open the Web UI

Open your web browser and navigate to:
```
http://localhost:8080
```

---

## 🗄️ Database Setup (MySQL)

1. Open your MySQL client (MySQL Workbench, phpMyAdmin, XAMPP, or CLI).
2. Execute the script located at:
   ```
   database/schema_mysql.sql
   ```
3. To enable live JDBC synchronization, open `database/db.properties` and configure:
   ```properties
   mysql.enabled=true
   mysql.host=localhost
   mysql.port=3306
   mysql.database=smart_traffic_db
   mysql.user=root
   mysql.password=your_password
   ```
4. *Note: If MySQL is not running or disabled, the application runs automatically in high-speed in-memory mode, and you can export the database state anytime via the "Export MySQL Dump" button!*

---

## ☁️ Cloud Storage Setup (Supabase)

1. Create a free project at [Supabase](https://supabase.com).
2. Go to the **SQL Editor** in your Supabase dashboard and run the script:
   ```
   database/schema_supabase.sql
   ```
   *This creates all tables, Row-Level-Security (RLS) policies, and the `traffic-backups` Cloud Storage bucket.*
3. Open `database/db.properties` (or configure via the **Cloud Settings** tab in the Web UI):
   ```properties
   supabase.enabled=true
   supabase.url=https://your-project-id.supabase.co
   supabase.anon_key=your_supabase_anon_key_here
   supabase.bucket=traffic-backups
   ```
4. Click **"☁️ Backup to Supabase"** in the web dashboard: the pure Java backend will upload a timestamped JSON snapshot directly to your Supabase Cloud Storage bucket!

---

## 🔌 REST API Endpoints

| Method | Endpoint | Description |
|---|---|---|
| `GET` | `/` | Serves dashboard HTML frontend |
| `GET` | `/api/status` | Full system snapshot (junctions, roads, vehicles, queues) |
| `POST` | `/api/sample-data` | Loads 12 junctions, 4 vehicles, 20 sample events |
| `POST` | `/api/demo` | Runs scripted demonstration of normal + 4 edge cases |
| `POST` | `/api/request` | Submits emergency routing request `{vehicleId, destination, severity}` |
| `POST` | `/api/request/escalate` | Escalates priority of request `{requestId, severity}` |
| `POST` | `/api/request/process-next` | Processes highest-priority request in min-heap |
| `POST` | `/api/request/process-all` | Processes all pending requests |
| `POST` | `/api/event` | Queues traffic event `{type, from, to, factor}` |
| `POST` | `/api/event/process-next` | Processes next traffic event in FIFO arrival order |
| `POST` | `/api/event/process-all` | Processes all pending traffic events |
| `POST` | `/api/vehicle/advance` | Steps vehicle to next adjacent junction `{vehicleId, to}` |
| `POST` | `/api/road` | Adds new road segment `{from, to, time, twoWay}` |
| `POST` | `/api/junction` | Adds new junction node `{id}` |
| `POST` | `/api/supabase/backup` | Uploads state snapshot to Supabase Cloud Storage |
| `GET` | `/api/export-sql` | Generates and downloads SQL dump script |
| `GET` | `/api/logs` | Fetches real-time system audit logs |

