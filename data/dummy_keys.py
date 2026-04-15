# ===========================================================
# data/dummy_keys.py — Data Dummy untuk Validasi Key
# ===========================================================
# File ini menyimpan data user dan admin beserta publicKey-nya.
# Ini pengganti database sementara.
#
# Struktur: dictionary (dict)
#   key   = id user/admin
#   value = publicKey milik user/admin tersebut
#
# Contoh: USERS["user01"] → "publicKey-ABC123"
# Artinya: user dengan id "user01" punya publicKey "publicKey-ABC123"
# ===========================================================

USERS = {
    "user01": "publicKey-ABC123",
    "user02": "publicKey-DEF456",
    "user03": "publicKey-GHI789",
}

ADMINS = {
    "admin": "publicKey-XYZ789"
}
