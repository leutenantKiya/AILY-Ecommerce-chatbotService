import sqlite3
import os

BASE_DIR = os.path.dirname(os.path.abspath(__file__))

def read_image(filename):
    path = os.path.join(BASE_DIR, filename)
    if os.path.exists(path):
        with open(path, "rb") as f:
            return f.read()
    print(f"WARNING: {filename} not found, skipping image")
    return None

print(read_image("kaosJMK48.jpg"))
conn = sqlite3.connect(os.path.join(BASE_DIR, "../aily.db"))
cursor = conn.cursor()

cursor.execute("SELECT * FROM product")
print(cursor.fetchall())
# Read available images
img_example = read_image("example.png")
img_kacamata = read_image("kacamata.jpeg")
img_kaos = read_image("kaosJMK48.jpg")
img_sepatu = read_image("sepatu.png")

# 36 products: mix of pakaian, teknologi, aksesoris, DIY for L/P/U
products = [
    # === WITH IMAGES (4) ===
    ("Sepatu Sneakers Pria", 350000, 20, img_sepatu, "Sepatu sneakers casual pria, nyaman untuk sehari-hari", "L"),
    ("Kacamata Hitam Unisex", 150000, 30, img_kacamata, "Kacamata hitam polarized, cocok untuk pria dan wanita", "U"),
    ("Kaos JMK48 Limited Edition", 120000, 50, img_kaos, "Kaos eksklusif JMK48 edisi terbatas, bahan cotton combed 30s", "U"),
    ("Tas Ransel Multifungsi", 275000, 15, img_example, "Tas ransel waterproof cocok untuk kuliah dan kerja", "U"),

    # === PAKAIAN PRIA (6) ===
    ("Kemeja Flannel Pria", 185000, 25, None, "Kemeja flannel kotak-kotak, bahan tebal dan hangat", "L"),
    ("Celana Jeans Slim Fit Pria", 250000, 20, None, "Celana jeans slim fit stretch, warna biru dongker", "L"),
    ("Jaket Hoodie Pria", 220000, 30, None, "Jaket hoodie fleece tebal, cocok untuk musim hujan", "L"),
    ("Polo Shirt Pria", 145000, 40, None, "Polo shirt katun pique, tersedia berbagai warna", "L"),
    ("Celana Chino Pria", 195000, 18, None, "Celana chino slim fit, bahan stretch nyaman", "L"),
    ("Kaos Polos Pria", 85000, 60, None, "Kaos polos cotton combed 24s, adem dan nyaman", "L"),

    # === PAKAIAN WANITA (6) ===
    ("Dress Casual Wanita", 275000, 15, None, "Dress casual selutut, bahan katun adem", "P"),
    ("Blouse Wanita Motif Bunga", 165000, 20, None, "Blouse wanita motif floral, bahan chiffon premium", "P"),
    ("Rok Plisket Wanita", 135000, 25, None, "Rok plisket panjang, bahan tidak mudah kusut", "P"),
    ("Cardigan Rajut Wanita", 195000, 18, None, "Cardigan rajut tebal, cocok untuk cuaca dingin", "P"),
    ("Celana Kulot Wanita", 175000, 22, None, "Celana kulot wide leg, bahan linen premium", "P"),
    ("Kaos Oversize Wanita", 95000, 35, None, "Kaos oversize crop, bahan cotton combed 30s", "P"),

    # === AKSESORIS (6) ===
    ("Topi Baseball Unisex", 75000, 40, None, "Topi baseball adjustable, bahan twill cotton", "U"),
    ("Gelang Kulit Pria", 55000, 50, None, "Gelang kulit asli handmade, desain vintage", "L"),
    ("Kalung Titanium Pria", 125000, 30, None, "Kalung titanium anti karat, desain minimalis", "L"),
    ("Anting Mutiara Wanita", 95000, 35, None, "Anting mutiara air tawar, setting perak 925", "P"),
    ("Jam Tangan Digital Unisex", 285000, 12, None, "Jam tangan digital waterproof, fitur stopwatch dan alarm", "U"),
    ("Dompet Kulit Pria", 165000, 20, None, "Dompet kulit sapi asli, model bifold slim", "L"),

    # === TEKNOLOGI (6) ===
    ("Earphone Bluetooth TWS", 199000, 25, None, "Earphone TWS bluetooth 5.0, bass boost, baterai 6 jam", "U"),
    ("Powerbank 10000mAh", 175000, 30, None, "Powerbank fast charging 20W, dual port USB-C dan USB-A", "U"),
    ("Stand HP Adjustable", 45000, 50, None, "Stand HP meja adjustable 360 derajat, bahan aluminium", "U"),
    ("Kabel USB-C Fast Charging", 35000, 80, None, "Kabel USB-C 1 meter, support fast charging 65W", "U"),
    ("Mouse Wireless Ergonomis", 125000, 20, None, "Mouse wireless 2.4GHz, desain ergonomis, silent click", "U"),
    ("Keyboard Mechanical Mini", 350000, 10, None, "Keyboard mechanical 60%, switch blue, RGB backlight", "U"),

    # === DIY & LAINNYA (8) ===
    ("Lem Tembak + Refill", 45000, 40, None, "Lem tembak mini + 10 batang refill lem", "U"),
    ("Set Obeng Presisi 25in1", 85000, 30, None, "Set obeng presisi untuk HP dan elektronik, 25 mata obeng", "U"),
    ("Gunting Serbaguna", 25000, 50, None, "Gunting stainless steel, tajam dan tahan lama", "U"),
    ("Lakban Bening 2 inch", 15000, 100, None, "Lakban bening 2 inch x 100 yard, lengket kuat", "U"),
    ("Cat Akrilik Set 12 Warna", 65000, 25, None, "Cat akrilik non-toxic 12 warna, cocok untuk kanvas dan craft", "U"),
    ("Kuas Lukis Set 10pcs", 35000, 30, None, "Set kuas lukis berbagai ukuran, bulu sintetis halus", "U"),
]

cursor.executemany(
    "INSERT INTO product (name, price, stock, image, description, gender) VALUES (?, ?, ?, ?, ?, ?)",
    products
)
conn.commit()

print(f"✅ Berhasil insert {len(products)} products!")
cursor.execute("SELECT COUNT(*) FROM product")
print(f"Total products in DB: {cursor.fetchone()[0]}")

conn.close()
