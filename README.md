# AILY E-commerce Chatbot

Aplikasi e-commerce terintegrasi chatbot berbasis NLP (Natural Language Processing) yang memungkinkan pengguna mencari produk, mengelola keranjang, checkout, melacak pesanan, dan mendapatkan jawaban FAQ — semuanya melalui percakapan bahasa Indonesia. Dilengkapi dashboard admin untuk mengelola produk, transaksi, dan informasi toko.

---

## Tech Stack

| Layer     | Teknologi                                                                 |
|-----------|---------------------------------------------------------------------------|
| Backend   | Python 3.11+, FastAPI 0.95.2, Uvicorn 0.22.0                             |
| Frontend  | Java 17, JavaFX 17.0.6, Maven, Gson 2.10.1, java.net.http.HttpClient     |
| DB Utama  | SQLite 3 (`backend/aily.db`)                                              |
| Chat Log  | MongoDB 4.x+ (koleksi `chatUserLog`)                                      |
| NLP       | TensorFlow/Keras 2.21.0, PySastrawi 1.2.0, NumPy 1.26.4 (Hybrid ML + Rule) |
| Auth      | bcrypt 4.0.1                                                              |

---

## Arsitektur Sistem

```
┌──────────────────────────────────────────────────────────────────┐
│                        JavaFX Frontend                           │
│  Landing → Login/Register → Chat → Cart → Checkout → Orders     │
│  Admin: Overview → Products → Transactions → Store Info → Chat  │
└───────────────────────────┬──────────────────────────────────────┘
                            │ HTTP (JSON)
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│                     FastAPI Backend (:8000)                      │
│  ┌──────────┐ ┌──────────────┐ ┌───────────┐ ┌──────────────┐  │
│  │AuthRouter│ │ConversationR.│ │CartService│ │OrderService  │  │
│  │ProductMg.│ │   (NLP Hub)  │ │           │ │              │  │
│  └────┬─────┘ └──────┬───────┘ └─────┬─────┘ └──────┬───────┘  │
│       │              │               │               │           │
│  ┌────▼──────────────▼───────────────▼───────────────▼────┐     │
│  │              NLPHandler (Hybrid Pipeline)              │     │
│  │  Normalisasi → ML Model → Threshold 2-tier → Rules    │     │
│  └────────────────────────┬───────────────────────────────┘     │
│                           │                                      │
│  ┌────────────────────────▼───────────────────────────────┐     │
│  │           databaseConnection.py (SQLite + MongoDB)      │     │
│  │  SQLite: user, product, cart, cart_item, orders,        │     │
│  │          order_items, tentangToko, help                  │     │
│  │  MongoDB: chatUserLog                                   │     │
│  └─────────────────────────────────────────────────────────┘     │
└──────────────────────────────────────────────────────────────────┘
```

---

## Struktur Direktori

```
AILY-Devin/
├── backend/
│   ├── main.py                          # Entry point FastAPI
│   ├── requirements.txt                 # Dependency Python
│   ├── aily.db                          # Database SQLite (auto-created)
│   ├── auth/
│   │   └── key_validator.py             # Validasi key (legacy)
│   ├── NLP/
│   │   ├── NLPHandler.py                # Pipeline NLP hybrid (ML + Rule)
│   │   └── model/
│   │       ├── chatbot.keras            # Model TensorFlow terlatih
│   │       ├── words.pkl                # Vocabulary Bag-of-Words
│   │       ├── classes.pkl              # Label intent classes
│   │       └── intents.json             # Dataset training (11 intents)
│   ├── routers/
│   │   ├── authRouter.py                # Auth: register, login, profile
│   │   ├── conversationRouter.py        # Chatbot, FAQ, store info, chat history
│   │   ├── cartService.py               # Keranjang: add, update, remove, clear
│   │   ├── orderService.py              # Pesanan: checkout, list, cancel, admin
│   │   └── productManagementService.py  # Produk: CRUD admin + search
│   ├── services/
│   │   └── databaseConnection.py        # SQLite + MongoDB, semua DB class
│   └── utils/
│       └── response.py                  # Standard response wrapper
│
└── frontend/
    ├── pom.xml                          # Maven config (Java 17, JavaFX 17.0.6)
    └── src/main/
        ├── java/com/aily/
        │   ├── App.java                 # Entry point & scene switcher
        │   ├── Session.java             # Global state (user, cart, orders)
        │   ├── controller/
        │   │   ├── LandingController.java
        │   │   ├── LoginController.java
        │   │   ├── LoginAdminController.java
        │   │   ├── RegisterController.java
        │   │   ├── ChatController.java
        │   │   ├── CartController.java
        │   │   ├── OrdersController.java
        │   │   ├── ProfileController.java
        │   │   ├── AdminOverviewController.java
        │   │   ├── AdminProductsController.java
        │   │   ├── AdminTransactionsController.java
        │   │   ├── AdminStoreController.java
        │   │   └── AdminChatController.java
        │   ├── model/
        │   │   ├── User.java
        │   │   ├── Product.java
        │   │   ├── CartItem.java
        │   │   ├── Order.java
        │   │   └── ChatMessage.java
        │   └── service/
        │       └── ApiService.java       # Semua HTTP call ke backend
        └── resources/com/aily/
            ├── style.css                 # Dark Teal Theme (global)
            ├── landing.fxml
            ├── login.fxml
            ├── login_admin.fxml
            ├── register.fxml
            ├── chat.fxml
            ├── cart.fxml
            ├── orders.fxml
            ├── profile.fxml
            ├── admin_overview.fxml
            ├── admin_products.fxml
            ├── admin_transactions.fxml
            ├── admin_store.fxml
            └── admin_chat.fxml
```

---

## Database Schema (SQLite)

### Tabel `user`
| Kolom    | Tipe         | Keterangan                          |
|----------|--------------|-------------------------------------|
| id       | INTEGER PK   | Auto-increment                      |
| username | TEXT         | Unique                              |
| password | TEXT         | Hash bcrypt                         |
| email    | TEXT         |                                     |
| phone    | TEXT         |                                     |
| address  | TEXT         |                                     |
| role     | TEXT         | `user` atau `admin`                 |
| gender   | TEXT         | `L` (Pria), `P` (Wanita), default `L` |

### Tabel `product`
| Kolom       | Tipe        | Keterangan                     |
|-------------|-------------|--------------------------------|
| id          | INTEGER PK  | Auto-increment                 |
| name        | TEXT        |                                |
| price       | INTEGER     | Harga dalam Rupiah             |
| stock       | INTEGER     | Jumlah stok tersedia           |
| image       | MEDIUMBLOB  | Gambar produk (Base64/binary)  |
| description | TEXT        |                                |
| gender      | TEXT        | `L`, `P`, atau `U` (Unisex)    |

### Tabel `cart`
| Kolom  | Tipe        | Keterangan                                              |
|--------|-------------|---------------------------------------------------------|
| id     | INTEGER PK  | Auto-increment                                          |
| userId | INTEGER FK  | → user.id                                               |
| status | TEXT        | `Belum Checkout`, `Checkout`, `Dalam Pengiriman`, `Selesai` |

### Tabel `cart_item`
| Kolom        | Tipe        | Keterangan     |
|--------------|-------------|----------------|
| id           | INTEGER PK  | Auto-increment |
| cartId       | INTEGER FK  | → cart.id      |
| productId    | INTEGER FK  | → product.id   |
| jumlah_barang| INTEGER     | Kuantitas      |

### Tabel `orders`
| Kolom         | Tipe        | Keterangan                                                   |
|---------------|-------------|--------------------------------------------------------------|
| id            | INTEGER PK  | Auto-increment                                               |
| userId        | INTEGER FK  | → user.id                                                    |
| status        | TEXT        | `Diproses`, `Dalam Pengiriman`, `Selesai`, `Dibatalkan`      |
| subtotal      | INTEGER     |                                                              |
| shipping_cost | INTEGER     | Default 15000                                                |
| discount      | INTEGER     | Default 0                                                    |
| total         | INTEGER     | subtotal + shipping_cost - discount                           |
| created_at    | TEXT        | ISO 8601                                                     |
| updated_at    | TEXT        | ISO 8601                                                     |

### Tabel `order_items`
| Kolom                | Tipe        | Keterangan          |
|----------------------|-------------|---------------------|
| id                   | INTEGER PK  | Auto-increment      |
| orderId              | INTEGER FK  | → orders.id         |
| productId            | INTEGER FK  | → product.id        |
| quantity             | INTEGER     |                     |
| price_snapshot       | INTEGER     | Harga saat checkout |
| product_name_snapshot| TEXT        | Nama saat checkout  |

### Tabel `tentangToko`
| Kolom    | Tipe       | Keterangan                    |
|----------|------------|-------------------------------|
| id       | INTEGER PK | Auto-increment                |
| question | TEXT(50)   | Contoh: "Jam Buka", "alamat" |
| answer   | TEXT(100)  |                               |

### Tabel `help`
| Kolom    | Tipe       | Keterangan                       |
|----------|------------|----------------------------------|
| id       | INTEGER PK | Auto-increment                   |
| question | TEXT(50)   | Contoh: "retur", "ongkir", "cod" |
| answer   | TEXT(100)  |                                  |

### Seed Data (auto-insert saat pertama kali)

**tentangToko:**
| question      | answer                                                        |
|---------------|---------------------------------------------------------------|
| Jam Buka      | 10.00 - 21.00                                                 |
| pemilik       | Antonius Kiya Ananda Derron                                   |
| deskripsi     | Kami adalah toko yang berfokus pada pembelian barang DIY...   |
| alamat        | Jalan Dr. Wahidin No.1-5, Kota Yogyakarta, DIY               |
| Visi dan Misi | Menjadi olshop terpercaya dan termurah di DIY...              |

**help:**
| question  | answer                                                               |
|-----------|----------------------------------------------------------------------|
| retur     | Barang bisa diretur maksimal 7 hari setelah diterima...              |
| refund    | Refund diproses setelah barang retur diterima dan diverifikasi...     |
| garansi   | Garansi mengikuti kebijakan masing-masing produk...                  |
| ongkir    | Ongkir dihitung saat checkout sesuai alamat dan metode pengiriman...  |
| pembayaran| Pembayaran dapat dilakukan melalui transfer bank, e-wallet...         |
| promo     | Promo yang tersedia akan diinformasikan melalui chat...               |
| cod       | COD tersedia hanya untuk wilayah dan produk tertentu.                 |

---

## API Endpoints

### Authentication

| Method | Endpoint              | Parameter                              | Keterangan              |
|--------|-----------------------|----------------------------------------|-------------------------|
| POST   | `/aily/registration`  | uname, pword, email, phone, add, role, gender | Register user baru |
| POST   | `/aily/login`         | uname, pword                           | Login, return user data |

### User Profile

| Method | Endpoint                | Parameter          | Keterangan            |
|--------|-------------------------|--------------------|-----------------------|
| GET    | `/aily/user/profile`    | id (user token)    | Ambil data profil     |
| POST   | `/aily/user/updateUser` | id, dataList       | Update profil user    |

### Chatbot & Conversation

| Method | Endpoint                              | Parameter               | Keterangan              |
|--------|---------------------------------------|-------------------------|-------------------------|
| POST   | `/aily/conversation`                  | id, message             | Kirim pesan ke chatbot  |
| POST   | `/aily/conversation/{user_token}`     | user_token, id, message | (Legacy) Kirim pesan    |
| GET    | `/aily/user/conversation/chat/load`   | user_id                 | Muat riwayat chat       |
| POST   | `/aily/user/conversation/chat/save`   | user_id, username, role, message | Simpan chat  |
| DELETE | `/aily/user/conversation/chat/delete` | user_id                 | Hapus riwayat chat      |

### Product (Admin CRUD)

| Method | Endpoint                            | Parameter / Body                  | Keterangan       |
|--------|-------------------------------------|-----------------------------------|------------------|
| GET    | `/aily/admin/product/list`          | —                                 | List semua produk|
| POST   | `/aily/admin/product/add`           | name, price, stock, image, description, gender | Tambah produk |
| PUT    | `/aily/admin/product/update/{id}`   | name, price, stock, image, description, gender | Edit produk   |
| DELETE | `/aily/admin/product/delete/{id}`   | —                                 | Hapus produk     |

### Cart

| Method | Endpoint                  | Parameter / Body               | Keterangan              |
|--------|---------------------------|--------------------------------|-------------------------|
| GET    | `/aily/user/cart`         | user_id                        | Lihat isi keranjang     |
| POST   | `/aily/user/cart/add`     | user_id, product_id, quantity  | Tambah ke keranjang     |
| PUT    | `/aily/user/cart/item`    | user_id, product_id, quantity  | Update jumlah item      |
| DELETE | `/aily/user/cart/item`    | user_id, product_id            | Hapus item dari keranjang|
| DELETE | `/aily/user/cart/clear`   | user_id                        | Kosongkan keranjang     |
| GET    | `/aily/admin/cart/list`   | —                              | Admin: list semua cart  |

### Order & Checkout

| Method | Endpoint                            | Parameter / Body    | Keterangan                    |
|--------|-------------------------------------|---------------------|--------------------------------|
| POST   | `/aily/user/checkout`               | user_id             | Checkout keranjang → pesanan   |
| GET    | `/aily/user/orders`                 | user_id             | List pesanan user              |
| POST   | `/aily/user/orders/{id}/cancel`     | user_id             | Batalkan pesanan               |
| GET    | `/aily/admin/orders`                | —                   | Admin: list semua pesanan      |
| PUT    | `/aily/admin/orders/{id}/status`    | status              | Admin: update status pesanan   |

### Store Info & FAQ

| Method | Endpoint                            | Parameter / Body     | Keterangan              |
|--------|-------------------------------------|----------------------|-------------------------|
| GET    | `/aily/tentangToko`                 | —                    | Info toko (publik)      |
| GET    | `/aily/help`                        | —                    | FAQ (publik)            |
| GET    | `/aily/admin/store-info/list`       | —                    | Admin: list info toko   |
| POST   | `/aily/admin/store-info/add`        | question, answer     | Admin: tambah info toko |
| PUT    | `/aily/admin/store-info/update/{id}`| question, answer     | Admin: edit info toko   |
| DELETE | `/aily/admin/store-info/delete/{id}`| —                    | Admin: hapus info toko  |

### Response Format

Semua endpoint mengembalikan response standar:

```json
// Sukses
{
  "status": 200,
  "success": true,
  "data": { ... }
}

// Error
{
  "status": 400,
  "success": false,
  "error": "Pesan error"
}
```

---

## NLP Pipeline (Hybrid ML + Rule-Based)

Chatbot AILY menggunakan pendekatan **hybrid** 2-tier untuk klasifikasi intent:

```
Input User
    │
    ▼
┌───────────────────┐
│ 1. Normalisasi    │  Kata informal → baku (contoh: "carikan" → "cari", "cowok" → "pria")
└─────────┬─────────┘
          ▼
┌───────────────────┐
│ 2. ML Prediction  │  TensorFlow model (Bag-of-Words → Dense layers → Softmax)
└─────────┬─────────┘
          ▼
┌───────────────────────────────────────────────────┐
│ 3. Threshold 2-tier                              │
│   • Confidence ≥ 0.67 → Percaya ML (source: ml)  │
│   • 0.50–0.67        → Konfirmasi rule engine     │
│     - Rule cocok → ml+rule                        │
│     - Rule beda  → rule                           │
│     - Produk ada → product_detect                 │
│     - Gagal semua → fallback                      │
│   • < 0.50           → Coba rule/product/fallback │
└─────────┬─────────────────────────────────────────┘
          ▼
┌───────────────────┐
│ 4. Intent Handler │  Route ke handler yang sesuai
└───────────────────┘
```

### Daftar Intent (11 intents)

| Intent           | Handler                | Deskripsi                              | Akses   |
|------------------|------------------------|----------------------------------------|---------|
| `salam`          | —                      | Sapaan (Halo, Hai, dll.)               | User    |
| `faq`            | FAQHandler             | Pertanyaan umum (retur, garansi, dll.) | User    |
| `mencari`        | ProductSearchHandler   | Pencarian produk di katalog            | User    |
| `checkout`       | CheckoutHandler        | Proses pembayaran                      | User    |
| `lacak_kiriman`  | TrackingHandler        | Lacak status pengiriman                | User    |
| `status_pesanan` | OrderStatusHandler     | Cek status pesanan                     | User    |
| `batal_pesanan`  | OrderHandler           | Batalkan pesanan                       | User    |
| `tanya_toko`     | StoreProfileHandler    | Informasi toko (alamat, jam, dll.)     | User    |
| `help`           | HelpHandler            | Menu bantuan/daftar fitur              | User    |
| `crud`           | AdminProductHandler    | Kelola produk (tambah/edit/hapus)      | Admin   |
| `terima_kasih`   | —                      | Ucapan terima kasih                    | User    |
| `selamat_tinggal`| —                      | Ucapan perpisahan                      | User    |
| `tidak_diketahui`| FallbackHandler        | Input tidak dikenali                   | User    |

### Fitur NLP Tambahan
- **Normalisasi bahasa informal**: 100+ mapping kata (contoh: `pengen`→`ingin`, `cewek`→`wanita`, `batalin`→`batalkan`)
- **Deteksi produk**: Mengenali 18+ jenis produk (baju, celana, sepatu, kaos, jaket, rok, dress, hoodie, sweater, kemeja, gamis, blazer, sandal, tas, topi, aksesoris, pakaian, ransel, jeans, payung)
- **Deteksi gender**: Mengenali kata pria/wanita dari input (cowok→L, cewek→P)
- **Keyword rules**: Backup intent detection berbasis keyword untuk 8 intent utama
- **CRUD via chat**: Admin bisa menambah/edit/hapus produk langsung dari chat dengan format `nama=..., harga=..., stok=...`

---

## Frontend Screens

| Screen               | FXML                  | Controller                    | Deskripsi                         |
|----------------------|-----------------------|-------------------------------|-----------------------------------|
| Landing              | `landing.fxml`        | LandingController             | Halaman pembuka                   |
| Login User           | `login.fxml`          | LoginController               | Login untuk user                  |
| Login Admin          | `login_admin.fxml`    | LoginAdminController          | Login untuk admin                 |
| Register             | `register.fxml`       | RegisterController            | Form registrasi (7 field)         |
| Chat                 | `chat.fxml`           | ChatController                | Chatbot + navigasi sidebar        |
| Keranjang            | `cart.fxml`           | CartController                | Isi keranjang + checkout          |
| Pesanan              | `orders.fxml`         | OrdersController              | Daftar pesanan + batal            |
| Profil               | `profile.fxml`        | ProfileController             | Edit data pribadi user            |
| Admin Overview       | `admin_overview.fxml` | AdminOverviewController       | Dashboard admin                   |
| Admin Produk         | `admin_products.fxml` | AdminProductsController       | CRUD produk + upload gambar       |
| Admin Transaksi      | `admin_transactions.fxml` | AdminTransactionsController | Kelola status pesanan            |
| Admin Info Toko      | `admin_store.fxml`    | AdminStoreController          | CRUD informasi toko               |
| Admin Chat           | `admin_chat.fxml`     | AdminChatController           | Monitor chat                      |

---

## Business Logic

### Checkout Flow
1. User mengisi keranjang (status `Belum Checkout`)
2. Checkout dipanggil → validasi stok
3. Buat record di `orders` (status `Diproses`) + `order_items`
4. Kurangi stok produk
5. Ubah status cart menjadi `Checkout`
6. Buat cart baru (status `Belum Checkout`) untuk user
7. Shipping cost default: Rp 15.000, discount default: Rp 0
8. Nomor pesanan format: `TRX-XXXXXX` (contoh: `TRX-000001`)

### Cancel Order
- Hanya bisa dibatalkan jika status **belum** `Dalam Pengiriman`, `Selesai`, atau `Dibatalkan`
- Saat dibatalkan: stok produk dikembalikan (restock)
- Pesanan yang sudah dibatalkan tidak bisa diaktifkan kembali oleh admin

### Admin Update Status
- Status valid: `Diproses` → `Dalam Pengiriman` → `Selesai` (atau `Dibatalkan`)
- Tidak bisa mengubah pesanan yang sudah `Dibatalkan` ke status lain

---

## Cara Menjalankan

### Prasyarat
- Python 3.11+
- Java 17+ (JDK)
- MongoDB 4.x+ (berjalan di `localhost:27017`)
- Maven 3.8+

### Backend

```bash
cd backend
pip install -r requirements.txt
python main.py
```

Server berjalan di `http://localhost:8000`.  
Swagger UI tersedia di `http://localhost:8000/docs`.

Database SQLite (`aily.db`) dan seed data akan dibuat otomatis saat pertama kali dijalankan.

### Frontend

```bash
cd frontend
mvn clean javafx:run
```

Aplikasi JavaFX akan terbuka dalam mode maximized.

### Environment Variables (opsional)

Buat file `.env` di folder `backend/`:

```env
MONGO_URI=mongodb://localhost:27017
MONGO_DB=aily
```

---

## Functional Requirements (FR) — Status

| FR   | Deskripsi                              | Status |
|------|----------------------------------------|--------|
| FR-01| Chat-based search via NLP              | ✅     |
| FR-02| Input data pribadi profil              | ✅     |
| FR-03| Identifikasi jenis permintaan (hybrid) | ✅     |
| FR-04| Tampilkan informasi produk             | ✅     |
| FR-05| Tampilkan barang yang dibeli           | ✅     |
| FR-06| Simpan state barang dibeli             | ✅     |
| FR-07| Batalkan pesanan sebelum pengantaran   | ✅     |
| FR-08| Jawab FAQ otomatis                     | ✅     |
| FR-09| Simpan riwayat percakapan              | ✅     |
| FR-10| Tambah data produk (admin)             | ✅     |
| FR-11| Tambah informasi toko (admin)          | ✅     |
| FR-12| Login pengguna                         | ✅     |
| FR-13| Login penjual / dashboard admin        | ✅     |
| FR-14| Checkout dari keranjang                | ✅     |

---

## Catatan Keamanan & Pengembangan Lanjutan

- **Autentikasi**: Saat ini menggunakan hashed password (bcrypt) sebagai session token. Untuk production, disarankan migrasi ke JWT.
- **Admin guard**: Endpoint `/aily/admin/*` belum memiliki middleware role guard. Siapa saja bisa memanggil endpoint admin.
- **SQL injection**: Sebagian besar query sudah menggunakan parameterized query, namun `searchBarang` masih menggunakan string interpolation untuk parameter `name`.
- **Validasi input**: Register endpoint menerima parameter via query string, belum menggunakan request body dengan Pydantic validation.
- **Image handling**: Gambar produk disimpan sebagai Base64 string / MEDIUMBLOB di SQLite. Untuk skala besar, pertimbangkan penyimpanan file terpisah (S3, lokal filesystem).
