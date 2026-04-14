"""
setup_env.py — Setup Virtual Environment & Install Dependencies
===============================================================
Membuat virtual environment (venv) dan menginstall semua
dependency dari requirements.txt secara otomatis.

Cara pakai:
    python setup_env.py
"""

import subprocess
import sys
import os


VENV_DIR = "venv"
REQUIREMENTS_FILE = "requirements.txt"


def run(cmd: list, check: bool = True):
    """Jalankan command dan tampilkan output realtime."""
    print(f"\n> {' '.join(cmd)}")
    print("-" * 50)
    result = subprocess.run(cmd, check=check)
    return result.returncode


def main():
    print("=" * 55)
    print("  SETUP VIRTUAL ENVIRONMENT — AILY Project")
    print("=" * 55)

    # ── 1. Cek apakah requirements.txt ada ──
    if not os.path.exists(REQUIREMENTS_FILE):
        print(f"[ERROR] File '{REQUIREMENTS_FILE}' tidak ditemukan!")
        print("        Pastikan file ada di direktori yang sama.")
        sys.exit(1)

    # ── 2. Buat virtual environment ──
    if os.path.exists(VENV_DIR):
        print(f"\n[INFO] Virtual environment '{VENV_DIR}' sudah ada.")
        print("       Melewati pembuatan venv...")
    else:
        print(f"\n[1/3] Membuat virtual environment '{VENV_DIR}'...")
        run([sys.executable, "-m", "venv", VENV_DIR])
        print("[OK]  Virtual environment berhasil dibuat.")

    # ── 3. Tentukan path pip di dalam venv ──
    if sys.platform == "win32":
        pip_path = os.path.join(VENV_DIR, "Scripts", "pip.exe")
        python_path = os.path.join(VENV_DIR, "Scripts", "python.exe")
    else:
        pip_path = os.path.join(VENV_DIR, "bin", "pip")
        python_path = os.path.join(VENV_DIR, "bin", "python")

    # ── 4. Upgrade pip ──
    print("\n[2/3] Upgrade pip...")
    run([python_path, "-m", "pip", "install", "--upgrade", "pip"])

    # ── 5. Install requirements ──
    print(f"\n[3/3] Install dependencies dari '{REQUIREMENTS_FILE}'...")
    run([pip_path, "install", "-r", REQUIREMENTS_FILE])

    # ── Selesai ──
    print("\n" + "=" * 55)
    print("  SETUP SELESAI!")
    print("=" * 55)
    print(f"  Virtual env  : {os.path.abspath(VENV_DIR)}")
    print(f"  Python       : {python_path}")
    print(f"  Pip          : {pip_path}")
    print()
    print("  Untuk mengaktifkan venv:")
    if sys.platform == "win32":
        print(f"    .\\{VENV_DIR}\\Scripts\\activate")
    else:
        print(f"    source {VENV_DIR}/bin/activate")
    print()
    print("  Lalu jalankan:")
    print("    python main.py")
    print()


if __name__ == "__main__":
    main()
