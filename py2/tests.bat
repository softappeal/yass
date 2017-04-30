call activate py2

pip install typing==3.6.1
pip install enum34==1.1.6

python -m unittest test.all_tests

call activate py3

cmd /c mypy -p tutorial
cmd /c mypy -p test
