# will take care of authentication 
from utils.response import Response
from fastapi import APIRouter
from auth.key_validator import validate_key
from services.databaseConnection import MongoDB, SQLite

import bcrypt

# branch route from FASTApi
router = APIRouter()

def connect(): 
    return SQLite()

# regist
@router.post("/aily/registration")
def registration(uname, pword, email, phone, add, role):
    encrypted_pword = bcrypt.hashpw(pword.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    db = connect()
    # di cek dl kalau role dia harus mastiin sebagai user bukan admin
    if db.registerUser(uname, encrypted_pword, email, phone, add, role):
        return Response.Ok(data = {"username" : uname,
        "password" : encrypted_pword,
        "email" : email,
        "phone" : phone,
        "address" : add,
        "role" : role
        })
    else:
        return Response.Error(message = "Gagal Mendaftarkan Diri")

    
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



