from sqlalchemy import create_engine, text
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


def ensure_inventory_unit_column_is_text():
    if not DATABASE_URL:
        return

    with engine.begin() as conn:
        dialect_name = conn.dialect.name
        if dialect_name != "postgresql":
            return

        try:
            conn.execute(text("""
                ALTER TABLE inventory
                ALTER COLUMN unit TYPE VARCHAR
                USING unit::text
            """))
        except Exception as exc:
            message = str(exc).lower()
            if "does not exist" in message or "already" in message or "cannot alter" in message:
                return
            raise

# Function for fetching the session
def get_db():
    with SessionLocal() as db:
        yield db