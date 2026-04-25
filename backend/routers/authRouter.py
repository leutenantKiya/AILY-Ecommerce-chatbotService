# will take care of authentication 
from utils.response import Response
from fastapi import APIRouter
from services.databaseConnection import SQLite

import bcrypt

# branch route from FASTApi
router = APIRouter()

def connect(): 
    return SQLite()

# regist
@router.post("/aily/registration")
def registration(uname, pword, email, phone, add, role, gender: str = "L"):
    encrypted_pword = bcrypt.hashpw(pword.encode('utf-8'), bcrypt.gensalt()).decode('utf-8')
    db = connect()
    if db.findUser(uname):
        return Response.ValidationError("Username sudah ada")
    # di cek dl kalau role dia harus mastiin sebagai user bukan admin
    normalized_gender = gender.upper() if gender.upper() in ("L", "P") else "L"
    if db.registerUser(uname, encrypted_pword, email, phone, add, role, normalized_gender):
        return Response.Ok(data = {"username" : uname,
        "password" : encrypted_pword,
        "email" : email,
        "phone" : phone,
        "address" : add,
        "role" : role,
        "gender": normalized_gender
        })
    else:
        return Response.Error(message = "Gagal Mendaftarkan Diri")

# login
@router.post("/aily/login")
def login(uname: str, pword: str):
    db = connect()
    userUname = db.findUser(uname)
    userEmail = db.findEmail(uname)
    
    print(userUname)
    print(userEmail)

    if userEmail is None and userUname is None:
        return Response.ValidationError("Username atau Email tidak ditemukan")

    # user = (id, username, password, email, phone, address, role)
    if '@' in uname and '.' in uname:
        stored_hash = userEmail[2]
        print(userEmail)
        user = userEmail
    else:
        stored_hash = userUname[2]
        print(userUname)
        user = userUname
    
    if not bcrypt.checkpw(pword.encode('utf-8'), stored_hash.encode('utf-8')):
        return Response.Error(message="Password salah")

    return Response.Ok(data={
        "id": stored_hash,
        "username": user[1],
        "email": user[3],
        "phone": user[4],
        "address": user[5],
        "role": user[6],
        "gender": user[7] if len(user) > 7 else "L"
    })
