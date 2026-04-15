from Sastrawi.Stemmer.StemmerFactory import StemmerFactory
from Sastrawi.StopWordRemover.StopWordRemoverFactory import StopWordRemoverFactory

# ─────────────────────────────────────────────
#  Inisialisasi
# ─────────────────────────────────────────────
factory        = StemmerFactory()
stemmer        = factory.create_stemmer()

stop_factory   = StopWordRemoverFactory()
stop_remover   = stop_factory.create_stop_word_remover()

# ─────────────────────────────────────────────
#  Fungsi Bantu
# ─────────────────────────────────────────────
def stem_kata(kata: str) -> str:
    """Melakukan stemming pada satu kata."""
    return stemmer.stem(kata.lower())


def stem_kalimat(kalimat: str) -> str:
    """Melakukan stemming pada seluruh kata dalam kalimat."""
    return stemmer.stem(kalimat.lower())


def hapus_stopword(teks: str) -> str:
    """Menghapus stop word dari teks."""
    return stop_remover.remove(teks)


def proses_lengkap(teks: str) -> dict:
    """
    Pipeline lengkap:
      1. Lowercase
      2. Hapus stop word
      3. Stemming
    Mengembalikan dict berisi setiap tahap proses.
    """
    teks_asli         = teks
    teks_lower        = teks.lower()
    teks_no_stop      = hapus_stopword(teks_lower)
    teks_stemmed      = stem_kalimat(teks_no_stop)

    kata_asli         = teks_lower.split()
    kata_stemmed      = [stem_kata(k) for k in kata_asli]
    pasangan          = list(zip(kata_asli, kata_stemmed))

    return {
        "teks_asli"       : teks_asli,
        "setelah_lowercase": teks_lower,
        "setelah_stopword" : teks_no_stop,
        "hasil_stemming"   : teks_stemmed,
        "pasangan_kata"    : pasangan,
    }

def demo():
    while True:
        teks = input("  >> Masukkan teks: ").strip()
        if teks.lower() in ("keluar", "exit", "quit"):
            print("  Terima kasih! Program selesai.")
            break
        if not teks:
            print("  [!] Teks tidak boleh kosong.\n")
            continue

        hasil = proses_lengkap(teks)
        print(f"\n  ✔ Asli        : {hasil['teks_asli']}")
        print(f"  ✔ Stop-word   : {hasil['setelah_stopword']}")
        print(f"  ✔ Stemmed     : {hasil['hasil_stemming']}")
        print(f"  ✔ Per kata    :")
        for asli, stem in hasil["pasangan_kata"]:
            print(f"       {asli:<20} →  {stem}")
        print()

if __name__ == "__main__":
    demo()
    