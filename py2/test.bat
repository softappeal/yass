call activate py2.7

pip install typing==3.5.2.2
pip install enum34==1.1.6
pip list

python -m unittest test.all_tests

call activate py3.5

pip install mypy-lang==0.4.4
pip list

cmd /c mypy --py2 --strict-optional --check-untyped-defs -p tutorial
cmd /c mypy --py2 --strict-optional --check-untyped-defs -p test
