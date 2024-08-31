from dataclasses import dataclass, field
from typing import Optional
from pydantic import BaseModel


@dataclass
class Product(BaseModel):
    _product_id: int = field(default=0, metadata={"json_property": "product_id"})
    _brand_id: Optional[str] = field(default=None, metadata={"json_property": "brand_id"})
    _category_id: Optional[str] = field(default=None, metadata={"json_property": "category_id"})
    _price: int = field(default=0, metadata={"json_property": "price"})

    @property
    def product_id(self) -> int:
        return self._product_id

    @product_id.setter
    def product_id(self, value: int) -> None:
        self._product_id = value

    @property
    def brand_id(self) -> Optional[str]:
        return self._brand_id

    @brand_id.setter
    def brand_id(self, value: Optional[str]) -> None:
        self._brand_id = value

    @property
    def category_id(self) -> Optional[str]:
        return self._category_id

    @category_id.setter
    def category_id(self, value: Optional[str]) -> None:
        self._category_id = value

    @property
    def price(self) -> int:
        return self._price

    @price.setter
    def price(self, value: int) -> None:
        self._price = value