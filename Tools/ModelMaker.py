# -*- coding: utf-8 -*-
"""
ModelMaker.py — Training NLP Chatbot Bahasa Indonesia
=====================================================
Membuat model neural network untuk klasifikasi intent chatbot.

Menggunakan:
  - PySastrawi  : Stemming & Tokenisasi Bahasa Indonesia
  - TensorFlow  : Membangun dan melatih model neural network

Output:
  - words.pkl       : Vocabulary hasil stemming
  - classes.pkl     : Daftar kelas/intent
  - chatbot.keras   : Model neural network yang sudah dilatih

Instalasi dependency:
  pip install PySastrawi tensorflow numpy
"""

import json
import pickle
import random
import re
import numpy as np

from Sastrawi.Stemmer.StemmerFactory import StemmerFactory
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Dense, Dropout
from tensorflow.keras.optimizers import SGD

# ─────────────────────────────────────────────
#  Fungsi tokenisasi sederhana (pengganti NLTK punkt)
# ─────────────────────────────────────────────
def word_tokenize(teks: str) -> list:
    """Tokenisasi teks menjadi daftar kata menggunakan regex."""
    return re.findall(r"[\w]+", teks)

# ─────────────────────────────────────────────
#  Inisialisasi Stemmer Sastrawi
# ─────────────────────────────────────────────
factory = StemmerFactory()
stemmer = factory.create_stemmer()

# ─────────────────────────────────────────────
#  Load Data Intents
# ─────────────────────────────────────────────
with open("intents.json", "r", encoding="utf-8") as f:
    intents = json.load(f)
# ─────────────────────────────────────────────
#  Preprocessing: Tokenisasi & Stemming
# ─────────────────────────────────────────────
words = []
classes = []
documents = []
ignore_letters = ["?", "!", ".", ",", "'"]

for intent in intents["intents"]:
    for pola in intent["patterns"]:
        # Tokenisasi setiap pattern menggunakan Sastrawi tokenizer
        word_list = word_tokenize(pola)
        words.extend(word_list)
        documents.append((word_list, intent["tag"]))
        
        if intent["tag"] not in classes:
            classes.append(intent["tag"])

# Stemming menggunakan Sastrawi (pengganti lemmatizer untuk Bahasa Indonesia)
words = [stemmer.stem(word.lower()) for word in words if word not in ignore_letters]

words = sorted(set(words))
classes = sorted(set(classes))

print(f"[INFO] Jumlah dokumen  : {len(documents)}")
print(f"[INFO] Jumlah kelas    : {len(classes)} -> {classes}")
print(f"[INFO] Jumlah kata unik: {len(words)}")

# Simpan vocabulary dan classes ke file pickle
pickle.dump(words, open("words.pkl", "wb"))
pickle.dump(classes, open("classes.pkl", "wb"))

# ─────────────────────────────────────────────
#  Membuat Training Data (Bag of Words)
# ─────────────────────────────────────────────
training = []
output_empty = [0] * len(classes)

for i, document in enumerate(documents):
    bag = []
    word_patterns = document[0]
    # Stemming kata-kata dalam pattern
    word_patterns = [stemmer.stem(word.lower()) for word in word_patterns]

    # Buat bag of words
    for word in words:
        bag.append(1) if word in word_patterns else bag.append(0)

    # Buat output row (one-hot encoding)
    output_row = list(output_empty)
    output_row[classes.index(document[1])] = 1
    training.append([bag, output_row])

# Shuffle data training
random.shuffle(training)
training = np.array(training, dtype=object)

train_x = list(training[:, 0])
train_y = list(training[:, 1])

print(f"[INFO] Ukuran train_x  : {len(train_x)} x {len(train_x[0])}")
print(f"[INFO] Ukuran train_y  : {len(train_y)} x {len(train_y[0])}")

# ─────────────────────────────────────────────
#  Membangun Model Neural Network
# ─────────────────────────────────────────────
model = Sequential()
model.add(Dense(128, input_shape=(len(train_x[0]),), activation="relu"))
model.add(Dropout(0.5))
model.add(Dense(64, activation="relu"))
model.add(Dropout(0.5))
model.add(Dense(len(train_y[0]), activation="softmax"))

sgd = SGD(learning_rate=0.001, momentum=0.9, nesterov=True)
model.compile(loss="categorical_crossentropy", optimizer=sgd, metrics=["accuracy"])

model.summary()

# ─────────────────────────────────────────────
#  Training Model
# ─────────────────────────────────────────────
print("\n" + "=" * 50)
print("  MULAI TRAINING MODEL")
print("=" * 50 + "\n")

model.fit(
    np.array(train_x),
    np.array(train_y),
    epochs=200,
    batch_size=5,
    verbose=1
)

# ─────────────────────────────────────────────
#  Simpan Model
# ─────────────────────────────────────────────
model.save("chatbot.keras")

print("\n" + "=" * 50)
print("  MODEL BERHASIL DISIMPAN!")
print("=" * 50)
print(f"  - Model    : chatbot.keras")
print(f"  - Words    : words.pkl ({len(words)} kata)")
print(f"  - Classes  : classes.pkl ({len(classes)} kelas)")
print("  DONE!")
