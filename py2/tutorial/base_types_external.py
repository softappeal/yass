# shows how to use contract external base types

import yass


class IntegerHandler(yass.BaseTypeHandler):
    def readBase(self, reader):
        return Integer(reader.readZigZagInt())

    def writeBase(self, value, writer):
        writer.writeZigZagInt(value.value)


class Integer:
    TYPE_DESC = yass.TypeDesc(yass.FIRST_DESC_ID, IntegerHandler())

    def __init__(self, value):
        # note: check if value is really a Java Integer should be implemented here
        self.value = value

    def __str__(self):
        return str(self.value)
