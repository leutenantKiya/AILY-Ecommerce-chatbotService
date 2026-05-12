# AILY E-commerce Chatbot Backend

FastAPI backend service for the AILY E-commerce Chatbot.

## Setup

1. Install dependencies:
```bash
pip install -r requirements.txt
```

2. Configure environment variables in `.env`:
```
MONGO_URI=mongodb://your-connection-string
MONGO_DB=aily
```

## Running the Server

From the `backend` directory:
```bash
python main.py
```

Or using uvicorn directly:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

The API will be available at `http://localhost:8000`

Swagger UI: `http://localhost:8000/docs`

## Project Structure

- `auth/` - Authentication and key validation
- `NLP/` - Natural Language Processing handlers
- `routers/` - API route handlers
- `services/` - Database connections and business logic
- `utils/` - Utility functions
- `data/` - Data files and configurations
- `prepare/` - Data preparation scripts
- `reverts/` - Revert/rollback scripts
