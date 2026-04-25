PRAGMA foreign_keys=OFF;
BEGIN TRANSACTION;
CREATE TABLE user(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                password TEXT NOT NULL,
                email TEXT NOT NULL,
                phone TEXT NOT NULL,
                address TEXT NOT NULL,
                role TEXT NOT NULL
            , gender BOOLEAN DEFAULT 1);
INSERT INTO user VALUES(1,'Anton','$2b$12$3jlPu86QG1kil0SbAzQLjuxsaXYvf5sXqqOR49E.rW0kcbPthIvc.','antoniuskiya57@gmail.com','0291012921','ajskdjasjdak','user','L');
INSERT INTO user VALUES(3,'Admin','$2b$12$VPwn1K8QtHz9bHAJ7R1nSeayvM7BsPGJJAgTbXrk83aZaAnJaYQ36','Ailystore@gmail.com','08386854493','Jl. Dr. Wahidin No. 1-5, Kota Yogyakarta, DIY','ADMIN','L');
INSERT INTO user VALUES(4,'kiya','$2b$12$WfYIlXOnScUocD5ksrOzqeK2EziFBArnUFzUCw67Gab7MaOGs/0XO','kiya@gmail.com','083144671780','Di Jakarta','user','L');
INSERT INTO user VALUES(5,'kevin','$2b$12$bBIzAGusanfFdLMJXBVNnesIS1LIETZhhZ53rarslcC0cKvLjI1xi','','','','user',1);
INSERT INTO user VALUES(6,'kevinA','$2b$12$Z7UxYUEDND23AIDo6wlJi.MU6PAuOiCjnA/ILs.EqpZ7gt4rJSrdi','','','','user',1);
CREATE TABLE cart(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                userId INTEGER NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('Belum Checkout', 'Checkout', 'Dalam Pengiriman', 'Selesai')),
                FOREIGN KEY (userId) REFERENCES user(id)
            );
CREATE TABLE product(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                price INTEGER NOT NULL,
                stock INTEGER NOT NULL,
                image TEXT,
                description TEXT NOT NULL
            , gender TEXT DEFAULT 'U', warna TEXT DEFAULT '');
/****** CORRUPTION ERROR *******/
CREATE TABLE cart_item (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                cartId INTEGER NOT NULL,
                productId INTEGER NOT NULL,
                jumlah_barang INTEGER NOT NULL, -- qty
                FOREIGN KEY (cartId) REFERENCES cart(id),
                FOREIGN KEY (productId) REFERENCES product(id)
            );
CREATE TABLE tentangToko(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT(50) NOT NULL,
                answer TEXT(100) NOT NULL
            );
INSERT INTO tentangToko VALUES(1,'Jam Buka','10.00 - 21.00');
INSERT INTO tentangToko VALUES(2,'pemilik','Antonius Kiya Ananda Derron');
INSERT INTO tentangToko VALUES(3,'deskripsi','Kami adalah toko yang berfokus pada pembelian barang DIY, teknologi, dan pakaian baik untuk pria maupun wanita');
INSERT INTO tentangToko VALUES(4,'alamat','Jalan Dr. Wahidin No.1 - 5, Kota Yogyakarta, DIY');
INSERT INTO tentangToko VALUES(5,'Visi dan Misi','Menjadi olshop terpercaya dan termurah di DIY yang akan mengalahkan Mr.DIY');
ANALYZE sqlite_schema;
INSERT INTO sqlite_stat1 VALUES('tentangToko',NULL,'5');
INSERT INTO sqlite_stat1 VALUES('product',NULL,'36');
INSERT INTO sqlite_stat1 VALUES('user',NULL,'3');
ANALYZE sqlite_schema;
CREATE TABLE help(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                question TEXT(50) NOT NULL,
                answer TEXT(100) NOT NULL
            );
PRAGMA writable_schema=ON;
CREATE TABLE IF NOT EXISTS sqlite_sequence(name,seq);
DELETE FROM sqlite_sequence;
INSERT INTO sqlite_sequence VALUES('user',6);
INSERT INTO sqlite_sequence VALUES('tentangToko',5);
INSERT INTO sqlite_sequence VALUES('product',38);
PRAGMA writable_schema=OFF;
COMMIT;
