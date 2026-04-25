import base64
import os
import sqlite3
from datetime import datetime
from pathlib import Path
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

DB_PATH = Path(__file__).resolve().parent.parent / "aily.db"


def connect_sqlite():
    conn = sqlite3.connect(str(DB_PATH))
    conn.execute("PRAGMA foreign_keys = ON")
    return conn

class MongoDB:
    def __init__(self, Table):
        mongo_uri = os.getenv("MONGO_URI", "mongodb://localhost:27017")
        mongo_db = os.getenv("MONGO_DB", "aily")
        self.client = MongoClient(mongo_uri)
        self.db = self.client[mongo_db]
        self.collection = self.db[Table]

    def insert(self, data):
        self.collection.insert_one(data)

    def find(self, query):
        return self.collection.find(query)

    def find_one(self, query):
        return self.collection.find_one(query)

    def update(self, query, data):
        self.collection.update_one(query, {"$set": data})

    def push(self, query, data):
        self.collection.update_one(query, {"$push": data})

    def delete(self, query):
        self.collection.delete_one(query)

class SQLite:
    def __init__(self):
        self.conn = connect_sqlite()
        self.cursor = self.conn.cursor()

    def createTable(self):
        query = """
            CREATE TABLE IF NOT EXISTS user(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                role TEXT NOT NULL,
                gender TEXT DEFAULT 'L'
            );

            CREATE TABLE IF NOT EXISTS cart(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userId INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('Belum Checkout', 'Checkout', 'Dalam Pengiriman', 'Selesai')),
                FOREIGN KEY (userId) REFERENCES user(id)
            );

            CREATE TABLE IF NOT EXISTS product(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price INTEGER NOT NULL,
                stock INTEGER NOT NULL,
                image MEDIUMBLOB,
                description TEXT NOT NULL,
                gender TEXT DEFAULT 'U'
            );

            CREATE TABLE IF NOT EXISTS cart_item (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cartId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                jumlah_barang INTEGER NOT NULL, -- qty
                FOREIGN KEY (cartId) REFERENCES cart(id),
                FOREIGN KEY (productId) REFERENCES product(id)
            );

            CREATE TABLE IF NOT EXISTS tentangToko(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT(50) NOT NULL,
                answer TEXT(100) NOT NULL
            );
            
            CREATE TABLE IF NOT EXISTS help(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT(50) NOT NULL,
                answer TEXT(100) NOT NULL
            );

            CREATE TABLE IF NOT EXISTS orders(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userId INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('Diproses', 'Dalam Pengiriman', 'Selesai', 'Dibatalkan')),
                subtotal INTEGER NOT NULL DEFAULT 0,
                shipping_cost INTEGER NOT NULL DEFAULT 0,
                discount INTEGER NOT NULL DEFAULT 0,
                total INTEGER NOT NULL DEFAULT 0,
                created_at TEXT NOT NULL,
                updated_at TEXT NOT NULL,
                FOREIGN KEY (userId) REFERENCES user(id)
            );

            CREATE TABLE IF NOT EXISTS order_items(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                orderId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                quantity INTEGER NOT NULL,
                price_snapshot INTEGER NOT NULL,
                product_name_snapshot TEXT NOT NULL,
                FOREIGN KEY (orderId) REFERENCES orders(id),
                FOREIGN KEY (productId) REFERENCES product(id)
            );
        """
        self.cursor.executescript(query)
        self.conn.commit()
        self.seedDefaultData()

    def seedDefaultData(self):
        self.cursor.execute("SELECT COUNT(*) FROM tentangToko")
        if self.cursor.fetchone()[0] == 0:
            self.cursor.executemany(
                "INSERT INTO tentangToko (question, answer) VALUES (?, ?)",
                [
                    ("Jam Buka", "10.00 - 21.00"),
                    ("pemilik", "Antonius Kiya Ananda Derron"),
                    ("deskripsi", "Kami adalah toko yang berfokus pada pembelian barang DIY, teknologi, dan pakaian baik untuk pria maupun wanita"),
                    ("alamat", "Jalan Dr. Wahidin No.1 - 5, Kota Yogyakarta, DIY"),
                    ("Visi dan Misi", "Menjadi olshop terpercaya dan termurah di DIY yang akan mengalahkan Mr.DIY"),
                ]
            )

        self.cursor.execute("SELECT COUNT(*) FROM help")
        if self.cursor.fetchone()[0] == 0:
            self.cursor.executemany(
                "INSERT INTO help (question, answer) VALUES (?, ?)",
                [
                    ("retur", "Barang bisa diretur maksimal 7 hari setelah diterima jika rusak atau tidak sesuai."),
                    ("refund", "Refund diproses setelah barang retur diterima dan diverifikasi oleh toko."),
                    ("garansi", "Garansi mengikuti kebijakan masing-masing produk dan kondisi barang."),
                    ("ongkir", "Ongkir dihitung saat checkout sesuai alamat dan metode pengiriman."),
                    ("pembayaran", "Pembayaran dapat dilakukan melalui transfer bank, e-wallet, atau metode yang tersedia saat checkout."),
                    ("promo", "Promo yang tersedia akan diinformasikan melalui chat atau halaman produk."),
                    ("cod", "COD tersedia hanya untuk wilayah dan produk tertentu."),
                ]
            )
        self.conn.commit()

    def registerUser(self, uname, pword, email, phone, add, role, gender="L"):
        try:
            self.cursor.execute(
                "INSERT INTO user (username, password, email, phone, address, role, gender) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (uname, pword, email, phone, add, role, gender)
            )
            self.conn.commit()
            return True
        except sqlite3.Error as error_db:
            print("Eror insert")
            return False

    def getTentangToko(self):
        self.cursor.execute("SELECT question, answer FROM tentangToko")
        return self.cursor.fetchall()

    def getTentangTokoWithId(self):
        self.cursor.execute("SELECT id, question, answer FROM tentangToko ORDER BY id ASC")
        return self.cursor.fetchall()
 
    def findUser(self, username):
        self.cursor.execute("SELECT * FROM user WHERE username = ?", (username,))
        return self.cursor.fetchone()

    def findEmail(self, email):
        self.cursor.execute("SELECT * FROM user WHERE email = ?", (email,))
        return self.cursor.fetchone()   

    def findUserById(self, user_id):
        self.cursor.execute("SELECT * FROM user WHERE id = ?", (user_id,))
        return self.cursor.fetchone()

    def findUserByPassword(self, hashed_password):
        self.cursor.execute("SELECT * FROM user WHERE password = ?", (hashed_password,))
        return self.cursor.fetchone()

    def resolveUser(self, user_token):
        if user_token is None:
            return None

        token = str(user_token).strip()
        if token.isdigit():
            user = self.findUserById(int(token))
            if user is not None:
                return user

        return self.findUserByPassword(token)

    def update(self, table, colum_name, data_new, user_id):
        allowed_columns = {
            "user": {"username", "email", "phone", "address", "gender"}
        }
        if table not in allowed_columns or colum_name not in allowed_columns[table]:
            return False
        self.cursor.execute(f"UPDATE {table} SET {colum_name} = ? WHERE id = ?", (data_new, user_id))
        self.conn.commit()
        return True

    def delete(self, query):
        self.cursor.execute(f"DELETE FROM {self.table} WHERE {query}")
        self.conn.commit()
        
    def getHelp(self):
        self.cursor.execute(f"SELECT question, answer FROM help")
        return self.cursor.fetchall()

    def searchHelp(self, keyword):
        normalized = f"%{str(keyword).lower()}%"
        self.cursor.execute(
            """
            SELECT question, answer FROM help
            WHERE lower(question) LIKE ? OR lower(answer) LIKE ?
            ORDER BY id ASC
            """,
            (normalized, normalized)
        )
        result = self.cursor.fetchall()
        if result:
            return result
        return self.getHelp()

    def addTentangToko(self, question, answer):
        self.cursor.execute(
            "INSERT INTO tentangToko (question, answer) VALUES (?, ?)",
            (question, answer)
        )
        self.conn.commit()
        return self.cursor.lastrowid

    def updateTentangToko(self, info_id, question, answer):
        self.cursor.execute(
            "UPDATE tentangToko SET question = ?, answer = ? WHERE id = ?",
            (question, answer, info_id)
        )
        self.conn.commit()
        return self.cursor.rowcount > 0

    def deleteTentangToko(self, info_id):
        self.cursor.execute("DELETE FROM tentangToko WHERE id = ?", (info_id,))
        self.conn.commit()
        return self.cursor.rowcount > 0

class ProductDB:
    def __init__(self):
        self.conn = connect_sqlite()
        self.cursor = self.conn.cursor()

    def _serialize_product_row(self, row):
        if row is None:
            return None

        row_list = list(row)
        if row_list[4] is not None and isinstance(row_list[4], bytes):
            row_list[4] = base64.b64encode(row_list[4]).decode("utf-8")

        return {
            "id": row_list[0],
            "name": row_list[1],
            "price": row_list[2],
            "stock": row_list[3],
            "image": row_list[4],
            "description": row_list[5],
            "gender": row_list[6],
        }

    def searchBarang(self,name, gender):
        like_name = f"%{name}%"
        if gender in ("L", "P"):
            self.cursor.execute(
                """
                SELECT * FROM product
                WHERE (name LIKE ? OR description LIKE ?)
                  and (gender = ? OR gender = 'U')
                """,
                (like_name, like_name, gender)
            )
        else:
            self.cursor.execute(
                "SELECT * FROM product WHERE name LIKE ? OR description LIKE ?",
                (like_name, like_name)
            )
        rows = self.cursor.fetchall()
        result = []
        for row in rows:
            row_list = list(row)
            # row[4] is the image column — encode binary to base64 string
            if row_list[4] is not None and isinstance(row_list[4], bytes):
                row_list[4] = base64.b64encode(row_list[4]).decode("utf-8")
            result.append(row_list)
        return result

    def getProductById(self, product_id):
        self.cursor.execute("SELECT * FROM product WHERE id = ?", (product_id,))
        row = self.cursor.fetchone()
        return self._serialize_product_row(row)

    def findProductsByKeyword(self, keyword, gender=None, limit=5):
        like_keyword = f"%{keyword.strip()}%"
        if gender in ("L", "P"):
            self.cursor.execute(
                """
                SELECT * FROM product
                WHERE (name LIKE ? OR description LIKE ?)
                  AND (gender = ? OR gender = 'U')
                ORDER BY id ASC
                LIMIT ?
                """,
                (like_keyword, like_keyword, gender, limit)
            )
        else:
            self.cursor.execute(
                """
                SELECT * FROM product
                WHERE name LIKE ? OR description LIKE ?
                ORDER BY id ASC
                LIMIT ?
                """,
                (like_keyword, like_keyword, limit)
            )

        return [self._serialize_product_row(row) for row in self.cursor.fetchall()]

    def addProduct(self, name, price, stock, image, description, gender):
        try:
            self.cursor.execute(
                "INSERT INTO product (name, price, stock, image, description, gender) VALUES (?, ?, ?, ?, ?, ?)",
                (name, price, stock, image, description, gender)
            )
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print("Error insert product:", e)
            return False

    def updateProduct(self, product_id, name, price, stock, image, description, gender):
        try:
            self.cursor.execute(
                "UPDATE product SET name=?, price=?, stock=?, image=?, description=?, gender=? WHERE id=?",
                (name, price, stock, image, description, gender, product_id)
            )
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print("Error update product:", e)
            return False

    def deleteProduct(self, product_id):
        try:
            self.cursor.execute("DELETE FROM product WHERE id=?", (product_id,))
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print("Error delete product:", e)
            return False

    def getAllProducts(self):
        self.cursor.execute("SELECT * FROM product")
        return self.cursor.fetchall()


class CartDB:
    def __init__(self):
        self.conn = connect_sqlite()
        self.cursor = self.conn.cursor()
    
    def getAllCart(self):
        self.cursor.execute("""
            SELECT
                c.id                            AS ID,
                u.username                      AS Pembeli,
                p.name                          AS Produk,
                (p.price * ci.jumlah_barang)    AS Total,
                c.status                        AS StatusPembayaran
            FROM cart c
            JOIN user u         ON c.userId     = u.id
            LEFT JOIN cart_item ci  ON ci.cartId    = c.id
            LEFT JOIN product p     ON ci.productId = p.id
        """)
        return self.cursor.fetchall()
    
    def _get_or_create_active_cart_id(self, user_id):
        self.cursor.execute(
            "SELECT id FROM cart WHERE userId = ? AND status = 'Belum Checkout' ORDER BY id DESC LIMIT 1",
            (user_id,)
        )
        row = self.cursor.fetchone()
        if row is not None:
            return row[0]

        self.cursor.execute(
            "INSERT INTO cart (userId, status) VALUES (?, 'Belum Checkout')",
            (user_id,)
        )
        self.conn.commit()
        return self.cursor.lastrowid

    def getCartItems(self, user_id):
        cart_id = self._get_or_create_active_cart_id(user_id)
        self.cursor.execute(
            """
            SELECT
                ci.productId,
                ci.jumlah_barang,
                p.name,
                p.price,
                p.stock,
                p.image,
                p.description,
                p.gender
            FROM cart_item ci
            JOIN product p ON p.id = ci.productId
            WHERE ci.cartId = ?
            ORDER BY ci.id ASC
            """,
            (cart_id,)
        )

        items = []
        for row in self.cursor.fetchall():
            image = row[5]
            if image is not None and isinstance(image, bytes):
                image = base64.b64encode(image).decode("utf-8")

            items.append({
                "product_id": row[0],
                "quantity": row[1],
                "name": row[2],
                "price": row[3],
                "stock": row[4],
                "image": image,
                "description": row[6],
                "gender": row[7],
            })

        return items

    def addToCart(self, user_id, product_id, quantity):
        self.cursor.execute("SELECT stock FROM product WHERE id = ?", (product_id,))
        product = self.cursor.fetchone()
        if product is None:
            return False, "Produk tidak ditemukan."

        stock = product[0]
        if quantity <= 0:
            return False, "Jumlah produk harus lebih dari 0."

        cart_id = self._get_or_create_active_cart_id(user_id)
        self.cursor.execute(
            "SELECT id, jumlah_barang FROM cart_item WHERE cartId = ? AND productId = ?",
            (cart_id, product_id)
        )
        existing = self.cursor.fetchone()

        new_quantity = quantity
        if existing is not None:
            new_quantity = existing[1] + quantity

        if new_quantity > stock:
            return False, f"Stok produk hanya tersedia {stock}."

        if existing is not None:
            self.cursor.execute(
                "UPDATE cart_item SET jumlah_barang = ? WHERE id = ?",
                (new_quantity, existing[0])
            )
        else:
            self.cursor.execute(
                "INSERT INTO cart_item (cartId, productId, jumlah_barang) VALUES (?, ?, ?)",
                (cart_id, product_id, quantity)
            )

        self.conn.commit()
        return True, "Produk berhasil ditambahkan ke keranjang."

    def updateCartItem(self, user_id, product_id, quantity):
        cart_id = self._get_or_create_active_cart_id(user_id)

        if quantity <= 0:
            self.cursor.execute(
                "DELETE FROM cart_item WHERE cartId = ? AND productId = ?",
                (cart_id, product_id)
            )
            self.conn.commit()
            return True, "Produk dihapus dari keranjang."

        self.cursor.execute("SELECT stock FROM product WHERE id = ?", (product_id,))
        product = self.cursor.fetchone()
        if product is None:
            return False, "Produk tidak ditemukan."

        stock = product[0]
        if quantity > stock:
            return False, f"Stok produk hanya tersedia {stock}."

        self.cursor.execute(
            "SELECT id FROM cart_item WHERE cartId = ? AND productId = ?",
            (cart_id, product_id)
        )
        existing = self.cursor.fetchone()
        if existing is None:
            return False, "Produk belum ada di keranjang."

        self.cursor.execute(
            "UPDATE cart_item SET jumlah_barang = ? WHERE id = ?",
            (quantity, existing[0])
        )
        self.conn.commit()
        return True, "Jumlah produk di keranjang berhasil diperbarui."

    def removeCartItem(self, user_id, product_id):
        cart_id = self._get_or_create_active_cart_id(user_id)
        self.cursor.execute(
            "DELETE FROM cart_item WHERE cartId = ? AND productId = ?",
            (cart_id, product_id)
        )
        self.conn.commit()
        return True

    def clearCart(self, user_id):
        cart_id = self._get_or_create_active_cart_id(user_id)
        self.cursor.execute("DELETE FROM cart_item WHERE cartId = ?", (cart_id,))
        self.conn.commit()
        return True

    def checkoutCart(self, user_id, shipping_cost=15000, discount=0):
        cart_id = self._get_or_create_active_cart_id(user_id)
        self.cursor.execute(
            """
            SELECT
                ci.productId,
                ci.jumlah_barang,
                p.name,
                p.price,
                p.stock
            FROM cart_item ci
            JOIN product p ON p.id = ci.productId
            WHERE ci.cartId = ?
            ORDER BY ci.id ASC
            """,
            (cart_id,)
        )
        items = self.cursor.fetchall()
        if not items:
            return False, "Keranjang kamu masih kosong.", None

        for item in items:
            if item[1] > item[4]:
                return False, f"Stok {item[2]} hanya tersedia {item[4]}.", None

        subtotal = sum(item[1] * item[3] for item in items)
        total = subtotal + shipping_cost - discount
        now = datetime.now().isoformat(timespec="seconds")

        try:
            self.cursor.execute("BEGIN")
            self.cursor.execute(
                """
                INSERT INTO orders (userId, status, subtotal, shipping_cost, discount, total, created_at, updated_at)
                VALUES (?, 'Diproses', ?, ?, ?, ?, ?, ?)
                """,
                (user_id, subtotal, shipping_cost, discount, total, now, now)
            )
            order_id = self.cursor.lastrowid

            for product_id, quantity, name, price, stock in items:
                self.cursor.execute(
                    """
                    INSERT INTO order_items (orderId, productId, quantity, price_snapshot, product_name_snapshot)
                    VALUES (?, ?, ?, ?, ?)
                    """,
                    (order_id, product_id, quantity, price, name)
                )
                self.cursor.execute(
                    "UPDATE product SET stock = stock - ? WHERE id = ?",
                    (quantity, product_id)
                )

            self.cursor.execute("UPDATE cart SET status = 'Checkout' WHERE id = ?", (cart_id,))
            self.cursor.execute(
                "INSERT INTO cart (userId, status) VALUES (?, 'Belum Checkout')",
                (user_id,)
            )
            self.conn.commit()
            return True, "Checkout berhasil. Pesanan kamu sudah dibuat.", order_id
        except sqlite3.Error as error_db:
            self.conn.rollback()
            print("Error checkout cart:", error_db)
            return False, "Checkout gagal diproses.", None


class OrderDB:
    NON_CANCELABLE_STATUSES = {"Dalam Pengiriman", "Selesai", "Dibatalkan"}
    VALID_STATUSES = {"Diproses", "Dalam Pengiriman", "Selesai", "Dibatalkan"}

    def __init__(self):
        self.conn = connect_sqlite()
        self.cursor = self.conn.cursor()

    def _order_code(self, order_id):
        return f"TRX-{int(order_id):06d}"

    def _normalize_order_id(self, order_id):
        if order_id is None:
            return None
        digits = "".join(ch for ch in str(order_id) if ch.isdigit())
        if not digits:
            return None
        return int(digits)

    def _serialize_item(self, row):
        image = row[9]
        if image is not None and isinstance(image, bytes):
            image = base64.b64encode(image).decode("utf-8")

        product_name = row[6] if row[6] is not None else row[5]
        price = row[7] if row[7] is not None else row[4]

        return {
            "id": row[0],
            "order_id": row[1],
            "product_id": row[2],
            "quantity": row[3],
            "price": row[4],
            "product_name": row[5],
            "subtotal": row[3] * row[4],
            "product": {
                "id": row[2],
                "name": product_name,
                "price": price,
                "stock": row[8] if row[8] is not None else 0,
                "image": image,
                "description": row[10] if row[10] is not None else "",
                "gender": row[11] if row[11] is not None else "U",
            }
        }

    def _get_order_items(self, order_id):
        self.cursor.execute(
            """
            SELECT
                oi.id,
                oi.orderId,
                oi.productId,
                oi.quantity,
                oi.price_snapshot,
                oi.product_name_snapshot,
                p.name,
                p.price,
                p.stock,
                p.image,
                p.description,
                p.gender
            FROM order_items oi
            LEFT JOIN product p ON p.id = oi.productId
            WHERE oi.orderId = ?
            ORDER BY oi.id ASC
            """,
            (order_id,)
        )
        return [self._serialize_item(row) for row in self.cursor.fetchall()]

    def _serialize_order(self, row, include_items=True):
        if row is None:
            return None
        order = {
            "id": row[0],
            "order_code": self._order_code(row[0]),
            "user_id": row[1],
            "username": row[9] if len(row) > 9 else None,
            "status": row[2],
            "subtotal": row[3],
            "shipping_cost": row[4],
            "discount": row[5],
            "total": row[6],
            "created_at": row[7],
            "updated_at": row[8],
            "can_cancel": row[2] not in self.NON_CANCELABLE_STATUSES,
        }
        if include_items:
            order["items"] = self._get_order_items(row[0])
        return order

    def getOrderById(self, order_id, user_id=None):
        normalized_id = self._normalize_order_id(order_id)
        if normalized_id is None:
            return None

        if user_id is None:
            self.cursor.execute(
                """
                SELECT o.id, o.userId, o.status, o.subtotal, o.shipping_cost, o.discount, o.total, o.created_at, o.updated_at, u.username
                FROM orders o
                JOIN user u ON u.id = o.userId
                WHERE o.id = ?
                """,
                (normalized_id,)
            )
        else:
            self.cursor.execute(
                """
                SELECT o.id, o.userId, o.status, o.subtotal, o.shipping_cost, o.discount, o.total, o.created_at, o.updated_at, u.username
                FROM orders o
                JOIN user u ON u.id = o.userId
                WHERE o.id = ? AND o.userId = ?
                """,
                (normalized_id, user_id)
            )
        return self._serialize_order(self.cursor.fetchone())

    def getUserOrders(self, user_id):
        self.cursor.execute(
            """
            SELECT o.id, o.userId, o.status, o.subtotal, o.shipping_cost, o.discount, o.total, o.created_at, o.updated_at, u.username
            FROM orders o
            JOIN user u ON u.id = o.userId
            WHERE o.userId = ?
            ORDER BY o.id DESC
            """,
            (user_id,)
        )
        return [self._serialize_order(row) for row in self.cursor.fetchall()]

    def getAllOrders(self):
        self.cursor.execute(
            """
            SELECT o.id, o.userId, o.status, o.subtotal, o.shipping_cost, o.discount, o.total, o.created_at, o.updated_at, u.username
            FROM orders o
            JOIN user u ON u.id = o.userId
            ORDER BY o.id DESC
            """
        )
        return [self._serialize_order(row) for row in self.cursor.fetchall()]

    def getLatestCancelableOrder(self, user_id):
        self.cursor.execute(
            """
            SELECT o.id, o.userId, o.status, o.subtotal, o.shipping_cost, o.discount, o.total, o.created_at, o.updated_at, u.username
            FROM orders o
            JOIN user u ON u.id = o.userId
            WHERE o.userId = ? AND o.status NOT IN ('Dalam Pengiriman', 'Selesai', 'Dibatalkan')
            ORDER BY o.id DESC
            LIMIT 1
            """,
            (user_id,)
        )
        return self._serialize_order(self.cursor.fetchone())

    def cancelOrder(self, user_id, order_id=None):
        order = self.getOrderById(order_id, user_id) if order_id is not None else self.getLatestCancelableOrder(user_id)
        if order is None:
            return False, "Pesanan tidak ditemukan atau tidak bisa dibatalkan.", None

        if order["status"] in self.NON_CANCELABLE_STATUSES:
            return False, "Pesanan tidak bisa dibatalkan karena sudah dalam pengantaran atau selesai.", order

        now = datetime.now().isoformat(timespec="seconds")
        try:
            self.cursor.execute("BEGIN")
            self.cursor.execute(
                "SELECT productId, quantity FROM order_items WHERE orderId = ?",
                (order["id"],)
            )
            for product_id, quantity in self.cursor.fetchall():
                self.cursor.execute(
                    "UPDATE product SET stock = stock + ? WHERE id = ?",
                    (quantity, product_id)
                )

            self.cursor.execute(
                "UPDATE orders SET status = 'Dibatalkan', updated_at = ? WHERE id = ?",
                (now, order["id"])
            )
            self.conn.commit()
            canceled_order = self.getOrderById(order["id"], user_id)
            return True, "Pesanan berhasil dibatalkan.", canceled_order
        except sqlite3.Error as error_db:
            self.conn.rollback()
            print("Error cancel order:", error_db)
            return False, "Gagal membatalkan pesanan.", order

    def updateOrderStatus(self, order_id, status):
        if status not in self.VALID_STATUSES:
            return False, "Status pesanan tidak valid.", None

        order = self.getOrderById(order_id)
        if order is None:
            return False, "Pesanan tidak ditemukan.", None

        if order["status"] == "Dibatalkan" and status != "Dibatalkan":
            return False, "Pesanan yang sudah dibatalkan tidak bisa diaktifkan kembali.", order

        now = datetime.now().isoformat(timespec="seconds")
        self.cursor.execute(
            "UPDATE orders SET status = ?, updated_at = ? WHERE id = ?",
            (status, now, order["id"])
        )
        self.conn.commit()
        return True, "Status pesanan berhasil diperbarui.", self.getOrderById(order["id"])
