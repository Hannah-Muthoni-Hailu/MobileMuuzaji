from sqlalchemy import create_engine
from sqlalchemy.orm import sessionmaker, DeclarativeBase
from dotenv import load_dotenv
import time
import os

load_dotenv(override=False)

DATABASE_URL = os.getenv('DATABASE_URL')

for i in range(10):
    try:
        engine = create_engine(DATABASE_URL, echo=True)
        engine.connect()
        print("Database connected successfully!")
        break
    except Exception as e:
        print(f"Database connection failed with error: {e}. Retring in 3 seconds ({i+1}/5)")
        time.sleep(5)
else:
    raise Exception("Could not connect to database after 5 retries")

SessionLocal = sessionmaker(bind=engine, autocommit=False, autoflush=False)

class Base(DeclarativeBase):
    pass

# Function for fetching the session
def get_db():
    with SessionLocal() as db:
        yield db