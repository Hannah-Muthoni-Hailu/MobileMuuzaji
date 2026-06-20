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
    buying_price:   int
    selling_price:  int
    vat_percentage: int | None = None
    org_id: int

class EditInventoryItem(BaseModel):
    item_id: int
    name: str | None = None
    quantity: int | None = None
    unit: Units | None = None
    cost_per_unit: int | None = None
    org_id: int

class UpdateInventoryRequest(BaseModel):
    item_name: str
    item_quantity: int
    unit: Units
    buying_price: int
    selling_price: int
    vat_percentage: int | None = None
    org_id: int

class SaleModel(BaseModel):
    item_id: int
    quantity_sold: int
    sale_price: int | None = None
    update_price: bool = False

class RemoveEmployeeRequest(BaseModel):
    org_id:      int
    employee_id: int

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

        org_result = {
            "id": organization.id,
            "name": organization.org_name,
            "admin_id": organization.admin.id,
            "admin_name": organization.admin.name,
            "employees": []
        }

        for employee in organization.employees:
            emp_result = {
                "id": employee.id,
                "name": employee.name,
                "email": employee.email
            }
            org_result["employees"].append(emp_result)

        return org_result
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")

@app.post("/new-product")
def newProduct(product: InventoryItem, db: Session = Depends(get_db)):
    try:
        inventory_item = Inventory(
            item_name      = product.name,
            item_quantity  = product.quantity,
            unit           = product.unit,
            buying_price   = product.buying_price,
            selling_price  = product.selling_price,
            vat_percentage = product.vat_percentage,
            org_id         = product.org_id
        )
        db.add(inventory_item)
        db.commit()
        db.refresh(inventory_item)
        return inventory_item
    except Exception as e:
        logger.error(f"Database/Server failure: {e}", exc_info=True)
        raise HTTPException(status_code=500, detail="Internal Server error")


@app.put("/update-product/{item_id}")
def update_product(item_id: int, item: UpdateInventoryRequest, db: Session = Depends(get_db)):
    db_item = db.query(Inventory).filter(Inventory.id == item_id).first()

    if not db_item:
        raise HTTPException(status_code=404, detail="Item not found")

    db_item.item_name      = item.item_name
    db_item.item_quantity  = item.item_quantity
    db_item.unit           = item.unit
    db_item.buying_price   = item.buying_price
    db_item.selling_price  = item.selling_price
    db_item.vat_percentage = item.vat_percentage

    db.commit()
    db.refresh(db_item)
    return db_item


@app.post("/sale")
def sale(saleitem: SaleModel, db: Session = Depends(get_db)):
    try:
        inventory_item = (
            db.query(Inventory)
            .filter(Inventory.id == saleitem.item_id)
            .with_for_update()
            .first()
        )

        if not inventory_item:
            raise HTTPException(status_code=404, detail="Item not found")

        if inventory_item.item_quantity - saleitem.quantity_sold < 1:
            raise HTTPException(status_code=400, detail="Not enough stock")

        # Use provided sale price or fall back to selling_price
        effective_selling_price = saleitem.sale_price \
            if saleitem.sale_price is not None \
            else inventory_item.selling_price

        # Calculate financials
        gross_income = effective_selling_price * saleitem.quantity_sold
        cost         = inventory_item.buying_price * saleitem.quantity_sold
        profit       = gross_income - cost

        # Calculate VAT if applicable
        vat_amount = None
        if inventory_item.vat_percentage is not None:
            vat_amount = int((gross_income * inventory_item.vat_percentage) / 100)

        # Deduct from inventory
        inventory_item.item_quantity -= saleitem.quantity_sold

        # Update selling price if user chose to save it
        if saleitem.update_price and saleitem.sale_price is not None:
            inventory_item.selling_price = saleitem.sale_price

        # Create the sale record
        new_sale = Sales(
            item_name     = inventory_item.item_name,
            item_quantity = saleitem.quantity_sold,
            buying_price  = inventory_item.buying_price,
            selling_price = effective_selling_price,
            gross_income  = gross_income,
            profit        = profit,
            vat_amount    = vat_amount,
            org_id        = inventory_item.org_id
        )
        db.add(new_sale)

        db.commit()
        db.refresh(new_sale)
        db.refresh(inventory_item)

        return new_sale

    except HTTPException:
        db.rollback()
        raise
    except Exception as e:
        db.rollback()
        logger.error(f"Sale transaction failed: {e}", exc_info=True)
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

@app.delete("/remove-employee")
def remove_employee(request: RemoveEmployeeRequest, db: Session = Depends(get_db)):
    organization = db.query(Organization).filter(Organization.id == request.org_id).first()
    if not organization:
        raise HTTPException(status_code=404, detail="Organization not found")

    employee = db.query(User).filter(User.id == request.employee_id).first()
    if not employee:
        raise HTTPException(status_code=404, detail="Employee not found")

    if employee not in organization.employees:
        raise HTTPException(status_code=400, detail="Employee is not in this organization")

    organization.employees.remove(employee)
    db.commit()

    org_result = {
            "id": organization.id,
            "name": organization.org_name,
            "admin_id": organization.admin.id,
            "admin_name": organization.admin.name,
            "employees": []
        }

    for employee in organization.employees:
        emp_result = {
            "id": employee.id,
            "name": employee.name,
            "email": employee.email
        }
        org_result["employees"].append(emp_result)

    return org_result