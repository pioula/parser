from typing import List
from pydantic import BaseModel

class AggregatesQueryResult(BaseModel):
    def __init__(self, columns: List[str] = None, rows: List[List[str]] = None):
        self._columns = columns if columns is not None else []
        self._rows = rows if rows is not None else []

    @property
    def columns(self) -> List[str]:
        return self._columns

    @columns.setter
    def columns(self, value: List[str]) -> None:
        self._columns = value

    @property
    def rows(self) -> List[List[str]]:
        return self._rows

    @rows.setter
    def rows(self, value: List[List[str]]) -> None:
        self._rows = value