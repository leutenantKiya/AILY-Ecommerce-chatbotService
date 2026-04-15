# AILY E-commerce Chatbot Service

Chatbot service untuk e-commerce yang memproses bahasa alami (NLP) dan mengarahkan ke handler yang sesuai.

**Tech Stack:** FastAPI (Python) + JavaFX (Frontend) + JDBC (Database)

---

## Arsitektur Sistem

```
User/Admin Input → FastAPI → Auth (validasi key) → NLP → Intent Router → Handler → Response
```

## Endpoint

| Role  | Endpoint                                  |
|-------|-------------------------------------------|
| User  | `POST /aily/user/{id}/{publicKeyUser}`    |
| Admin | `POST /aily/admin/{id}/{publicKeyAdmin}`  |

> Jika `id + publicKey` tidak valid → kembalikan `"KeyAndaTidakSesuai"`

## Daftar Intent & Handler

| No | Handler                 | Fungsi                                    |
|----|-------------------------|-------------------------------------------|
| 1  | FAQHandler              | Merespons pertanyaan umum pelanggan       |
| 2  | FallbackHandler         | Menangani input yang gagal dipahami       |
| 3  | ProductSearchHandler    | Memproses pencarian katalog produk        |
| 4  | AdminProductHandler     | Admin mengelola data produk (CRUD)        |
| 5  | ProductDetailHandler    | Menyajikan informasi rinci satu produk    |
| 6  | CartHandler             | Mengelola item dalam keranjang belanja    |
| 7  | CheckoutHandler         | Mengarahkan proses transaksi pembayaran   |
| 8  | TrackingHandler         | Melacak status pengiriman logistik        |
| 9  | StoreProfileHandler     | Menampilkan informasi detail toko         |
| 10 | OrderStatusHandler      | Memeriksa kondisi pesanan pelanggan       |
| 11 | OrderHandler            | Memproses data pesanan secara keseluruhan |

## Tahapan Build

- [x] **Tahap 1** — Setup proyek & entry point (`main.py`, `requirements.txt`)
- [ ] **Tahap 2** — Autentikasi key (`auth/key_validator.py`)
- [ ] **Tahap 3** — Model data / schema (`models/schemas.py`)
- [ ] **Tahap 4** — NLP processor (`nlp/processor.py`)
- [ ] **Tahap 5** — Intent router (`intents/router.py`)
- [ ] **Tahap 6** — 11 Handler (`handlers/*.py`)
- [ ] **Tahap 7** — Integrasi & testing

## Cara Menjalankan

```bash
pip install -r requirements.txt
python main.py
```

Buka Swagger UI di: `http://localhost:8000/docs`