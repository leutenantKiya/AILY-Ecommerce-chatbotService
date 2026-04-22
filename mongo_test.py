from pymongo import MongoClient

uid = "$2b$12$WfYIlXOnScUocD5ksrOzqeK2EziFBArnUFzUCw67Gab7MaOGs/0XO"
client = MongoClient("mongodb://Kiya:Jogja321@localhost:27017/?authSource=admin")
doc = client["aily"]["chatUserLog"].find_one({"user_id": uid})

if doc is not None:
    import json
    chats = doc.get("chats", [])
    print(json.dumps(chats[-5:], default=lambda x: str(x), indent=2))
