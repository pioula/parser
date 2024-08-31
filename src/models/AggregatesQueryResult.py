from typing import List
from pydantic import BaseModel

class AggregatesQueryResult(BaseModel):
    def __init__(self, columns: List[str] = None, rows: List[List[str]] = None):
        self.columns = columns if columns is not None else []
        self.rows = rows if rows is not None else []