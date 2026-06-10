import bcrypt
bcrypt.__about__ = bcrypt

from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session
from database import Base, engine, get_db
from models import User, Organization, Inventory, Sales
from pydantic import BaseModel, EmailStr, field_validator, model_validator
from enum import Enum
from pint import UnitRegistry
import logging

logger = logging.getLogger(__name__)

# Create tables
Base.metadata.create_all(bind=engine)

app = FastAPI()
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"]
)
ureg = UnitRegistry()

# Utilities
def hash_password(password: str) -> str:
    return bcrypt.hashpw(password.encode("utf-8"), bcrypt.gensalt()).decode("utf-8")

def verify_password(plain_password: str, hashed_password: str) -> bool:
    return bcrypt.checkpw(
        plain_password.encode("utf-8"),
        hashed_password.encode("utf-8"),
    )

# Models
class UserModel(BaseModel):
    name: str
    email: EmailStr
    password: str
    password_repeat: str

    @field_validator("name")
    @classmethod
    def name_must_not_be_empty(cls, v):
        if not v.strip():
            raise ValueError("Name cannot be empty")
        return v.strip()
    
    @field_validator("password")
    @classmethod
    def password_min_length(cls, v):
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        return v
    
    @model_validator(mode='after')
    def check_passwords_match(self):
        if self.password != self.password_repeat:
            raise ValueError('Passwords do not match')
        return self

class LoginModel(BaseModel):
    email: EmailStr
    password: str

    @field_validator("password")
    @classmethod
    def must_be_long(cls, v):
        if len(v) < 8:
            raise ValueError("Password must be at least 8 characters")
        return v

class NewOrgModel(BaseModel):
    admin_id: int
    org_name: str

class EmployeesModel(BaseModel):
    org_id: int
    employee_email: EmailStr

class Units(str, Enum):
    kilogram  = str(ureg.kilogram)
    gram      = str(ureg.gram)
    pound     = str(ureg.pound)
    ounce     = str(ureg.ounce)
    tonne     = str(ureg.metric_ton)
    litre       = str(ureg.liter)
    millilitre  = str(ureg.milliliter)
    gallon      = str(ureg.gallon)
    fluid_ounce = str(ureg.fluid_ounce)
    cup         = str(ureg.cup)

class InventoryItem(BaseModel):
    name: str
    quantity: int
    unit: Units
    cost_per_unit: int
    org_id: int

class EditInventoryItem(BaseModel):
    item_id: int
    name: str | None = None
    quantity: int | None = None
    unit: Units | None = None
    cost_per_unit: int | None = None
    org_id: int

class SaleModel(BaseModel):
    item_id: int
    quantity_sold: int

@app.get("/")
def root():
    return {"status": "ok"}

@app.post("/signup")
def signup(user: UserModel, db: Session = Depends(get_db)):
    try:
        db_user = db.query(User).filter(User.email == user.email).first()
    except Exception as e:
        print(e)
        raise HTTPException(status_code=500, detail="Internal server error")

    if not db_user:
        try:
            db_user = User(name=user.name, email=user.email, password=hash_password(user.password))
        except Exception as e:
            print(e)
            raise HTTPException(status_code=500, detail="Internal server error")

        db.add(db_user)
        db.commit()
        db.refresh(db_user)
        
        user_item = {
            "id": db_user.id,
            "name": db_user.name,
            "email": db_user.email,
            "admin_orgs": db_user.admin_status,
            "employee_orgs": db_user.employement_status
        }

        return {"message": f"User {db_user.name} created", "user": user_item}
    else:
        raise HTTPException(status_code=401, detail="User already exists. Please log in instead")

@app.post("/login")
def login(user: LoginModel, db: Session = Depends(get_db)):
    try:
        db_user = db.query(User).filter(User.email == user.email).first()
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

    if not db_user or not verify_password(user.password, db_user.password):
        raise HTTPException(status_code=401, detail="Invalid credentials")

    user_item = {
            "id": db_user.id,
            "name": db_user.name,
            "email": db_user.email,
            "admin_orgs": db_user.admin_status,
            "employee_orgs": db_user.employement_status
        }

    return {"message": f"User {db_user.name} logged in", "user": user_item}

@app.post("/new-org")
def newOrg(org: NewOrgModel, db: Session =  Depends(get_db)):
    try:
        db_org = Organization(
            org_name=org.org_name,
            admin_id=org.admin_id
        )

        db.add(db_org)
        db.commit()
        db.refresh(db_org)

        db_user = db.query(User).filter(User.id == org.admin_id).first()

        user_item = {
            "id": db_user.id,
            "name": db_user.name,
            "email": db_user.email,
            "admin_orgs": db_user.admin_status,
            "employee_orgs": db_user.employement_status
        }

        return {"message": f"New organization {db_org.org_name} created", "user": user_item}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

@app.get("/organization/{org_id}")
def getOrganization(org_id: int, db: Session = Depends(get_db)):
    organization = db.query(Organization).filter(Organization.id == org_id).first()

    if not organization:
        raise HTTPException(status_code=404, detail="Organization not found")

    organization_data = {
        "id": organization.id,
        "name": organization.org_name,
        "admin_id": organization.admin.id,
        "admin_name": organization.admin.name,
        "employees": organization.employees,
        "inv_items": organization.inv_items,
        "sales_items": organization.sales_items
    }
    
    return {"organization": organization_data}

@app.post("/new-employee")
def newEmployee(employee: EmployeesModel, db: Session = Depends(get_db)):
    try:
        employee_details = db.query(User).filter(User.email == employee.employee_email).first()
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

    if not employee_details:
        raise HTTPException(status_code=401, detail="No employee exists with that email")

    # Implement session management to maintain organization id
    try:
        organization = db.query(Organization).filter(Organization.id == employee.org_id).first()

        organization.employees.append(employee_details)
        db.commit()
        db.refresh(organization)

        inventory = organization.inv_items
        sales = organization.sales_items

        return {"message": "New employee added to your organization.", "organization": organization}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

@app.post("/new-product")
def newProduct(product: InventoryItem, db: Session = Depends(get_db)):
    try:
        inventory_item = Inventory(
            item_name=product.name,
            item_quantity=product.quantity,
            unit=product.unit,
            cost_per_unit=product.cost_per_unit,
            org_id=product.org_id # Use session handling to obtain this
        )

        db.add(inventory_item)
        db.commit()
        db.refresh(inventory_item)

        return { "item": inventory_item}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

@app.post("/edit-product")
def edit_product(product: EditInventoryItem, db: Session = Depends(get_db)):
    # Product ID will be sent to frontend for recovery
    try:
        inventory_item = db.query(Inventory).filter(Inventory.id == product.item_id).first()

        if product.name is not None: inventory_item.item_name = product.name
        if product.quantity is not None: inventory_item.item_quantity = product.quantity
        if product.unit is not None: inventory_item.unit = product.unit
        if product.cost_per_unit is not None: inventory_item.cost_per_unit = product.cost_per_unit

        db.commit()
        db.refresh(inventory_item)

        organization = db.query(Organization).filter(Organization.id == product.org_id).first()
        inventory = organization.inv_items
        sales = organization.sales_items

        return {"message": "item updated", "organization": organization}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")
    
@app.post("/sale")
def sale(saleitem: SaleModel, db: Session = Depends(get_db)):
    try:
        # Decrease quantity in inventory
        inventory_item = db.query(Inventory).filter(Inventory.id == saleitem.item_id).first()
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")
    
    if inventory_item.item_quantity - saleitem.quantity_sold < 1 :
        raise HTTPException(status_code=401, detail="Not enough stock")

    # Update inventory
    inventory_item.item_quantity -= saleitem.quantity_sold
    try:
        db.commit()
        db.refresh(inventory_item)
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")
    
    # Record a sale
    try:
        earnings = saleitem.quantity_sold * inventory_item.cost_per_unit

        sale_entry = Sales(
            item_name=inventory_item.item_name,
            item_quantity=saleitem.quantity_sold,
            earnings=earnings,
            org_id=inventory_item.org_id
        )

        db.add(sale_entry)
        db.commit()
        db.refresh(sale_entry)

        organization = db.query(Organization).filter(Organization.id == inventory_item.org_id).first()
        inventory = organization.inv_items
        sales = organization.sales_items

        return {"message": "Successful sale made", "organization": organization}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

@app.delete("delete-inventory")
def delete_inventory(id: int, db: Session = Depends(get_db)):
    try:
        inventory_item = db.query(Inventory).filter(Inventory.id == id.item_id).first()
        org_id = inventory_item.org_id

        db.delete(inventory_item)
        db.commit()

        organization = db.query(Organization).filter(Organization.id == org_id).first()
        inventory = organization.inv_items
        sales = organization.sales_items
        
        return {"message": "Inventory item deleted successfully"}
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")
