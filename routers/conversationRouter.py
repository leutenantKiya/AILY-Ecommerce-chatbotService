from datetime import datetime
import base64
import re
from typing import Any, List, Optional

from fastapi import APIRouter, Body
from pydantic import BaseModel

from NLP.NLPHandler import process as nlp_process
from routers.cartService import perform_add_to_cart, perform_get_cart_summary
from routers.productManagementService import (
    perform_add_product,
    perform_delete_product,
    perform_list_products,
    perform_update_product,
)
from services.databaseConnection import MongoDB, ProductDB, SQLite
from utils.response import Response

router = APIRouter()


class ChatMessage(BaseModel):
    id: Optional[str] = None
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
    "cart", "tidak_diketahui"
]

ADMIN_ONLY_INTENTS = ["crud"]

PRODUCT_FIELD_MAP = {
    "nama": "name",
    "name": "name",
    "harga": "price",
    "price": "price",
    "stok": "stock",
    "stock": "stock",
    "deskripsi": "description",
    "description": "description",
    "gender": "gender",
    "gambar": "image",
    "image": "image",
}


def normalize_gender_value(value: Optional[str]) -> str:
    if value is None:
        return "U"

    normalized = value.strip().upper()
    if normalized in ("L", "P", "U"):
        return normalized
    if normalized in ("PRIA", "LAKI", "LAKI-LAKI"):
        return "L"
    if normalized in ("WANITA", "PEREMPUAN"):
        return "P"
    return "U"


def parse_numeric_value(value: Optional[str], default: Optional[int] = None):
    if value is None:
        return default

    digits = re.sub(r"[^\d]", "", value)
    if not digits:
        return default
    return int(digits)


def extract_key_value_fields(text: str):
    pattern = r"(nama|name|harga|price|stok|stock|deskripsi|description|gender|gambar|image)\s*="
    matches = list(re.finditer(pattern, text, flags=re.IGNORECASE))
    if not matches:
        return {}

    fields = {}
    for index, match in enumerate(matches):
        key = PRODUCT_FIELD_MAP[match.group(1).lower()]
        start = match.end()
        end = matches[index + 1].start() if index + 1 < len(matches) else len(text)
        value = text[start:end].strip(" ,;")
        value = value.strip().strip('"').strip("'")
        fields[key] = value
    return fields


def build_admin_crud_help(action_type: str):
    help_map = {
        "add": "Contoh tambah: tambah produk nama=Kaos Polos, harga=99000, stok=10, deskripsi=Kaos basic nyaman, gender=U",
        "update": "Contoh update: update produk 12 harga=109000, stok=8, deskripsi=Stok baru, gender=L",
        "delete": "Contoh hapus: hapus produk 12",
        "default": "Contoh: lihat produk, tambah produk nama=Kaos, harga=99000, stok=10, deskripsi=Kaos basic, gender=U",
    }
    return help_map.get(action_type, help_map["default"])


def execute_add_product_from_chat(message: str):
    fields = extract_key_value_fields(message)
    missing_fields = [field for field in ("name", "price", "stock", "description") if not fields.get(field)]
    if missing_fields:
        return {
            "message": f"Data produk belum lengkap. Wajib isi: {', '.join(missing_fields)}.\n{build_admin_crud_help('add')}",
            "type": "add",
        }

    price = parse_numeric_value(fields.get("price"))
    stock = parse_numeric_value(fields.get("stock"))
    if price is None or stock is None:
        return {
            "message": f"Harga dan stok harus berupa angka.\n{build_admin_crud_help('add')}",
            "type": "add",
        }

    return perform_add_product(
        name=fields["name"],
        price=price,
        stock=stock,
        description=fields["description"],
        image=fields.get("image"),
        gender=normalize_gender_value(fields.get("gender")),
    )


def execute_update_product_from_chat(message: str, product_id: Optional[int]):
    if product_id is None:
        return {
            "message": f"Sebutkan ID produk yang ingin diubah.\n{build_admin_crud_help('update')}",
            "type": "update",
        }

    product = ProductDB().getProductById(product_id)
    if product is None:
        return {
            "message": f"Produk dengan ID {product_id} tidak ditemukan.",
            "type": "update",
        }

    fields = extract_key_value_fields(message)
    if not fields:
        return {
            "message": f"Belum ada data baru untuk diperbarui.\n{build_admin_crud_help('update')}",
            "type": "update",
        }

    price = parse_numeric_value(fields.get("price"), product["price"])
    stock = parse_numeric_value(fields.get("stock"), product["stock"])
    if price is None or stock is None:
        return {
            "message": f"Harga dan stok harus berupa angka.\n{build_admin_crud_help('update')}",
            "type": "update",
        }

    return perform_update_product(
        product_id=product_id,
        name=fields.get("name", product["name"]),
        price=price,
        stock=stock,
        description=fields.get("description", product["description"]),
        image=fields.get("image", product["image"]),
        gender=normalize_gender_value(fields.get("gender", product["gender"])),
    )


def handle_admin_product_command(message: str):
    lowered = message.lower()
    id_match = re.search(r"\bproduk\s+(\d+)\b|\bid\s+(\d+)\b|\b(\d+)\b", lowered)
    product_id = None
    if id_match is not None:
        for group in id_match.groups():
            if group is not None:
                product_id = int(group)
                break

    if any(keyword in lowered for keyword in ("tambah", "input", "buat")):
        return execute_add_product_from_chat(message)

    if any(keyword in lowered for keyword in ("hapus", "delete")):
        if product_id is None:
            return {
                "message": f"Sebutkan ID produk yang ingin dihapus.\n{build_admin_crud_help('delete')}",
                "type": "delete",
            }
        return perform_delete_product(product_id)

    if any(keyword in lowered for keyword in ("update", "edit", "ubah")):
        return execute_update_product_from_chat(message, product_id)

    if any(keyword in lowered for keyword in ("lihat", "list", "daftar", "semua", "tampilkan")):
        return perform_list_products()

    return {
        "message": f"Perintah CRUD belum dikenali.\n{build_admin_crud_help('default')}",
        "type": "crud",
    }


def resolve_product_from_cart_command(message: str, user_gender: str):
    product_db = ProductDB()
    id_match = re.search(r"\bproduk\s+(\d+)\b|\bid\s+(\d+)\b", message.lower())
    if id_match is not None:
        for group in id_match.groups():
            if group is not None:
                return product_db.getProductById(int(group))

    cleaned = re.sub(
        r"\b(tambahkan|tambah|masukkan|masukin|taruh|beli|produk|barang|ke|dalam|keranjang|cart|qty|jumlah|sebanyak|\d+)\b",
        " ",
        message,
        flags=re.IGNORECASE,
    )
    keyword = re.sub(r"\s+", " ", cleaned).strip(" ,.-")
    if not keyword:
        return None

    matches = product_db.findProductsByKeyword(keyword, user_gender, limit=5)
    if len(matches) == 1:
        return matches[0]
    if len(matches) > 1:
        return {
            "ambiguous": True,
            "keyword": keyword,
            "matches": matches,
        }
    return None


def try_handle_cart_command(user_token: str, user: tuple, message: str):
    lowered = message.lower().strip()
    if not any(keyword in lowered for keyword in ("keranjang", "cart")):
        return None

    if any(keyword in lowered for keyword in ("lihat", "isi", "cek", "tampilkan")) or lowered in ("keranjang", "cart"):
        return perform_get_cart_summary(user_token)

    if any(keyword in lowered for keyword in ("tambah", "tambahkan", "masukkan", "masukin", "taruh", "beli")):
        quantity_match = re.search(r"(qty|jumlah|sebanyak)\s*(=|:)?\s*(\d+)", lowered)
        quantity = int(quantity_match.group(3)) if quantity_match is not None else 1
        product_ref = resolve_product_from_cart_command(message, user[7] if len(user) > 7 else "U")

        if product_ref is None:
            return {
                "message": "Produk yang mau dimasukkan ke keranjang belum ketemu. Coba pakai ID produk atau nama yang lebih spesifik.",
                "type": "cart",
            }

        if isinstance(product_ref, dict) and product_ref.get("ambiguous"):
            options = "\n".join(
                f"- ID {item['id']}: {item['name']}" for item in product_ref["matches"]
            )
            return {
                "message": (
                    f"Ada beberapa produk yang cocok dengan '{product_ref['keyword']}':\n"
                    f"{options}\n"
                    "Coba sebutkan ID produknya ya."
                ),
                "type": "cart",
            }

        return perform_add_to_cart(user_token, int(product_ref["id"]), quantity)

    return {
        "message": "Saya bisa bantu lihat keranjang atau menambahkan produk ke keranjang. Contoh: 'lihat keranjang' atau 'tambah produk 14 ke keranjang'.",
        "type": "cart",
    }


@router.post("/aily/conversation")
def chat(body: ChatMessage):
    return handle_chat(body.id, body)


@router.post("/aily/conversation/{user_token:path}")
def chat_legacy(user_token: str, body: ChatMessage):
    return handle_chat(body.id or user_token, body)


def handle_chat(user_token: Optional[str], body: ChatMessage):
    if not user_token:
        return Response.ValidationError("User tidak ditemukan, silahkan login ulang")

    db = SQLite()
    user = db.findUserByPassword(user_token)
    try:
        if user is None:
            return Response.NotFound("User tidak ditemukan, silahkan login ulang")

        role = user[6].lower()
        username = user[1]

        save_chat(user_token, username, role, body.message)

        cart_action = try_handle_cart_command(user_token, user, body.message)
        if cart_action is not None:
            result = {"intent": "cart", "konten": body.message}
            action_data = cart_action
        else:
            result = nlp_process(body.message)
            intent = result.get("intent")
            konten = result.get("konten", "")
            action_data = None

            if intent in ADMIN_ONLY_INTENTS and role != "admin":
                return Response.Error(message="Anda tidak memiliki akses untuk fitur ini. Hanya admin yang bisa mengelola produk.")

            if intent == "mencari" and str(konten).strip() == "":
                action_data = {"message": "Panjenengan puniki nggolek apa si sakjane", "type": "mencari"}
            elif intent in ("faq", "tanya_toko"):
                action_data = tentangToko()
                action_data["type"] = "tanya_toko"
            elif intent == "help":
                action_data = help()
            elif intent == "crud" and role == "admin":
                action_data = handle_admin_product_command(body.message)
            elif intent == "mencari":
                gender = result.get("atribut", {}).get("gender", "default_user")
                if gender == "default_user":
                    gender = user[7]
                action_data = searchBarangResult(str(konten), gender)
            elif intent == "checkout":
                action_data = {"message": "Silakan buka halaman keranjang untuk melanjutkan checkout.", "type": "checkout"}
            elif intent in ("salam", "terima_kasih", "selamat_tinggal", "tidak_diketahui"):
                action_data = {"message": result.get("respons", ""), "type": intent}

            if action_data is None:
                action_data = {"message": result.get("respons", ""), "type": intent or "unknown"}

        bot_response_text = sanitize_for_json(action_data)
        save_chat(user_token, "AILY Bot", "bot", bot_response_text)

        return sanitize_for_json(Response.Ok(data={
            "user_id": user_token,
            "username": username,
            "role": role,
            "input": body.message,
            "nlp_result": result,
            "action_data": action_data
        }))
    except Exception as e:
        import traceback
        return sanitize_for_json({"error": str(e), "traceback": traceback.format_exc()})


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


@router.post("/aily/user/conversation/chat/save")
def save_chat_endpoint(body: ChatSaveRequest):
    save_chat(body.user_id, body.username, body.role, body.message)
    return Response.Ok(data={
        "message": "Chat successfully saved"
    })


def save_chat(user_id, username: str, role: str, message: Any):
    chatLog = MongoDB("chatUserLog")
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


def sanitize_for_json(obj):
    if isinstance(obj, bytes):
        return base64.b64encode(obj).decode("utf-8")
    if isinstance(obj, dict):
        return {k: sanitize_for_json(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [sanitize_for_json(item) for item in obj]
    return obj


@router.get("/aily/user/conversation/chat/load")
def load_chat(user_id: str):
    chatLog = MongoDB("chatUserLog")
    user_doc = chatLog.find_one({"user_id": user_id})

    if user_doc is None:
        chat_data = {
            "user_id": user_id,
            "chats": []
        }
        chatLog.insert(chat_data)
        all_chats = []
    else:
        all_chats = user_doc.get("chats", [])

    safe_chats = sanitize_for_json(all_chats)

    return Response.Ok(data={
        "user_id": user_id,
        "chat_history": safe_chats
    })


@router.delete("/aily/user/conversation/chat/delete")
def delete_chat(user_id: str):
    chatLog = MongoDB("chatUserLog")
    chatLog.delete({"user_id": user_id})
    return Response.Ok(data={
        "message": "Chat deleted successfully"
    })


@router.post("/aily/user/updateUser")
def modifyProfile(id: str, dataList: List[list] = Body(...)):
    db = SQLite()
    user = db.findUserByPassword(id)
    if user is None:
        return Response.NotFound("User tidak ditemukan")

    user_id = user[0]
    for item in dataList:
        updateUser(user_id, item[0], item[1])
    return Response.Ok(data={
        "message": "Profile updated successfully"
    })


def updateUser(id, colum_name, data_new):
    db = SQLite()
    db.update("user", colum_name, data_new, id)


def searchBarangResult(name: str, gender: str):
    db = ProductDB()
    results = db.searchBarang(name, gender)
    formatted = []
    for product in results:
        formatted.append({
            "id": product[0],
            "name": product[1],
            "price": product[2],
            "stock": product[3],
            "image": product[4],
            "description": product[5],
            "gender": product[6],
        })
    return {"products": formatted, "type": "mencari"}
