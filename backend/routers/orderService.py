import re

from fastapi import APIRouter
from pydantic import BaseModel

from services.databaseConnection import CartDB, MongoDB, OrderDB, SQLite
from utils.response import Response

router = APIRouter()


class CheckoutRequest(BaseModel):
    user_id: str


class OrderStatusRequest(BaseModel):
    status: str


def _resolve_user(user_token: str):
    db = SQLite()
    return db.resolveUser(user_token)

def _sanitize_for_json(obj):
    if isinstance(obj, bytes):
        import base64
        return base64.b64encode(obj).decode("utf-8")
    if isinstance(obj, dict):
        return {k: _sanitize_for_json(v) for k, v in obj.items()}
    if isinstance(obj, (list, tuple)):
        return [_sanitize_for_json(item) for item in obj]
    return obj


def extract_order_ref(message: str):
    if not message:
        return None

    trx_match = re.search(r"\bTRX[-\s]?0*(\d+)\b", message.upper())
    if trx_match is not None:
        return int(trx_match.group(1))

    number_match = re.search(r"\b(?:pesanan|order|id|nomor|no)\s*#?\s*(\d+)\b", message.lower())
    if number_match is not None:
        return int(number_match.group(1))

    return None


def _format_order_lines(order):
    lines = [
        f"Pesanan {order['order_code']}",
        f"Status: {order['status']}",
        f"Total: Rp {order['total']:,}".replace(",", "."),
    ]
    items = order.get("items", [])
    if items:
        lines.append("Barang:")
        for item in items:
            lines.append(f"- {item['product_name']} x{item['quantity']}")
    return "\n".join(lines)


def perform_checkout(user_token: str):
    user = _resolve_user(user_token)
    if user is None:
        return {"message": "User tidak ditemukan.", "type": "checkout"}

    success, message, order_id = CartDB().checkoutCart(user[0])
    if not success:
        return {"message": message, "type": "checkout"}

    order = OrderDB().getOrderById(order_id, user[0])
    return {
        "message": f"{message}\nNomor pesanan: {order['order_code']}",
        "type": "checkout",
        "order": order,
    }


def perform_get_order_status(user_token: str, message: str = ""):
    user = _resolve_user(user_token)
    if user is None:
        return {"message": "User tidak ditemukan.", "type": "status_pesanan"}

    order_db = OrderDB()
    order_ref = extract_order_ref(message)
    if order_ref is not None:
        order = order_db.getOrderById(order_ref, user[0])
        if order is None:
            return {"message": "Pesanan tidak ditemukan.", "type": "status_pesanan"}
        return {
            "message": _format_order_lines(order),
            "type": "status_pesanan",
            "order": order,
        }

    orders = order_db.getUserOrders(user[0])
    if not orders:
        return {"message": "Kamu belum memiliki pesanan.", "type": "status_pesanan", "orders": []}

    lines = ["Daftar pesanan terakhir:"]
    for order in orders[:5]:
        lines.append(f"- {order['order_code']} | {order['status']} | Rp {order['total']:,}".replace(",", "."))

    return {
        "message": "\n".join(lines),
        "type": "status_pesanan",
        "orders": orders,
    }


def perform_cancel_order(user_token: str, message: str = ""):
    user = _resolve_user(user_token)
    if user is None:
        return {"message": "User tidak ditemukan.", "type": "batal_pesanan"}

    order_ref = extract_order_ref(message)
    success, result_message, order = OrderDB().cancelOrder(user[0], order_ref)
    payload = {
        "message": result_message,
        "type": "batal_pesanan",
    }
    if order is not None:
        payload["order"] = order
        if success:
            payload["message"] = f"{result_message}\nPesanan {order['order_code']} berstatus {order['status']}."
    return payload


@router.post("/aily/user/checkout")
def checkout(body: CheckoutRequest):
    result = perform_checkout(body.user_id)
    if "order" not in result:
        return Response.Error(message=result["message"])
    return Response.Ok(data=result)


@router.get("/aily/user/orders")
def get_user_orders(user_id: str):
    user = _resolve_user(user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    return Response.Ok(data={"orders": OrderDB().getUserOrders(user[0])})


@router.post("/aily/user/orders/{order_id}/cancel")
def cancel_order(order_id: int, user_id: str):
    user = _resolve_user(user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    success, message, order = OrderDB().cancelOrder(user[0], order_id)
    if not success:
        return Response.Error(message=message, code=400)

    return Response.Ok(data={"message": message, "order": order})


@router.get("/aily/admin/orders")
def get_admin_orders():
    return Response.Ok(data={"orders": OrderDB().getAllOrders()})


@router.put("/aily/admin/orders/{order_id}/status")
def update_order_status(order_id: int, body: OrderStatusRequest):
    success, message, order = OrderDB().updateOrderStatus(order_id, body.status)
    if not success:
        return Response.Error(message=message)

    return Response.Ok(data={"message": message, "order": order})


@router.get("/aily/admin/chat/history")
def get_admin_chat_history_grouped():
    """
    Return chat history grouped by user_id and datetime ("Apr, 19 2026").
    Each chat includes time ("21:40").
    """
    chat_log = MongoDB("chatUserLog")
    sqlite = SQLite()

    users = []
    try:
        for doc in chat_log.find({}):
            user_id = str(doc.get("user_id", "")).strip()
            chats = _sanitize_for_json(doc.get("chats", [])) or []

            resolved = sqlite.resolveUser(user_id)
            username = resolved[1] if resolved is not None else ""
            if not username and chats:
                first = chats[0] if isinstance(chats[0], dict) else {}
                username = first.get("username", "")

            by_date = {}
            for chat in chats:
                if not isinstance(chat, dict):
                    continue
                date_key = chat.get("datetime", "")
                if date_key not in by_date:
                    by_date[date_key] = []
                by_date[date_key].append({
                    "username": chat.get("username", username),
                    "role": chat.get("role", "user"),
                    "message": chat.get("message", ""),
                    "time": chat.get("time", ""),
                })

            date_groups = []
            for date_key, items in by_date.items():
                date_groups.append({
                    "datetime": date_key,
                    "chats": items
                })

            users.append({
                "user_id": user_id,
                "username": username,
                "groups": date_groups
            })
    except Exception:
        users = []

    return Response.Ok(data={"users": users})


@router.get("/aily/admin/chat/history/user")
def get_admin_chat_history_by_user(user_id: str):
    chat_log = MongoDB("chatUserLog")
    sqlite = SQLite()

    user_id = str(user_id).strip()
    groups = []
    username = ""
    try:
        resolved = sqlite.resolveUser(user_id)
        username = resolved[1] if resolved is not None else ""

        doc = chat_log.find_one({"user_id": user_id})
        chats = _sanitize_for_json(doc.get("chats", [])) if doc is not None else []

        by_date = {}
        for chat in chats or []:
            if not isinstance(chat, dict):
                continue
            date_key = chat.get("datetime", "")
            if date_key not in by_date:
                by_date[date_key] = []
            by_date[date_key].append({
                "username": chat.get("username", username),
                "role": chat.get("role", "user"),
                "message": chat.get("message", ""),
                "time": chat.get("time", ""),
            })

        for date_key, items in by_date.items():
            groups.append({"datetime": date_key, "chats": items})
    except Exception:
        groups = []

    return Response.Ok(data={"user_id": user_id, "username": username, "groups": groups})
