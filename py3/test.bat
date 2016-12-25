call activate py3

pip install mypy-lang==0.4.6

python -m unittest test.all_tests

cmd /c mypy -p tutorial
cmd /c mypy -p test
