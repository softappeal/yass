call activate py3.5

pip install mypy-lang==0.4.4
pip list

python -m unittest test.all_tests

cmd /c mypy --strict-optional --check-untyped-defs -p tutorial
cmd /c mypy --strict-optional --check-untyped-defs -p test
