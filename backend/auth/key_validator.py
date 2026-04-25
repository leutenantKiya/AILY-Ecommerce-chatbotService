try:
    from data.dummy_keys import USERS, ADMINS
except ModuleNotFoundError:
    USERS = {}
    ADMINS = {}


def validate_key(id: str, public_key: str, role: str) -> bool:
    # choose data based on role
    if role == "user":
        data = USERS
    elif role == "admin":
        data = ADMINS
    else:
        return False

    # Cek apakah id ada di data, dan apakah key-nya cocok
    # data.get(id) → ambil value dari key `id`, return None jika tidak ada
    stored_key = data.get(id)

    if stored_key is None:
        # ID tidak ditemukan
        return False

    # Bandingkan key yang tersimpan dengan key yang dikirim
    return stored_key == public_key
