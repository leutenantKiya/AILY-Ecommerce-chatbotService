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

# login
@router.post("/aily/login")
def login(uname: str, pword: str):
    db = connect()
    user = db.findUser(uname)

    if user is None:
        return Response.NotFound("Username tidak ditemukan")

    # user = (id, username, password, email, phone, address, role)
    stored_hash = user[2]

    if not bcrypt.checkpw(pword.encode('utf-8'), stored_hash.encode('utf-8')):
        return Response.Error(message="Password salah")

    return Response.Ok(data={
        "id": stored_hash,
        "username": user[1],
        "email": user[3],
        "phone": user[4],
        "address": user[5],
        "role": user[6]
    })
