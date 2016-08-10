from enum import Enum
from typing import List, Any

import yass
from tutorial.base_types_external import Integer
from .... import contract


class Stock(contract.Instrument):
    def __init__(self) -> None:
        super().__init__()
        self.paysDividend = None  # type: bool
