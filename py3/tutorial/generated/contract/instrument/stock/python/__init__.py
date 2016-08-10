from enum import Enum
from typing import List, Any

import yass
from tutorial.base_types_external import Integer
from .....contract import instrument as contract_instrument
from .....contract.instrument import stock as contract_instrument_stock


class PythonBond(contract_instrument.Bond):
    def __init__(self) -> None:
        super().__init__()


class PythonStock(contract_instrument_stock.Stock):
    def __init__(self) -> None:
        super().__init__()
