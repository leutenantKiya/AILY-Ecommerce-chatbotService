import os
import bcrypt
import sqlite3
from pymongo import MongoClient
from dotenv import load_dotenv

load_dotenv()

class MongoDB:
    def __init__(self, Table):
        self.table = Table
<<<<<<< Updated upstream
        mongo_uri = os.getenv("MONGO_URI", "mongodb://localhost:27017")
        mongo_db = os.getenv("MONGO_DB", "aily")
        self.client = MongoClient(mongo_uri)
        self.db = self.client[mongo_db]
=======
        self.client = MongoClient("mongodb://Kiya:Jogja321@localhost:27017/?authSource=admin")
        self.db = self.client["aily"]
>>>>>>> Stashed changes
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

class ProductDB:
    def __init__(self):
        self.conn = sqlite3.connect("aily.db")
        self.cursor = self.conn.cursor()

<<<<<<< Updated upstream
    def searchBarang(self, name, gender):
        # Cari berdasarkan nama ATAU kategori
        self.cursor.execute(
            "SELECT * FROM product WHERE (name LIKE ? OR category LIKE ?) AND (gender = ? OR gender = 'U')", 
            (f"%{name}%", f"%{name}%", gender)
        )
        return self.cursor.fetchall()

    def searchByCategory(self, category, gender):
        # Mapping alias informal → kategori di database
        CATEGORY_ALIAS = {
            "baju": ["kaos", "kemeja", "gamis", "dress"],
            "pakaian": ["kaos", "kemeja", "gamis", "dress", "jaket", "hoodie", "sweater", "blazer"],
            "celana": ["celana", "jeans"],
            "sepatu": ["sepatu", "sandal"],
            "aksesoris": ["aksesoris", "topi"],
            "tas": ["tas"],
        }
        
        categories = CATEGORY_ALIAS.get(category.lower(), [category.lower()])
        placeholders = ",".join(["?" for _ in categories])
        query = f"SELECT * FROM product WHERE category IN ({placeholders}) AND (gender = ? OR gender = 'U')"
        params = categories + [gender]
        self.cursor.execute(query, params)
        return self.cursor.fetchall()
=======
    def searchBarang(self,role, name, gender):
        import base64
        if name.strip() == "":
            if role != "admin":
                return []
        self.cursor.execute(f"SELECT * FROM product WHERE name LIKE '%{name}%' and (gender = ? OR gender = 'U')",(gender,))
        rows = self.cursor.fetchall()
        result = []
        for row in rows:
            row_list = list(row)
            # row[4] is the image column — encode binary to base64 string
            if row_list[4] is not None and isinstance(row_list[4], bytes):
                row_list[4] = base64.b64encode(row_list[4]).decode("utf-8")
            result.append(row_list)
        return result
>>>>>>> Stashed changes

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
