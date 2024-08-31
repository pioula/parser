from enum import Enum
from pydantic import BaseModel

class Device(Enum):
    MOBILE = "MOBILE"
    PC = "PC"
    TV = "TV"