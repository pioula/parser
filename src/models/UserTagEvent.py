from datetime import datetime
from dataclasses import dataclass, Field
from typing import Optional
from .Device import Device
from .Action import Action
from .Product import Product
from pydantic import BaseModel


@dataclass
class UserTagEvent(BaseModel):
    time: datetime
    cookie: str
    country: str
    device: Device
    action: Action
    origin: str
    product_info: Product

    def to_dict(self):
        return {
            "time": self.time.isoformat(),
            "cookie": self.cookie,
            "country": self.country,
            "device": self.device.value,
            "action": self.action.value,
            "origin": self.origin,
            "product_info": {
                "product_id": self.product_info.product_id,
                "brand_id": self.product_info.brand_id,
                "category_id": self.product_info.category_id,
                "price": self.product_info.price
            }
        }