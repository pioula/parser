from dataclasses import dataclass
from typing import Optional
from pydantic import BaseModel, Field


@dataclass
class Product(BaseModel):
    product_id: int
    brand_id: Optional[str]
    category_id: Optional[str]
    price: int