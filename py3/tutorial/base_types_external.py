# shows how to use contract external base types

import yass


class IntegerHandler(yass.BaseTypeHandler):
    def readBase(self, reader: yass.Reader) -> 'Integer':
        return Integer(reader.readZigZagInt())

    def writeBase(self, value: 'Integer', writer: yass.Writer) -> None:
        writer.writeZigZagInt(value.value)


class Integer:
    TYPE_DESC = yass.TypeDesc(yass.FIRST_DESC_ID, IntegerHandler())

    def __init__(self, value: int) -> None:
        # note: check if value is really a Java Integer should be implemented here
        self.value = value

    def __str__(self) -> str:
        return str(self.value)
