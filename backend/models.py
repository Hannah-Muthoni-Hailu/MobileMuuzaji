from sqlalchemy import Column, Integer, String, ForeignKey, Table, Enum, DateTime, func
from sqlalchemy.orm import relationship
from database import Base
import enum
from pint import UnitRegistry

ureg = UnitRegistry()

class Units(enum.Enum):
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

organization_employees = Table(
    "organization_employees",
    Base.metadata,
    Column("employee_id", Integer, ForeignKey("users.id"), primary_key=True),
    Column("organization_id", Integer, ForeignKey("organization.id"), primary_key=True)
)

class User(Base):
    __tablename__ = "users"

    id = Column(Integer, primary_key=True, index=True)
    name = Column(String, nullable=False)
    email = Column(String, nullable=False)
    password = Column(String, nullable=False)

    # Relationships
    admin_status = relationship("Organization",  back_populates="admin", cascade="all, delete-orphan")
    employement_status = relationship("Organization", secondary=organization_employees, back_populates="employees")

class Organization(Base):
    __tablename__ = "organization"

    id = Column(Integer, primary_key=True, index=True)
    org_name = Column(String, nullable=False)
    admin_id = Column(Integer, ForeignKey("users.id"), nullable=False)

    admin = relationship("User", back_populates="admin_status")
    employees = relationship("User", secondary=organization_employees,  back_populates="employement_status")
    inv_items = relationship("Inventory", back_populates="organization")
    sales_items = relationship("Sales", back_populates="organization")

class Inventory(Base):
    __tablename__ = "inventory"

    id = Column(Integer, primary_key=True, index=True)
    item_name = Column(String, nullable=False)
    item_quantity = Column(Integer, nullable=False)
    unit = Column(Enum(Units), nullable=False)
    buying_price   = Column(Integer, nullable=False)
    selling_price  = Column(Integer, nullable=False)
    vat_percentage = Column(Integer, nullable=True)
    org_id = Column(Integer, ForeignKey("organization.id"), nullable=False)

    organization = relationship("Organization", back_populates="inv_items")

# For record keeping and is thus not connected to any inventory
class Sales(Base):
    __tablename__ = "sales"

    id = Column(Integer, primary_key=True, index=True)
    item_name = Column(String, nullable=False)
    item_quantity = Column(Integer, nullable=False)
    buying_price   = Column(Integer, nullable=False)
    selling_price  = Column(Integer, nullable=False)
    gross_income   = Column(Integer, nullable=False)
    profit         = Column(Integer, nullable=False)
    vat_amount     = Column(Integer, nullable=True)
    date = Column(DateTime, server_default=func.now())
    org_id = Column(Integer, ForeignKey("organization.id"), nullable=False)

    organization = relationship("Organization", back_populates="sales_items")