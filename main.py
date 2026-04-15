from fastapi import FastAPI
from routers.authRouter import router as auth_router
from routers.conversationRouter import router as conversationRouter
from services.databaseConnection import MongoDB, SQLite

import sqlite3
# instance app with FastAPI
app = FastAPI(
    title="AILY E-commerce Chatbot",
    description="Chatbot service AILY"
)

db = SQLite()
db.createTable()

# connect to auth router
app.include_router(auth_router)
# connect to faq router
app.include_router(conversationRouter)

# entry point here la
if __name__ == "__main__":
    import uvicorn
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)