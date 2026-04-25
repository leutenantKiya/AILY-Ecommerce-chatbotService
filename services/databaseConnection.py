import base64
# import os
# import bcrypt
import sqlite3
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

class MongoDB:
    def __init__(self, Table):
        self.table = Table
# <<<<<<< Updated upstream
#         mongo_uri = os.getenv("MONGO_URI", "mongodb://localhost:27017")
#         mongo_db = os.getenv("MONGO_DB", "aily")
#         self.client = MongoClient(mongo_uri)
#         self.db = self.client[mongo_db]
# =======
        self.client = MongoClient("mongodb://Kiya:Jogja321@localhost:27017/?authSource=admin")
        self.db = self.client["aily"]
        self.collection = self.db[self.table]

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
        self.conn = sqlite3.connect("aily.db")
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
        """
        self.cursor.executescript(query)

    def registerUser(self, uname, pword, email, phone, add, role):
        try:
            self.cursor.execute("INSERT INTO user (username, password, email, phone, address, role) VALUES (?, ?, ?, ?, ?, ?)", (uname, pword, email, phone, add, role))
            self.conn.commit()
            return True
        except sqlite3.Error as error_db:
            print("Eror insert")
            return False

    def getTentangToko(self):
        self.cursor.execute("SELECT question, answer FROM tentangToko")
        return self.cursor.fetchall()
 
    def findUser(self, username):
        self.cursor.execute("SELECT * FROM user WHERE username = ?", (username,))
        return self.cursor.fetchone()

    def findUserByPassword(self, hashed_password):
        self.cursor.execute("SELECT * FROM user WHERE password = ?", (hashed_password,))
        return self.cursor.fetchone()

    def update(self, table, colum_name, data_new, user_id):
        self.cursor.execute(f"UPDATE {table} SET {colum_name} = ? WHERE id = ?", (data_new, user_id))
        self.conn.commit()

    def delete(self, query):
        self.cursor.execute(f"DELETE FROM {self.table} WHERE {query}")
        self.conn.commit()
        
    def getHelp(self):
        self.cursor.execute(f"SELECT question, answer FROM help")
        return self.cursor.fetchall()

class ProductDB:
    def __init__(self):
        self.conn = sqlite3.connect("aily.db")
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
        self.cursor.execute(f"SELECT * FROM product WHERE name LIKE '%{name}%' and (gender = ? OR gender = 'U') ",(gender,))
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
                WHERE name LIKE ?
                  AND (gender = ? OR gender = 'U')
                ORDER BY id ASC
                LIMIT ?
                """,
                (like_keyword, gender, limit)
            )
        else:
            self.cursor.execute(
                """
                SELECT * FROM product
                WHERE name LIKE ?
                ORDER BY id ASC
                LIMIT ?
                """,
                (like_keyword, limit)
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
        self.conn = sqlite3.connect("aily.db")
        self.cursor = self.conn.cursor()

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
