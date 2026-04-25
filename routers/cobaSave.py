import sqlite3
import sys, os
import sqlite3
from PIL import Image
import io
sys.path.append(os.path.abspath(os.path.join(os.path.dirname(__file__), '..')))

# from services.databaseConnection import ProductDB

# db = ProductDB()
image_path = os.path.join(os.path.dirname(__file__), '..', 'kacamata.jpeg')
with open(image_path, "rb") as f:
    image = f.read()
    print(image)

image = Image.open(io.BytesIO(image))
image.show()
# db.addProduct(
#     name="example",
#     price=1000,
#     stock=100,
#     image=image,
#     description="example",
#     gender="U"
# )


# db_path = os.path.join(os.path.dirname(os.path.dirname(__file__)), 'aily.db')
# conn = sqlite3.connect(db_path)
# cursor = conn.cursor()
# cursor.execute("UPDATE product SET image = ? WHERE name LIKE ?", (image, "%Kaos%"))
# cursor.execute("SELECT image from product where id = 32")
# conn.commit()
# row = cursor.fetchone()
# conn.close()

# if row: 
    # print(row[0])
    # binary_data = row[0] 