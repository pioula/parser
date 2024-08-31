from typing import List, Optional
from .UserTagEvent import UserTagEvent
from pydantic import BaseModel

class UserProfileResult(BaseModel):
    cookie: str
    views: List[UserTagEvent]
    buys: List[UserTagEvent]