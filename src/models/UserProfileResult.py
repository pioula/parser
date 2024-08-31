from typing import List, Optional
from .UserTagEvent import UserTagEvent
from pydantic import BaseModel

class UserProfileResult(BaseModel):
    def __init__(self, cookie: Optional[str] = None, views: Optional[List[UserTagEvent]] = None, buys: Optional[List[UserTagEvent]] = None):
        self._cookie: Optional[str] = cookie
        self._views: List[UserTagEvent] = views if views is not None else []
        self._buys: List[UserTagEvent] = buys if buys is not None else []

    @property
    def cookie(self) -> Optional[str]:
        return self._cookie

    @cookie.setter
    def cookie(self, value: Optional[str]) -> None:
        self._cookie = value

    @property
    def views(self) -> List[UserTagEvent]:
        return self._views

    @views.setter
    def views(self, value: List[UserTagEvent]) -> None:
        self._views = value

    @property
    def buys(self) -> List[UserTagEvent]:
        return self._buys

    @buys.setter
    def buys(self, value: List[UserTagEvent]) -> None:
        self._buys = value