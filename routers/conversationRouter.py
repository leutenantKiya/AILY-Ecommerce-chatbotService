# conversation handles user's question → NLP → response
from fastapi import APIRouter
from pydantic import BaseModel
from utils.response import Response
from NLP.NLPHandler import process as nlp_process
from services.databaseConnection import SQLite, MongoDB

router = APIRouter()

class ChatMessage(BaseModel):
    message: str

USER_ALLOWED_INTENTS = [
    "mencari", "checkout", "lacak_kiriman",
    "status_pesanan", "batal_pesanan",
    "faq", "tanya_toko", "help",
    "salam", "terima_kasih", "selamat_tinggal",
    "tidak_diketahui"
]

# intent yang hanya boleh diakses admin
ADMIN_ONLY_INTENTS = ["crud"] + USER_ALLOWED_INTENTS

# base endpoint bot
@router.post("/aily/conversation/{id}")
def chat(id: str, body: ChatMessage):
    print(f"[CHAT] user={id}, pesan={body.message}")

    # cari user berdasarkan hashed pass for a tokenized session
    db = SQLite()
    user = db.findUserByPassword(id)

    if user is None:
        return Response.NotFound("User tidak ditemukan, silahkan login ulang")

    # user = (id, username, password, email, phone, address, role)
    role = user[6].lower()
    username = user[1]
    print(f"[AUTH] username={username}, role={role}")

    # 
    result = nlp_process(body.message)
    intent = result.get("intent")
    action_data = None

    # cek akses berdasarkan role
    if intent in ADMIN_ONLY_INTENTS and role != "admin":
        return Response.Error(message="Anda tidak memiliki akses untuk fitur ini. Hanya admin yang bisa mengelola produk.")

    # intent handler
    if intent == "faq" or intent == "tanya_toko":
        toko_response = tentangToko()
        action_data = toko_response
    elif intent == "crud" and role == "admin":
        action_data = {"message": "Silahkan lanjutkan operasi CRUD produk"}

    return Response.Ok(data={
        "user_id": id,
        "username": username,
        "role": role,
        "input": body.message,
        "nlp_result": result,
        "action_data": action_data
    })

@router.get("/aily/tentangToko")
def tentangToko():
    db = SQLite()
    result = db.getTentangToko()
    return Response.Ok(data={
        "result": result
    })

@router.post("/aily/user/{id}/{publicKeyUser}")
def chat_user(id: str, publicKeyUser: str):
    # Cetak ke terminal (untuk debugging)
    print(f"[USER] id={id}, key={publicKeyUser}")

    # Validasi: cek apakah id + publicKey cocok
    if not validate_key(id, publicKeyUser, role="user"):
        return Response.NotFound("Key Tidak Sesuai")

    # Jika valid, lanjutkan (nanti di sini akan ada NLP processing)
    return Response.Ok(data=
        [{
            "role" : "Admin",
            "datetime" : "Jan, 12 2025",
            "time" : "12:00",
            "message" : "apa la"
        },
        {
            "role" : "System",
            "datetime" : "Jan, 12 2025",
            "time" : "12:01",
            "message" : "Gapaham jink" 
        }]
    )

@router.post("/aily/admin/{id}/{publicKeyAdmin}")
def chat_admin(id: str, publicKeyAdmin: str):
    print(f"[ADMIN] id={id}, key={publicKeyAdmin}")

    # Validasi: cek apakah id + publicKey cocok
    if not validate_key(id, publicKeyAdmin, role="admin"):
        return Response.NotFound("Key Tidak Sesuai")

    # Jika valid, lanjutkan (nanti di sini akan ada NLP processing)
    return Response.Ok(data=
        ({
            "role" : "Admin",
            "datetime" : "Jan, 12 2025",
            "time" : "12:00",
            "message" : "apa la"
        },
        {
            "role" : "System",
            "datetime" : "Jan, 12 2025",
            "time" : "12:01",
            "message" : "Gapaham jink" 
        })
    )



