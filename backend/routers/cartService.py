from fastapi import APIRouter
from pydantic import BaseModel

from services.databaseConnection import CartDB, ProductDB, SQLite
from utils.response import Response

router = APIRouter()


class CartAddRequest(BaseModel):
    user_id: str
    product_id: int
    quantity: int = 1


class CartUpdateRequest(BaseModel):
    user_id: str
    product_id: int
    quantity: int


def _resolve_user(user_token: str):
    db = SQLite()
    return db.resolveUser(user_token)


def _build_cart_payload(user_id: int):
    cart_db = CartDB()
    items = cart_db.getCartItems(user_id)
    subtotal = sum(item["price"] * item["quantity"] for item in items)
    item_count = sum(item["quantity"] for item in items)
    return {
        "items": items,
        "item_count": item_count,
        "subtotal": subtotal,
    }


@router.get("/aily/user/cart")
def get_cart(user_id: str):
    user = _resolve_user(user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    return Response.Ok(data=_build_cart_payload(user[0]))


@router.post("/aily/user/cart/add")
def add_to_cart(body: CartAddRequest):
    user = _resolve_user(body.user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    cart_db = CartDB()
    success, message = cart_db.addToCart(user[0], body.product_id, body.quantity)
    if not success:
        return Response.Error(message=message)

    payload = _build_cart_payload(user[0])
    payload["message"] = message
    return Response.Ok(data=payload)


@router.put("/aily/user/cart/item")
def update_cart_item(body: CartUpdateRequest):
    user = _resolve_user(body.user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    cart_db = CartDB()
    success, message = cart_db.updateCartItem(user[0], body.product_id, body.quantity)
    if not success:
        return Response.Error(message=message)

    payload = _build_cart_payload(user[0])
    payload["message"] = message
    return Response.Ok(data=payload)


@router.delete("/aily/user/cart/item")
def remove_cart_item(user_id: str, product_id: int):
    user = _resolve_user(user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    cart_db = CartDB()
    cart_db.removeCartItem(user[0], product_id)
    payload = _build_cart_payload(user[0])
    payload["message"] = "Produk dihapus dari keranjang."
    return Response.Ok(data=payload)


@router.delete("/aily/user/cart/clear")
def clear_cart(user_id: str):
    user = _resolve_user(user_id)
    if user is None:
        return Response.NotFound("User tidak ditemukan.")

    cart_db = CartDB()
    cart_db.clearCart(user[0])
    payload = _build_cart_payload(user[0])
    payload["message"] = "Keranjang berhasil dikosongkan."
    return Response.Ok(data=payload)


def perform_add_to_cart(user_token: str, product_id: int, quantity: int = 1):
    user = _resolve_user(user_token)
    if user is None:
        return {"message": "User tidak ditemukan.", "type": "cart"}

    cart_db = CartDB()
    success, message = cart_db.addToCart(user[0], product_id, quantity)
    if not success:
        return {"message": message, "type": "cart"}

    product = ProductDB().getProductById(product_id)
    if product is None:
        return {"message": message, "type": "cart"}

    return {
        "message": f"{product['name']} berhasil ditambahkan ke keranjang ({quantity}x).",
        "type": "cart",
        "product": product,
        "quantity": quantity,
        "cart_summary": _build_cart_payload(user[0]),
    }


def perform_get_cart_summary(user_token: str):
    user = _resolve_user(user_token)
    if user is None:
        return {"message": "User tidak ditemukan.", "type": "cart"}

    payload = _build_cart_payload(user[0])
    items = payload["items"]
    if not items:
        return {"message": "Keranjang kamu masih kosong.", "type": "cart", "cart_summary": payload}

    lines = ["Isi keranjang kamu:"]
    for index, item in enumerate(items, start=1):
        lines.append(
            f"{index}. {item['name']} - {item['quantity']}x - Rp {item['price']:,}".replace(",", ".")
        )
    lines.append(f"Subtotal: Rp {payload['subtotal']:,}".replace(",", "."))

    return {
        "message": "\n".join(lines),
        "type": "cart",
        "cart_summary": payload,
    }

@router.get("/aily/admin/cart/list")
def get_cart():
    cart_db = CartDB()
    payload = cart_db.getAllCart()
    
    return Response.Ok(data=payload)
