# conversation handles user's question → NLP → response
from fastapi import APIRouter, Body
from pydantic import BaseModel
from typing import Any, List
from utils.response import Response
from NLP.NLPHandler import process as nlp_process
from services.databaseConnection import SQLite, MongoDB, ProductDB



# brainstorming baru plan:
# 1. when login -> check the validation for user existence. if exist go to 2
# 2. java fx request for the the chat user's do have in the mongodb
# 3. if user ask -> save to db -> return the value so java fx can display -> check the intent -> save the bot response -> return the response
# 4. everytime java got a return message -> immediately append the message to child (i think this will do?)
router = APIRouter()

class ChatMessage(BaseModel):
    id: str
    message: str

class ChatSaveRequest(BaseModel):
    user_id: str
    username: str
    role: str
    message: Any

USER_ALLOWED_INTENTS = [
    "mencari", "checkout", "lacak_kiriman",
    "status_pesanan", "batal_pesanan",
    "faq", "tanya_toko", "help",
    "salam", "terima_kasih", "selamat_tinggal",
    "tidak_diketahui"
]

# only admin
ADMIN_ONLY_INTENTS = ["crud"]

# base endpoint bot
# intent search
@router.post("/aily/conversation/{role}")
def chat(role: str, body: ChatMessage):
    # print(f"[CHAT] user={id}, pesan={body.message}")

    # cari user berdasarkan hashed pass for a tokenized session
    db = SQLite()
    user = db.findUserByPassword(body.id)

    try:
        if user is None:
            return Response.NotFound("User tidak ditemukan, silahkan login ulang")

        # user = (id, username, password, email, phone, address, role)
        role = user[6].lower()
        username = user[1]
        
        result = nlp_process(body.message)
        intent = result.get("intent")
        action_data = None

        # save input -> log
        save_chat(body.id, username, role, body.message)

        # cek akses berdasarkan role
        if intent in ADMIN_ONLY_INTENTS and role != "admin":
            return Response.Error(message="Anda tidak memiliki akses untuk fitur ini. Hanya admin yang bisa mengelola produk.")

        # intent handler
    #     "mencari", "checkout", "lacak_kiriman",
    # "status_pesanan", "batal_pesanan",
    # "faq", "tanya_toko", "help",
    # "salam", "terima_kasih", "selamat_tinggal",
    # "tidak_diketahui"
        if intent == "faq" or intent == "tanya_toko":
            response = tentangToko()
            action_data = response
        elif intent == "help":
            response = help()
            action_data = response
        elif intent == "crud" and role == "admin":
            from routers.productManagementService import perform_delete_product, perform_list_products
            import re
            
            konten = result.get("konten", "").lower()
            id_match = re.search(r'\d+', konten)
            product_id = int(id_match.group()) if id_match else None
            
            if "tambah" in konten or "input" in konten:
                action_data = {"message": "Silakan gunakan endpoint POST /aily/admin/product/add pelengkap data produk untuk menambah produk.", "type": "add"}
            elif "hapus" in konten or "delete" in konten:
                if product_id is not None:
                    action_data = perform_delete_product(product_id)
                else:
                    action_data = {"message": "Sebutkan ID produk yang ingin dihapus. Contoh: 'hapus produk 1'", "type": "delete"}
            elif "update" in konten or "edit" in konten or "ubah" in konten:
                if product_id is not None:
                    action_data = {"message": f"Silakan gunakan endpoint PUT /aily/admin/product/update/{product_id} dengan data terbaru.", "type": "update", "product_id": product_id}
                else:
                    action_data = {"message": "Sebutkan ID produk yang ingin diubah. Contoh: 'edit produk 2'", "type": "update"}
            else:
                action_data = perform_list_products()
        elif intent == "mencari":
            gender = result.get("atribut").get("gender") 
            if gender == "default_user":
                gender = 'L' 
            
            toko_response = searchBarangResult(role, result.get("konten"), gender)
            action_data = toko_response
    # elif intent == "checkout": 

        # Simpan response bot ke MongoDB
        bot_response_text = action_data
        save_chat(body.id, "AILY Bot", "bot", bot_response_text)
       
        return Response.Ok(data={
            "user_id": body.id,
            "username": username,
            "role": role,
            "input": body.message,
            "nlp_result": result,
            "action_data": action_data
        })
    except Exception as e:
        import traceback
        return {"error": str(e), "traceback": traceback.format_exc()}

@router.get("/aily/tentangToko")
def tentangToko():
    db = SQLite()
    result = db.getTentangToko()
    return Response.Ok(data={
        "result": result
    })

@router.get("/aily/help")
def help():
    db = SQLite()
    result = db.getHelp()
    return Response.Ok(data={
        "result": result
    })

def save_chat(user_id, username: str, role: str, message: Any):
    chatLog = MongoDB("chatUserLog")
    from datetime import datetime
    now = datetime.now()

    chat_message = {
        "username": username,
        "role": role,
        "message": message,
        "datetime": now.strftime("%b, %d %Y"),
        "time": now.strftime("%H:%M")
    }

    user_doc = chatLog.find_one({"user_id": user_id})

    if user_doc is None:
        chat_data = {
            "user_id": user_id,
            "chats": [chat_message]
        }
        chatLog.insert(chat_data)
    else:
        chatLog.push({"user_id": user_id}, {"chats": chat_message})


@router.post("/aily/user/conversation/chat/save")
def save_chat_endpoint(body: ChatSaveRequest):
    save_chat(body.user_id, body.username, body.role, body.message)
    return Response.Ok(data={
        "message": "Chat successfully saved"
    })

@router.get("/aily/user/conversation/chat/load")
def load_chat(user_id: str):
    chatLog = MongoDB("chatUserLog")
    
    # Cari dokumen log untuk user ini
    user_doc = chatLog.find_one({"user_id": user_id})

    if user_doc is None:
        # Jika belum ada, buat 1 dokumen baru
        chat_data = {
            "user_id": user_id,
            "chats": []
        }
        chatLog.insert(chat_data)
        all_chats = []
    else:
        # Jika sudah ada, load dari id
        all_chats = user_doc.get("chats", [])

    return Response.Ok(data={
        "user_id": user_id,
        "chat_history": all_chats
    })

@router.post("/aily/user/updateUser")
def modifyProfile(id: str, dataList: List[list] = Body(...)):
    db = SQLite()
    user = db.findUserByPassword(id)
    if user is None:
        return Response.NotFound("User tidak ditemukan")

    user_id = user[0]

    #  [[col1, new_data1], [col2, new_data2]]
    for i in dataList:
        updateUser(user_id, i[0], i[1])
    return Response.Ok(data={
        "message": "Profile updated successfully"
    })

# modif profile
def updateUser(id, colum_name, data_new):
    db = SQLite()
    db.update("user", colum_name, data_new, id)


# cari barang
def searchBarangResult(role: str, name: str, gender: str):
    db = ProductDB()
    result = db.searchBarang(role, name, gender)
    return result
