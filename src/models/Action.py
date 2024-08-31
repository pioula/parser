from enum import Enum
from pydantic import BaseModel

class Action(Enum):
    VIEW = "VIEW"
    BUY = "BUY"