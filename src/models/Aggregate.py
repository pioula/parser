from enum import Enum
from pydantic import BaseModel

class Aggregate(Enum):
    COUNT = "COUNT"
    SUM_PRICE = "SUM_PRICE"