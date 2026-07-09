# MobileMuuzaji

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Repository Structure](#4-repository-structure)
5. [Backend Setup](#5-backend-setup)
6. [Android Setup](#6-android-setup)
7. [Database Schema](#7-database-schema)
8. [API Reference](#8-api-reference)
9. [Offline-First Architecture](#9-offline-first-architecture)
10. [Deployment](#10-deployment)
11. [Known Issues & Limitations](#11-known-issues--limitations)
12. [Roadmap](#12-roadmap)

---

## 1. Project Overview

MobileMuuzaji is an offline-first Android inventory and sales management application built for small business owners in Kenya. The app allows users to track stock levels, record sales, manage organizations and employees, and view financial summaries including gross income and profit — all without requiring a persistent internet connection.

The name "Muuzaji" means "seller" in Swahili.

---

## 2. System Architecture

```
┌─────────────────────────────────────────────┐
│              Android App (Kotlin)            │
│                                             │
│  UI Layer (Activities)                      │
│       ↕                                     │
│  ViewModel Layer                            │
│       ↕                                     │
│  Repository Layer                           │
│       ↕              ↕                      │
│  Room/SQLite      Retrofit (HTTP)           │
│  (local cache)    (remote API)              │
│       ↕                                     │
│  WorkManager (background sync)              │
└──────────────────────┬──────────────────────┘
                       │ HTTPS
┌──────────────────────▼──────────────────────┐
│           FastAPI Backend (Python)           │
│                                             │
│  Route Handlers                             │
│       ↕                                     │
│  SQLAlchemy ORM                             │
│       ↕                                     │
│  PostgreSQL (Supabase)                      │
└─────────────────────────────────────────────┘
```

### Data Flow

**Online operation:**
1. User action → Activity → API call via Retrofit
2. FastAPI processes request → commits to PostgreSQL
3. Response saved to Room with `isSynced = true`
4. UI updates from in-memory list

**Offline operation:**
1. User action → Activity → saved to Room with `isSynced = false`
2. Temporary negative ID assigned to new records
3. WorkManager queues a sync job constrained to run only when connected
4. On connectivity restore → SyncWorker pushes changes to backend
5. Temp IDs replaced with server-assigned IDs

---

## 3. Technology Stack

### Backend
| Component | Technology | Version |
|---|---|---|
| Language | Python | 3.12+ |
| Framework | FastAPI | Latest |
| ORM | SQLAlchemy | 2.0+ |
| Database | PostgreSQL | 18 |
| Database host | Supabase | — |
| Validation | Pydantic | v2 |
| Password hashing | passlib (bcrypt) | Latest |
| Server | Uvicorn | Latest |

### Android
| Component | Technology | Version |
|---|---|---|
| Language | Kotlin | 2.0.0 |
| Min SDK | Android 7.0 | API 24 |
| Target SDK | Android 15 | API 35 |
| Local database | Room | 2.6.1 |
| HTTP client | Retrofit + OkHttp | 2.9.0 |
| JSON parsing | Gson | — |
| Background sync | WorkManager | 2.9.0 |
| Build system | Gradle | 8.9 |
| Android Gradle Plugin | AGP | 8.7.3 |

### Infrastructure
| Component | Service |
|---|---|
| Database | Supabase (PostgreSQL) |
| Backend hosting | Railway |
| Source control | GitHub |

---

## 4. Repository Structure

```
MobileMuuzaji/
├── backend/                          # Python/FastAPI backend
│   ├── main.py                       # App entry point and route definitions
│   ├── database.py                   # SQLAlchemy engine and session setup
│   ├── models.py                     # SQLAlchemy table models
│   ├── schemas.py                    # Pydantic request/response schemas
│   ├── security.py                   # Password hashing utilities
│   ├── requirements.txt
│   ├── Dockerfile
│   ├── docker-compose.yml
│   ├── .env.example
│   └── .gitignore
│
└── MobileMuuzajiApp/                 # Android application
    ├── app/src/main/java/com/mobilemuuzaji/app/
    │   ├── MainActivity.kt           # Entry point, connectivity check
    │   ├── OnboardingActivity.kt     # First-time user onboarding
    │   ├── AuthActivity.kt           # Login and signup
    │   ├── OrganizationsActivity.kt  # Organization list
    │   ├── OrganizationActivity.kt   # Inventory and sales views
    │   ├── EmployeesActivity.kt      # Employee management
    │   ├── SessionManager.kt         # SharedPreferences session handling
    │   ├── NetworkUtils.kt           # Connectivity checking
    │   ├── TooltipHelper.kt          # Contextual onboarding tooltips
    │   ├── database/
    │   │   ├── AppDatabase.kt        # Room database definition
    │   │   ├── entities/             # Room table entities
    │   │   └── dao/                  # Data Access Objects
    │   ├── network/
    │   │   ├── RetrofitClient.kt     # HTTP client setup
    │   │   ├── ApiClient.kt          # Singleton API service
    │   │   ├── ApiService.kt         # Retrofit endpoint definitions
    │   │   └── models/
    │   │       └── AuthModels.kt     # Network request/response data classes
    │   ├── repository/               # Repository layer (coordinates Room + API)
    │   ├── viewmodel/                # ViewModels and ViewModelFactories
    │   └── sync/
    │       ├── SyncWorker.kt         # WorkManager background sync logic
    │       └── SyncManager.kt        # Sync scheduling utilities
    └── app/src/main/res/
        ├── layout/                   # XML layout files
        ├── drawable/                 # Icons, backgrounds, shapes
        └── anim/                     # Side panel animations
```

---

## 5. Backend Setup

### Prerequisites

- Python 3.12+
- PostgreSQL 18 (or a Supabase account)
- pip

### Local Development Setup

**1. Clone the repository**
```bash
git clone https://github.com/Hannah-Muthoni-Hailu/MobileMuuzaji.git
cd MobileMuuzaji/backend
```

**2. Create and activate a virtual environment**
```bash
python -m venv venv
source venv/bin/activate        # macOS/Linux
venv\Scripts\activate           # Windows
```

**3. Install dependencies**
```bash
pip install -r requirements.txt
```

**4. Create your `.env` file**
```env
DATABASE_URL=postgresql://username:password@host:5432/dbname
```

**5. Run the development server**
```bash
uvicorn main:app --reload
```

The API will be available at `http://localhost:8000`.
Interactive API docs are available at `http://localhost:8000/docs`.

### Docker Setup

```bash
# Copy and configure environment
cp .env.example .env
# Edit .env with your database credentials

# Build and run
docker compose up --build
```

### Environment Variables

| Variable | Description | Required |
|---|---|---|
| `DATABASE_URL` | Full PostgreSQL connection string | Yes |

---

## 6. Android Setup

### Prerequisites

- Java 21
- Android SDK (API 35)
- A physical Android device (API 24+) or emulator
- ADB installed

### Build Setup

**1. Clone the repository**
```bash
git clone https://github.com/Hannah-Muthoni-Hailu/MobileMuuzaji.git
cd MobileMuuzaji/MobileMuuzajiApp
```

**2. Configure the API base URL**

In `app/src/main/java/com/mobilemuuzaji/app/network/RetrofitClient.kt`:
```kotlin
private const val BASE_URL = "https://your-backend-url/"
```

**3. Build a debug APK**
```bash
./gradlew assembleDebug
```

The APK will be at `app/build/outputs/apk/debug/app-debug.apk`.

**4. Install on a connected device**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Java version installation (Incase your system has a different Java version)
Set up Android SDK
```
mkdir -p /workspaces/MobileMuuzaji/android-sdk/cmdline-tools
cd /workspaces/MobileMuuzaji/android-sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

echo 'export ANDROID_HOME=/workspaces/MobileMuuzaji/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/platform-tools' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin' >> ~/.bashrc
source ~/.bashrc

source ~/.bashrc
sdkmanager "platform-tools" "platforms;android-35" "build-tools;35.0.0"
```

Create ```local.properties```
```
echo "sdk.dir=/workspaces/MobileMuuzaji/android-sdk" > /workspaces/MobileMuuzaji/MobileMuuzajiApp/local.properties
```

Determine the Java version being used currently
```
which java
readlink -f $(which java) # If Java version 21 is in use skip this step
```

Install Java 21
```
sdk install java 21.0.11-ms

# Switch to Java 21
sdk use java 21.0.11-ms

# Verify
java -version
```

Get the path
```
readlink -f $(which java)
# Will output something like /usr/local/sdkman/candidates/java/21.0.7-ms/bin/java
```

Ensure this is the path in ```gradle.properties```
```
org.gradle.java.home=/usr/local/sdkman/candidates/java/21.0.11-ms
org.gradle.daemon=false
```

### Gradle Properties

The following properties are configured in `gradle.properties` and may need adjusting based on your development environment:

```properties
org.gradle.java.home=/path/to/java/21
org.gradle.daemon=false
org.gradle.jvmargs=-Xmx1024m -XX:MaxMetaspaceSize=512m -XX:+HeapDumpOnOutOfMemoryError
android.useAndroidX=true
```

---

## 7. Database Schema

### `users`
| Column | Type | Constraints |
|---|---|---|
| id | INTEGER | PRIMARY KEY |
| name | VARCHAR | NOT NULL |
| email | VARCHAR | NOT NULL, UNIQUE |
| password | VARCHAR | NOT NULL (bcrypt hash) |

### `organization`
| Column | Type | Constraints |
|---|---|---|
| id | INTEGER | PRIMARY KEY |
| org_name | VARCHAR | NOT NULL |
| admin_id | INTEGER | FK → users.id |

### `organization_employees`
| Column | Type | Constraints |
|---|---|---|
| employee_id | INTEGER | FK → users.id, PRIMARY KEY |
| organization_id | INTEGER | FK → organization.id, PRIMARY KEY |

### `inventory`
| Column | Type | Constraints |
|---|---|---|
| id | INTEGER | PRIMARY KEY |
| item_name | VARCHAR | NOT NULL |
| item_quantity | INTEGER | NOT NULL |
| unit | ENUM(Units) | NOT NULL |
| buying_price | INTEGER | NOT NULL |
| selling_price | INTEGER | NOT NULL |
| vat_percentage | INTEGER | NULLABLE |
| org_id | INTEGER | FK → organization.id |

### `sales`
| Column | Type | Constraints |
|---|---|---|
| id | INTEGER | PRIMARY KEY |
| item_name | VARCHAR | NOT NULL |
| item_quantity | INTEGER | NOT NULL |
| buying_price | INTEGER | NOT NULL |
| selling_price | INTEGER | NOT NULL |
| gross_income | INTEGER | NOT NULL |
| profit | INTEGER | NOT NULL |
| vat_amount | INTEGER | NULLABLE |
| date | DATETIME | server default: now() |
| org_id | INTEGER | FK → organization.id |

### Supported Units (Enum)

```python
kilogram, gram, pound, ounce, metric_ton,
liter, milliliter, gallon, fluid_ounce, cup, item
```

---

## 8. API Reference

All endpoints are prefixed with the base URL. Responses follow standard HTTP status codes. Validation errors return `422` with a `detail` array of field-level errors. General errors return the appropriate 4xx/5xx with a `detail` string.

---

### Authentication

#### `POST /signup`

Creates a new user account.

**Request body:**
```json
{
  "name": "Jane Doe",
  "email": "janedoe@example.com",
  "password": "securepassword",
  "password_repeat": "securepassword"
}
```

**Response `200`:**
```json
{
  "token": "jwt-token-string",
  "user": {
    "id": 1,
    "name": "Jane Doe",
    "email": "janedoe@example.com",
    "admin_orgs": [],
    "employee_orgs": []
  }
}
```

---

#### `POST /login`

Authenticates an existing user.

**Request body:**
```json
{
  "email": "janedoe@example.com",
  "password": "securepassword"
}
```

**Response `200`:** Same structure as `/signup`.

**Error `401`:**
```json
{ "detail": "Invalid credentials" }
```

---

### Organizations

#### `POST /new-org`

Creates a new organization. The requesting user becomes the admin.

**Request body:**
```json
{
  "org_name": "My Shop",
  "admin_id": 1
}
```

**Response `200`:** Returns updated user object including the new organization in `admin_orgs`.

---

#### `GET /organization/{org_id}`

Returns full organization details including employees, inventory and sales.

**Response `200`:**
```json
{
  "organization": {
    "id": 1,
    "name": "My Shop",
    "admin_id": 1,
    "admin_name": "Jane Doe",
    "employees": [],
    "inv_items": [],
    "sales_items": []
  }
}
```

---

#### `POST /new-employee`

Adds an existing user to an organization by email.

**Request body:**
```json
{
  "org_id": 1,
  "employee_email": "employee@example.com"
}
```

**Response `200`:** Returns updated organization with employee list.

**Error `404`:** Employee account does not exist.

---

#### `DELETE /remove-employee`

Removes an employee from an organization without deleting their account.

**Request body:**
```json
{
  "org_id": 1,
  "employee_id": 2
}
```

**Response `200`:** Returns updated organization with employee list.

---

### Inventory

#### `POST /new-product`

Creates a new inventory item.

**Request body:**
```json
{
  "name": "Sugar",
  "quantity": 100,
  "unit": "kilogram",
  "buying_price": 80,
  "selling_price": 120,
  "vat_percentage": null,
  "org_id": 1
}
```

**Response `200`:**
```json
{
  "id": 1,
  "item_name": "Sugar",
  "item_quantity": 100,
  "unit": "kilogram",
  "buying_price": 80,
  "selling_price": 120,
  "vat_percentage": null,
  "org_id": 1
}
```

---

#### `PUT /update-product/{item_id}`

Updates an existing inventory item.

**Request body:** Same structure as `/new-product` minus `org_id`.

**Response `200`:** Returns updated inventory item.

**Error `404`:** Item not found.

---

### Sales

#### `POST /sale`

Records a sale. Deducts quantity from inventory atomically using a row-level lock to prevent race conditions. Calculates gross income, profit and VAT automatically.

**Request body:**
```json
{
  "item_id": 1,
  "quantity_sold": 10,
  "sale_price": null,
  "update_price": false
}
```

| Field | Description |
|---|---|
| `item_id` | ID of the inventory item being sold |
| `quantity_sold` | Number of units sold |
| `sale_price` | Optional override for selling price. If `null`, uses the item's current `selling_price` |
| `update_price` | If `true` and `sale_price` is provided, updates the item's `selling_price` permanently |

**Response `200`:**
```json
{
  "id": 1,
  "item_name": "Sugar",
  "item_quantity": 10,
  "buying_price": 80,
  "selling_price": 120,
  "gross_income": 1200,
  "profit": 400,
  "vat_amount": null,
  "date": "2025-07-04T10:00:00",
  "org_id": 1
}
```

**Error `400`:** `"Not enough stock"` — quantity sold exceeds current stock.

**Error `404`:** Item not found.

---

## 9. Offline-First Architecture

### Local Database (Room)

The Android app maintains a local SQLite database via Android Room that mirrors the server schema. Every read and write goes to Room first. Records created or modified offline are marked with `isSynced = false`.

**Key entities and their sync fields:**

| Entity | `isSynced` | Temp ID strategy |
|---|---|---|
| `InventoryEntity` | Yes | Negative timestamp ID for offline-created items |
| `SalesEntity` | Yes | Negative timestamp ID for offline sales |
| `UserEntity` | Yes | — |
| `OrganizationEntity` | Yes | — |

### Sync Worker

`SyncWorker` is a `CoroutineWorker` managed by WorkManager. It runs when the device has an active network connection and handles:

**Inventory sync:**
- Negative ID → `POST /new-product` (new item created offline)
- Positive ID, unsynced → `PUT /update-product/{id}` (item edited offline)
- On success: temp ID replaced with server ID, `isSynced` set to `true`

**Sales sync:**
- Multiple offline sales for the same item are consolidated into a single API call to prevent "not enough stock" errors from sequential deductions
- `salePrice` and `updatePrice` fields are preserved through to sync so price changes made offline are correctly applied to the backend

**Retry strategy:** Exponential backoff starting at 30 seconds. Returns `Result.retry()` if any item fails, `Result.success()` only when all items sync successfully.

### Scheduling

Sync is scheduled in three situations:
1. Every app startup (catches changes from previous offline sessions)
2. Immediately after any offline write
3. `ExistingWorkPolicy.REPLACE` ensures only one sync job is queued at a time

### Offline ID Management

Records created offline are assigned a temporary ID using:
```kotlin
val tempId = -(System.currentTimeMillis().toInt())
```

Negative IDs are guaranteed not to conflict with server-assigned positive IDs. On successful sync, the temp record is deleted and replaced with the server-assigned ID.

---

## 10. Deployment

### Backend (Railway)

**Prerequisites:** GitHub account, Railway account

1. Push your backend code to GitHub
2. Go to [railway.app](https://railway.app) → **New Project** → **Deploy from GitHub repo**
3. Select your repository — Railway detects the `Dockerfile` automatically
4. Set environment variables under **Variables**:
   ```
   DATABASE_URL=your-supabase-pooler-connection-string
   ```
5. Railway assigns a public URL on deploy

**Auto-deploy:** Every push to `main` triggers a redeploy automatically.

---

### Database (Supabase)

1. Create a project at [supabase.com](https://supabase.com)
2. Go to **Project Settings** → **Database** → **Connection pooling**
3. Copy the **Transaction pooler** URI (port 6543)
4. Append `?pgbouncer=true` to the connection string
5. Use this as `DATABASE_URL` in Railway

Tables are created automatically on first backend startup via `Base.metadata.create_all()`.

**Important:** Supabase free tier pauses projects after 1 week of inactivity. Set up a periodic health check ping to prevent this:

```python
@app.get("/health")
def health_check():
    return {"status": "ok"}
```

Ping `https://your-railway-url/health` every 6 days using a cron service such as [cron-job.org](https://cron-job.org).

---

### Android (APK distribution)

The app is not currently on the Play Store. Distribution is via direct APK install:

```bash
# Build
./gradlew assembleDebug

# Install via ADB
adb install app/build/outputs/apk/debug/app-debug.apk
```

For production builds:
```bash
./gradlew assembleRelease
```

A signed release APK is required for Play Store submission.

---

## 11. Known Issues & Limitations

**Sync race condition:** If `fetchFromApi()` and `SyncWorker` run simultaneously after coming online, server data can overwrite unsynced local changes. Mitigation: `fetchFromApi()` checks for unsynced records before pulling from the server. Full resolution requires a proper sync queue with conflict resolution strategy.

**Sale consolidation on sync:** Multiple offline sales of the same item are merged into a single API call. This means individual sale timestamps for offline sales are lost — only a single consolidated sale record is created on the server.

**Earnings on offline sales:** Profit and gross income for offline sales are estimated using local buying/selling prices. If prices differ from what the server calculates (e.g. due to VAT rounding), figures will be corrected on sync.

**Employee management is online-only:** Adding and removing employees requires an active internet connection. There is no offline queue for employee changes.

**No token expiry handling:** JWT tokens do not currently expire or refresh. Session management beyond logout is not yet implemented.

**Supabase inactivity pause:** Free tier Supabase projects pause after 1 week of inactivity, causing connection failures until manually resumed from the dashboard.

---

## 12. Roadmap

### Version 2 (Planned)

**Data management tools:**
- Sales and inventory trend visualisations
- Data export in CSV, JSON and XLSX formats
- Purchase tracking to map actual profit against buying costs

**Financial features:**
- VAT reporting and management
- Profit and loss summaries over custom date ranges
- Price history tracking per inventory item

**Technical improvements:**
- Alembic database migrations (replacing `create_all()`)
- JWT token refresh and proper session expiry
- Full conflict resolution strategy for the sync layer
- Play Store release with signed APK

**Stretch goals:**
- M-Pesa payment integration
- WhatsApp Business sharing for stock and receipt sharing
- Mobile-based data analysis tooling