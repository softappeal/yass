from enum import Enum
from typing import List, Any, cast

import yass
from tutorial.base_types_external import Integer
from .... import contract


class Stock(contract.Instrument):
    def __init__(self):  # type: () -> None
        contract.Instrument.__init__(self)
        self.paysDividend = cast('bool', None)  # type: bool
