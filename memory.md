# AILY E-commerce Chatbot — Devin Memory

Quick-reference notes for future sessions. Scanned 2026-04-24.

---

## 1. What this project is
AILY is a chatbot-driven e-commerce app (Tugas Akhir RPLBO). Users / admins
chat in Bahasa Indonesia; an NLP layer classifies the intent and routes to a
handler (search, cart, checkout, admin CRUD, FAQ, etc.).

- **Backend**: Python 3 + FastAPI (`main.py`, port 8000)
- **Frontend**: JavaFX 17 + FXML + Gson (`frontend/`, Maven)
- **Databases**:
  - **SQLite** `aily.db` — relational data (users, products, carts, tentangToko, help)
  - **MongoDB** `aily` — chat logs (`chatUserLog` collection), connection
    hard-coded to `mongodb://Kiya:Jogja321@localhost:27017/?authSource=admin`
    inside `services/databaseConnection.py` (⚠️ credentials in source).
- **ML model**: tiny Keras feed-forward net (`NLP/Model/chatbot.keras`)
  trained by `NLP/Tools/ModelMaker.py` on `intents.json` with Sastrawi stemmer.

Git: origin `github.com/leutenantKiya/AILY-Ecommerce-chatbotService`,
currently on branch `kiya` (local branches: `Kevin`, `KevinSandbox`, `kiya`, `main`).

---

## 2. Directory map

```
.
├── main.py                     # FastAPI entry point, registers 4 routers
├── requirements.txt            # fastapi, uvicorn, pymongo, PySastrawi, tensorflow 2.13.1, numpy 1.26.4, bcrypt, python-dotenv
├── setup_env.py                # Creates ./venv and installs requirements
├── aily.db                     # SQLite DB (gitignored but currently committed)
├── auth/key_validator.py       # Legacy key validator (imports `data.dummy_keys` — module missing)
├── services/databaseConnection.py  # MongoDB, SQLite, ProductDB, CartDB classes
├── utils/response.py           # Response.Ok / Error / NotFound / Unauthorized / ValidationError / ServerError
├── routers/
│   ├── authRouter.py           # POST /aily/registration, POST /aily/login (bcrypt)
│   ├── conversationRouter.py   # Main chat endpoint + chat history + profile update
│   ├── productManagementService.py  # Admin product CRUD REST + perform_* helpers
│   ├── cartService.py          # User cart REST + perform_add_to_cart / perform_get_cart_summary helpers
│   └── cobaSave.py             # Scratch script (image insert experiment, mostly commented out)
├── NLP/
│   ├── NLPHandler.py           # Hybrid rule+ML intent router, normalization dict, handler classes
│   ├── Model/                  # chatbot.keras, words.pkl, classes.pkl, intents.json
│   └── Tools/
│       ├── ModelMaker.py       # Trains the Keras model (must be run from NLP/Model dir — paths are relative)
│       └── Stemming.py         # Standalone Sastrawi stemmer demo
├── prepare/
│   ├── seed_products.py        # Product seeding script (body is commented, only prints kaos image bytes)
│   ├── mongo_test.py           # Quick chat-log inspector
│   ├── aily_backup.sql
│   └── *.png / *.jpg / *.jpeg  # product images
├── reverts/ + REVERT.md        # revert-marker files (git housekeeping)
└── frontend/                   # JavaFX Maven project, see §6
```

---

## 3. Backend architecture

```
User types → ChatController (JavaFX) → POST /aily/conversation
          → handle_chat()
          → auth by userId = bcrypt hash (treated as opaque token)
          → save_chat() → MongoDB chatUserLog
          → try_handle_cart_command() fast-path (keyword "keranjang"/"cart")
          → else NLP.process() returns {intent, konten, atribut, probabilitas, source, respons}
          → intent dispatch: mencari / checkout / faq / tanya_toko / help / crud (admin only) / salam / ...
          → response JSON: {status, success, data:{user_id, username, role, input, nlp_result, action_data}}
```

Notable quirks:
- User identification token = the bcrypt-hashed password string. `findUserByPassword()`
  uses this as a lookup key. Not a proper JWT session — fragile, but pervasive.
- Login route returns `data.id = stored_hash` (confusing naming: it's the password
  hash, not the numeric user id).
- `role` column values: `"user"` / `"admin"`; intents in `ADMIN_ONLY_INTENTS = ["crud"]`.
- `gender` column on user/product: `L` (laki-laki), `P` (perempuan), `U` (unisex).
  Search falls back to `user[7]` for default gender; product search filters `gender = ? OR gender = 'U'`.
- `auth/key_validator.py` imports from a non-existent `data.dummy_keys` — it's
  legacy / dead code, not used by current routers.

### SQLite schema (created by `SQLite.createTable`)
- `user(id, username, password [bcrypt], email, phone, address, role, gender='L')`
- `cart(id, userId→user, status ∈ {'Belum Checkout','Checkout','Dalam Pengiriman','Selesai'})`
- `product(id, name, price, stock, image BLOB, description, gender='U')`
- `cart_item(id, cartId→cart, productId→product, jumlah_barang)`
- `tentangToko(id, question, answer)`
- `help(id, question, answer)`

### REST endpoints (all registered under `/aily/...`)
| Method | Path | Where |
|---|---|---|
| POST | `/aily/registration` | authRouter |
| POST | `/aily/login` | authRouter |
| POST | `/aily/conversation` | conversationRouter (body: `{id, message}`) |
| POST | `/aily/conversation/{user_token:path}` | legacy variant |
| GET  | `/aily/tentangToko` | conversationRouter |
| GET  | `/aily/help` | conversationRouter |
| POST | `/aily/user/conversation/chat/save` | conversationRouter |
| GET  | `/aily/user/conversation/chat/load?user_id=` | conversationRouter |
| DELETE | `/aily/user/conversation/chat/delete?user_id=` | conversationRouter |
| POST | `/aily/user/updateUser?id=` body=`[[col, val], ...]` | conversationRouter |
| GET  | `/aily/admin/product/list` | productManagementService |
| POST | `/aily/admin/product/add` | productManagementService |
| PUT  | `/aily/admin/product/update/{id}` | productManagementService |
| DELETE | `/aily/admin/product/delete/{id}` | productManagementService |
| GET  | `/aily/user/cart?user_id=` | cartService |
| POST | `/aily/user/cart/add` | cartService |
| PUT  | `/aily/user/cart/item` | cartService |
| DELETE | `/aily/user/cart/item?user_id=&product_id=` | cartService |
| DELETE | `/aily/user/cart/clear?user_id=` | cartService |

---

## 4. NLP layer (`NLP/NLPHandler.py`)

Hybrid pipeline:
1. `normalize_text()` — lowercase + informal→baku via `NORMALIZATION_DICT`
   (e.g. "cariin"→"cari", "cowok"→"pria", partikel "dong"/"deh"/... dropped).
2. Tokenize (regex) → Sastrawi stem → bag-of-words.
3. Keras model predicts intent (softmax over `classes.pkl`).
4. 2-tier thresholding:
   - prob ≥ 0.67 → trust ML
   - 0.5 ≤ prob < 0.67 → require rule-engine confirmation OR product keyword
   - prob < 0.5 → rule engine / product detect / else fallback
5. Route to a `*Handler` class returning `{intent, konten, [atribut]}`.

Intents in `classes.pkl` + `intents.json`: `salam`, `faq`, `mencari`, `crud`,
`checkout`, `lacak_kiriman`, `tanya_toko`, `status_pesanan`, `batal_pesanan`,
`help`, `terima_kasih`, `selamat_tinggal`, `tidak_diketahui`.

⚠️ Model files are loaded with **relative paths** `NLP/model/...` → the server
must be started from the repo root, otherwise `pickle.load` fails.
(Note: on case-sensitive FS the folder is `NLP/Model` but code uses lowercase
`NLP/model`; works on Windows / macOS default, breaks on Linux — keep an eye
on this if deploying.)

### Retraining
```
cd NLP/Model
python ../Tools/ModelMaker.py   # overwrites chatbot.keras / words.pkl / classes.pkl in CWD
```
`ModelMaker.py` reads `intents.json` from CWD and writes pickles + keras file to CWD.

---

## 5. Admin chat-based CRUD (conversationRouter)
When intent is `crud` and role is admin, `handle_admin_product_command()` parses:
- Action keyword: `tambah|input|buat` → add, `hapus|delete` → delete,
  `update|edit|ubah` → update, `lihat|list|daftar|semua|tampilkan` → list.
- ID: regex `\bproduk\s+(\d+)\b|\bid\s+(\d+)\b|\b(\d+)\b`.
- Fields via `key=value` pairs (keys: nama|harga|stok|deskripsi|gender|gambar
  + English aliases), parsed by `extract_key_value_fields`.

Example: `tambah produk nama=Kaos Polos, harga=99000, stok=10, deskripsi=Basic, gender=U`.

---

## 6. Frontend (`frontend/`, JavaFX)

Maven: `com.aily:aily-frontend:1.0-SNAPSHOT`, Java 17, JavaFX 17.0.6, Gson 2.10.1.
Entry `com.aily.App` — always loads maximized; scenes switched via `App.switchScene(fxmlName)`.

FXMLs in `src/main/resources/com/aily/`:
- `landing`, `login`, `login_admin`, `register`
- `chat` (user), `cart`, `orders`
- `admin_chat`, `admin_overview`, `admin_products`, `admin_transactions`
- `style.css`

Controllers in `com.aily.controller.*Controller`. Models in `com.aily.model.*`
(`User`, `Product`, `CartItem`, `Order`, `ChatMessage`). Shared state in
`com.aily.Session` (static fields for current user / cart / orders).

Service layer: `com.aily.service.ApiService` — all HTTP calls go to
`http://localhost:8000` via Java 11 `HttpClient` + Gson. Method list:
`login`, `register`, `sendMessage`, `getChatHistory`, `saveChatMessage`,
`deleteChat`, `getTentangToko`, `getProduk`, `addProduct` (sends `category`
and `warna` fields that backend currently ignores),
`updateProduct`, `deleteProduct`, `getUserCart`, `addToCart`,
`updateCartItem`, `removeFromCart`, `clearUserCart`, `updateUserProfile`.

Build/run frontend (PowerShell):
```
cd frontend
mvn clean javafx:run        # if javafx-maven-plugin is configured
# otherwise: mvn package and run via `java --module-path ... -m com.aily/com.aily.App`
```
(Check full `pom.xml` — only first ~60 lines read; plugins section truncated.)

---

## 7. Running the stack (Windows, current env)

Prereqs: Python 3.10/3.11 (TF 2.13 wheels), Java 17, MongoDB running on `:27017`
with user `Kiya` / password `Jogja321` on authSource `admin`, SQLite file present.

```powershell
# one-time setup
python setup_env.py
.\venv\Scripts\activate
pip install -r requirements.txt

# run backend (must be from repo root so NLP/model/*.pkl resolves)
python main.py                 # or: uvicorn main:app --reload --port 8000
# Swagger at http://localhost:8000/docs

# frontend
cd frontend
mvn clean compile
mvn javafx:run    # verify in pom.xml
```

---

## 8. Known rough edges / things to watch

- **Secrets in code**: MongoDB URI `mongodb://Kiya:Jogja321@localhost:27017/...`
  hardcoded in `services/databaseConnection.py`. The commented-out block
  already shows an env-based alternative — migrate to `os.getenv("MONGO_URI")`
  when cleaning up.
- **`data.dummy_keys`** referenced by `auth/key_validator.py` does not exist
  → import error if anything touches it. Confirm it's dead code before deleting.
- **Relative model paths** (`NLP/model/...`) assume CWD is repo root. Case
  mismatch (`Model` vs `model`) can break on Linux.
- **`findUserByPassword` returns a tuple** with indices:
  `(id, username, password, email, phone, address, role, gender)`.
  Multiple routers index into it by position — don't reorder the SQL columns.
- **Duplicate keys in `NORMALIZATION_DICT`**: "cariin" and "cwk" each appear
  twice (second definition wins). "cwk" maps to both "pria" and later "wanita".
- **Cart/product frontend fields mismatch**: `ApiService.addProduct` sends
  `category` and `warna` which `ProductRequest`(`productManagementService.py`)
  does not define → Pydantic will error with extra fields (depending on
  config). Either extend `ProductRequest` or drop these in the Java call.
- **`aily.db` is committed** despite `*.db` being in `.gitignore`. It was
  likely added before the ignore rule.
- **Saved chat content**: `save_chat()` pushes the full sanitized bot response
  object into Mongo (product lists, base64 images). Keeps chats large.
- **`response.data.id`** in `/aily/login` is the password hash, not `user[0]`.
  Frontend `Session` relies on this being the opaque token.

---

## 9. Git / branch state

- Current branch: `kiya` (last commit `cb70237 Resolve conflicts: keeping kiya branch changes`).
- Remotes: single `origin` on GitHub.
- Working tree clean at session start.

---

## 10. Useful greps / starting points

- Where does a new intent get handled? → `HANDLER_MAP` in `NLP/NLPHandler.py`
  + dispatch switch in `handle_chat()` in `routers/conversationRouter.py`.
- Add a new REST endpoint? → create an `APIRouter`, add to `main.py` via
  `app.include_router(...)`.
- Add a DB column? → modify `createTable` SQL in `services/databaseConnection.py`
  AND update all tuple indexing in routers (esp. `conversationRouter.handle_chat`
  accessing `user[6]`, `user[7]`).
- Add training data? → edit `NLP/Model/intents.json`, run `ModelMaker.py`
  from `NLP/Model/`.
