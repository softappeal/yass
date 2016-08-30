import unittest
from typing import cast, Any


def load_tests(loader, tests, pattern):
    return loader.discover('.', "*_test.py")


if __name__ == '__main__':
    cast(Any, unittest).main(verbosity=1)
