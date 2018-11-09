call activate py3

pip install mypy==0.641

python -m unittest test.all_tests

cmd /c mypy -p tutorial
cmd /c mypy -p test

call deactivate
