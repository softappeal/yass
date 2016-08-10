pip install mypy-lang==0.4.3
pip list

python -m unittest test.all_tests

python -m mypy --check-untyped-defs test/contract_test.py test/serialize_test.py test/service_test.py tutorial/socket_client.py tutorial/socket_server.py tutorial/std_server.py
