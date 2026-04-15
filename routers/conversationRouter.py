# conversation handles FAQ, user's question
from fastapi import APIRouter
from utils.response import Response
# Gunakan APIRouter, ini ibarat "cabang" dari aplikasi utama FastAPI
router = APIRouter()

@router.get("/aily/conversation/faq/{id}/{publicKeyUser}")
def faq_question(id: str, publicKeyUser: str):
    if (True):
        return [Response.Ok,{
                "role" : "Admin",
                "datetime" : "Jan, 12 2025",
                "time" : "12:00",
                "message" : [
                    "Tentang Toko AILY",
                    {
                        "jam buka" : "00.00 - 24.00",
                        "hari buka" : "Setiap Hari kecuali kiamat",
                        "Lokasi" : "Planet Bekasi",
                        "Barang Dijual" : "DIY, Elektronik, your mom",
                        "Pemilik" : "Anton",
                        "Kontak" : "08386854493",
                        "Instagram" : "Gwkiyaco",
                    }
                ]
        }]
    else:
        return Response.HTTP_NOT_FOUND("Ga ngerti")

# ni ntar question nya mau dimasukin ke url nya atau enda
@router.post("/aily/conversation/question/{id}/{publicKeyUser}")
def chat_user(id: str, publicKeyUser: str):
    if(True):
        return {
                "role" : "Admin",
                "datetime" : "Jan, 12 2025",
                "time" : "12:00",
                "message" : {
                    "Saya menemukan beberapa barang ini": 
                    [
                        {"nama" : "baranga", "harga": 90000}, 
                        {"nama" : "barang b", "harga" : 100000}
                    ]
                }
        }
    else: 
        return  Response.NotFound("Tidak ada barang seperti yang kamu cari")



