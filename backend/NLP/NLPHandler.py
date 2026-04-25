import json
import pickle
import re
import random
import numpy as np

from Sastrawi.Stemmer.StemmerFactory import StemmerFactory
from tensorflow.keras.models import load_model


# ═════════════════════════════════════════════
#  KONFIGURASI
# ═════════════════════════════════════════════
CONFIDENCE_THRESHOLD = 0.5   # Diturunkan dari 0.67 → 0.5


# ═════════════════════════════════════════════
#  NORMALISASI — Kamus bahasa informal → baku
# ═════════════════════════════════════════════
NORMALIZATION_DICT = {
    # Kata kerja informal
    "cariin":    "cari",
    "carikan":   "cari",
    "cariin":    "cari",
    "carin":     "cari",
    "nyari":     "cari",
    "pengen":    "ingin",
    "pengin":    "ingin",
    "pingin":    "ingin",
    "mau":       "ingin",
    "mo":        "ingin",
    "pgn":       "ingin",
    "beli":      "beli",
    "membeli":   "beli",
    "liat":      "lihat",
    "lht":       "lihat",
    "liatin":    "lihat",
    "tunjukin":  "tunjukkan",
    "tampilin":  "tampilkan",
    "kasih":     "beri",
    "kasiin":    "beri",
    "batalin":   "batalkan",
    "cancellin": "batalkan",
    "hapusin":   "hapus",
    "bayarin":   "bayar",
    "udah":      "sudah",
    "udh":       "sudah",
    "uda":       "sudah",
    "blm":       "belum",
    "blum":      "belum",
    "gak":       "tidak",
    "ga":        "tidak",
    "gk":        "tidak",
    "nggak":     "tidak",
    "ngga":      "tidak",
    "engga":     "tidak",
    "enggak":    "tidak",
    "gpp":       "tidak apa",
    "gimana":    "bagaimana",
    "gmn":       "bagaimana",
    "gmana":     "bagaimana",
    "dimana":    "di mana",
    "dmn":       "di mana",
    "kapan":     "kapan",
    "kpn":       "kapan",

    # Gender informal
    "lanang":    "pria",
    "cowok":     "pria",
    "cowo":      "pria",
    "cwk":       "pria",
    "laki":      "pria",
    "mas":       "pria",
    "cewek":     "wanita",
    "cewe":      "wanita",
    "cwk":       "wanita",
    "perempuan": "wanita",
    "mbak":      "wanita",

    # Sapaan informal
    "halooo":    "halo",
    "haloo":     "halo",
    "hay":       "hai",
    "hei":       "hai",

    # Singkatan umum
    "dong":      "",
    "donk":      "",
    "deh":       "",
    "sih":       "",
    "nih":       "",
    "ya":        "",
    "yah":       "",
    "aja":       "saja",
    "doang":     "saja",
    "bgt":       "sekali",
    "banget":    "sekali",
    "bro":       "",
    "kak":       "",
    "min":       "",
    "gan":       "",
    "sis":       "",

    # Produk informal
    "jaket":     "jaket",
    "kaos":      "kaos",
    "pakaian":   "baju",
    "abju":      "baju",
}


# ═════════════════════════════════════════════
#  KEYWORD RULES — Backup intent detection
# ═════════════════════════════════════════════
KEYWORD_RULES = {
    "mencari": [
        "cari", "beli", "ingin", "lihat", "tampilkan", "tunjukkan",
        "baju", "celana", "sepatu", "kaos", "jaket", "rok", "dress",
        "hoodie", "sweater", "kemeja", "gamis", "blazer", "sandal",
        "tas", "topi", "aksesoris", "pakaian","payung"
    ],
    "checkout": [
        "checkout", "bayar", "pembayaran", "transaksi",
    ],
    "lacak_kiriman": [
        "lacak", "tracking", "resi", "paket", "kirim", "kiriman",
    ],
    "batal_pesanan": [
        "batal", "batalkan", "cancel",
    ],
    "status_pesanan": [
        "status", "pesanan", "order", "orderan", "proses",
    ],
    "crud": [
        "tambah", "update", "hapus", "edit", "ubah", "perbarui",
        "stok", "input", "delete", "ganti",
    ],
    "tanya_toko": [
        "toko", "alamat", "lokasi", "cabang", "telepon", "kontak",
        "jam", "buka", "tutup", "operasional",
    ],
    "help": [
        "bantuan", "help", "menu", "fitur", "perintah", "panduan",
    ],
    "faq": [
        "retur", "garansi", "refund", "pengembalian", "ongkir",
        "ekspedisi", "cicilan", "diskon", "promo",
    ],
}

# Produk yang dikenali (untuk entity extraction)
PRODUCT_KEYWORDS = {
    "baju", "celana", "sepatu", "kaos", "jaket", "rok", "dress",
    "hoodie", "sweater", "kemeja", "gamis", "blazer", "sandal",
    "tas", "topi", "aksesoris", "pakaian", "ransel", "jeans",
    "muslim", "olahraga","payung",
}


# ═════════════════════════════════════════════
#  INISIALISASI MODEL
# ═════════════════════════════════════════════
factory = StemmerFactory()
stemmer = factory.create_stemmer()

words   = pickle.load(open("NLP/model/words.pkl", "rb"))
classes = pickle.load(open("NLP/model/classes.pkl", "rb"))
model   = load_model("NLP/model/chatbot.keras")

with open("NLP/model/intents.json", "r", encoding="utf-8") as f:
    intents_data = json.load(f)


# ═════════════════════════════════════════════
#  PIPELINE NLP
# ═════════════════════════════════════════════
def word_tokenize(teks: str) -> list:
    """Tokenisasi teks menggunakan regex."""
    return re.findall(r"[\w]+", teks)


def normalize_text(text: str) -> str:
    """
    Tahap 1: Normalisasi kata informal → baku.
    Dilakukan SEBELUM stemming.
    """
    tokens = word_tokenize(text.lower())
    normalized = []
    for token in tokens:
        replacement = NORMALIZATION_DICT.get(token, token)
        if replacement:  # skip token yang di-map ke "" (partikel: dong, deh, dll.)
            normalized.append(replacement)
    return " ".join(normalized)


def clean_up_sentence(sentence: str) -> list:
    """Tahap 2: Normalisasi → Tokenisasi → Stemming."""
    normalized = normalize_text(sentence)
    tokens = word_tokenize(normalized)
    return [stemmer.stem(w.lower()) for w in tokens]


def bag_of_words(sentence: str) -> np.ndarray:
    """Tahap 3: Buat bag of words dari kalimat."""
    sentence_words = clean_up_sentence(sentence)
    bag = [0] * len(words)
    for w in sentence_words:
        for i, word in enumerate(words):
            if word == w:
                bag[i] = 1
    return np.array(bag)


def predict_intent_ml(sentence: str) -> list:
    """Prediksi intent menggunakan ML model."""
    bow = bag_of_words(sentence)
    res = model.predict(np.array([bow]), verbose=0)[0]
    results = [[i, float(r)] for i, r in enumerate(res)]
    results.sort(key=lambda x: x[1], reverse=True)
    return [{"intent": classes[r[0]], "probability": r[1]} for r in results]


def predict_intent_rules(sentence: str) -> str | None:
    """
    Prediksi intent menggunakan keyword rules (backup).
    Menghitung skor berdasarkan jumlah keyword yang cocok.
    """
    normalized = normalize_text(sentence)
    tokens = set(word_tokenize(normalized))
    # Juga stem tokens untuk matching
    stemmed_tokens = set(stemmer.stem(t) for t in tokens)
    all_tokens = tokens | stemmed_tokens

    scores = {}
    for intent, keywords in KEYWORD_RULES.items():
        score = sum(1 for kw in keywords if kw in all_tokens)
        if score > 0:
            scores[intent] = score

    if scores:
        return max(scores, key=scores.get)
    return None


def detect_product(text: str) -> str:
    """Ekstrak nama produk dari teks."""
    normalized = normalize_text(text)
    tokens = word_tokenize(normalized)
    products = [t for t in tokens if t in PRODUCT_KEYWORDS]
    return " ".join(products) if products else ""


def detect_gender(text: str) -> str:
    """Deteksi gender dari teks. Mengembalikan L/P/default_user."""
    normalized = normalize_text(text)
    if any(g in normalized for g in ["pria", "laki"]):
        return "L"
    if any(g in normalized for g in ["wanita"]):
        return "P"
    return "default_user"


# ═════════════════════════════════════════════
#  HANDLER CLASSES
# ═════════════════════════════════════════════

class FAQHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "faq",
            "konten": user_input
        }


class ProductSearchHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        product = detect_product(user_input)
        gender  = detect_gender(user_input)
        return {
            "intent": "mencari",
            "konten": product,
            "atribut": {"gender": gender}
        }


class AdminProductHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "crud",
            "konten": user_input
        }


class CheckoutHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "checkout",
            "konten": ""
        }


class TrackingHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        resi_match = re.findall(r"[A-Z0-9]{8,}", user_input.upper())
        resi = resi_match[0] if resi_match else ""
        return {
            "intent": "lacak_kiriman",
            "konten": resi
        }


class StoreProfileHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "tanya_toko",
            "konten": ""
        }


class OrderStatusHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "status_pesanan",
            "konten": "pesanan"
        }


class OrderHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "batal_pesanan",
            "konten": ""
        }


class FallbackHandler:
    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "tidak_diketahui",
            "konten": ""
        }


class HelpHandler:
    AVAILABLE_INTENTS = [
        {"intent": "faq",             "deskripsi": "Pertanyaan umum (retur, garansi, ongkir, dll.)"},
        {"intent": "mencari",         "deskripsi": "Mencari produk di katalog"},
        {"intent": "crud",            "deskripsi": "Mengelola data produk (admin)"},
        {"intent": "checkout",        "deskripsi": "Proses pembayaran / checkout"},
        {"intent": "lacak_kiriman",   "deskripsi": "Melacak status pengiriman paket"},
        {"intent": "tanya_toko",      "deskripsi": "Informasi profil toko"},
        {"intent": "status_pesanan",  "deskripsi": "Mengecek status pesanan"},
        {"intent": "batal_pesanan",   "deskripsi": "Membatalkan pesanan"},
        {"intent": "help",            "deskripsi": "Menampilkan menu bantuan ini"},
    ]

    @staticmethod
    def handle(user_input: str, predictions: list) -> dict:
        return {
            "intent": "help",
            "konten": HelpHandler.AVAILABLE_INTENTS
        }


# ═════════════════════════════════════════════
#  ROUTER — Mapping intent → Handler
# ═════════════════════════════════════════════
HANDLER_MAP = {
    "faq":              FAQHandler,
    "mencari":          ProductSearchHandler,
    "crud":             AdminProductHandler,
    "checkout":         CheckoutHandler,
    "lacak_kiriman":    TrackingHandler,
    "tanya_toko":       StoreProfileHandler,
    "status_pesanan":   OrderStatusHandler,
    "batal_pesanan":    OrderHandler,
    "help":             HelpHandler,
    "salam":            None,
    "terima_kasih":     None,
    "selamat_tinggal":  None,
}


def get_response_text(tag: str) -> str:
    for intent in intents_data["intents"]:
        if intent["tag"] == tag:
            return random.choice(intent["responses"])
    return ""


# ═════════════════════════════════════════════
#  PROSES UTAMA — Hybrid Rule + ML
# ═════════════════════════════════════════════
def process(user_input: str) -> dict:
    """
    Pipeline utama (hybrid):
      1. Normalisasi teks
      2. Prediksi intent via ML model
      3. Threshold 2-tier:
         - >= 0.67 : percaya ML langsung
         - 0.5–0.67: ML harus dikonfirmasi oleh rule engine atau produk
         - < 0.5   : coba rule engine / product detect / fallback
      4. Jika ada produk terdeteksi → jangan fallback
      5. Jika semua gagal → FallbackHandler
    """
    predictions   = predict_intent_ml(user_input)
    top           = predictions[0]
    ml_tag        = top["intent"]
    ml_prob       = top["probability"]

    # Deteksi pendukung
    product  = detect_product(user_input)
    rule_tag = predict_intent_rules(user_input)

    HIGH_THRESHOLD = 0.67
    LOW_THRESHOLD  = 0.5

    # ── CASE 1: ML confidence tinggi (>= 0.67) → percaya ML ──
    if ml_prob >= HIGH_THRESHOLD:
        final_tag  = ml_tag
        final_prob = ml_prob
        source     = "ml"

    # ── CASE 2: ML sedang (0.5–0.67) → perlu konfirmasi rule/produk ──
    elif ml_prob >= LOW_THRESHOLD:
        if rule_tag and rule_tag == ml_tag:
            # Rule engine mengkonfirmasi ML → aman
            final_tag  = ml_tag
            final_prob = ml_prob
            source     = "ml+rule"
        elif rule_tag:
            # Rule engine mendeteksi intent lain → ikuti rule
            final_tag  = rule_tag
            final_prob = ml_prob
            source     = "rule"
        elif product:
            # Ada produk → anggap mencari
            final_tag  = "mencari"
            final_prob = ml_prob
            source     = "product_detect"
        else:
            # Tidak ada konfirmasi → fallback
            result = FallbackHandler.handle(user_input, predictions)
            result["probabilitas"] = round(ml_prob, 4)
            result["source"]       = "fallback"
            result["respons"]      = "Maaf, saya tidak memahami permintaan Anda. Ketik 'bantuan' untuk melihat menu."
            return result

    # ── CASE 3: ML rendah (< 0.5) → coba rule engine / product ──
    else:
        if rule_tag:
            final_tag  = rule_tag
            final_prob = ml_prob
            source     = "rule"
        elif product:
            final_tag  = "mencari"
            final_prob = ml_prob
            source     = "product_detect"
        else:
            result = FallbackHandler.handle(user_input, predictions)
            result["probabilitas"] = round(ml_prob, 4)
            result["source"]       = "fallback"
            result["respons"]      = "Maaf, saya tidak memahami permintaan Anda. Ketik 'bantuan' untuk melihat menu."
            return result

    # ── Route ke handler yang sesuai ──
    handler_class = HANDLER_MAP.get(final_tag)

    if handler_class is not None:
        result = handler_class.handle(user_input, predictions)
    else:
        result = {
            "intent": final_tag,
            "konten": ""
        }

    result["probabilitas"] = round(final_prob, 4)
    result["source"]       = source
    result["respons"]      = get_response_text(final_tag)

    return result


# ═════════════════════════════════════════════
#  DEMO INTERAKTIF
# ═════════════════════════════════════════════
if __name__ == "__main__":
    print("=" * 55)
    print("  AILY NLP Handler v2 (Hybrid: Rule + ML)")
    print("  Ketik 'Q' untuk keluar")
    print("=" * 55)

    while True:
        user_input = input("\nKamu : ").strip()
        if user_input.upper() == "Q":
            print("Sampai jumpa!")
            break
        if not user_input:
            continue

        result = process(user_input)
        print(f"\n[OUTPUT JSON]")
        print(json.dumps(result, indent=2, ensure_ascii=False))
