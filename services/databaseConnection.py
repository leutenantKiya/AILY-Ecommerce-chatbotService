import bcrypt
import sqlite3
from pymongo import MongoClient

class MongoDB:
    def __init__(self, Table):
        self.table = Table
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
                image TEXT,
                description TEXT NOT NULL,
                category TEXT NOT NULL,
                gender TEXT DEFAULT 'U',
                warna TEXT
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
            self.cursor.execute(f"INSERT INTO user (username, password, email, phone, address, role) VALUES (?, ?, ?, ?, ?, ?)", (uname, pword, email, phone, add, role))
            self.conn.commit()
            return True
        except sqlite3.Error as error_db:
            print("Eror insert")
            return False

    def getTentangToko(self):
        self.cursor.execute(f"SELECT question, answer FROM tentangToko")
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

    def searchBarang(self, name, gender):
        self.cursor.execute(f"SELECT * FROM product WHERE name LIKE '%{name}%' and (gender = ? OR gender = 'U')",(gender,))
        # print(self.cursor.fetchall())
        return self.cursor.fetchall()

    def addProduct(self, name, price, stock, image, description, category, gender, warna):
        try:
            self.cursor.execute(
                "INSERT INTO product (name, price, stock, image, description, category, gender, warna) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                (name, price, stock, image, description, category, gender, warna)
            )
            self.conn.commit()
            return True
        except sqlite3.Error as e:
            print("Error insert product:", e)
            return False

    def updateProduct(self, product_id, name, price, stock, image, description, category, gender, warna):
        try:
            self.cursor.execute(
                "UPDATE product SET name=?, price=?, stock=?, image=?, description=?, category=?, gender=?, warna=? WHERE id=?",
                (name, price, stock, image, description, category, gender, warna, product_id)
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
