cmd /c node_modules\.bin\istanbul cover src\test\test-node.js
cmd /c node_modules\.bin\remap-istanbul -i ../build/coverage-js/coverage.json -o ../build/coverage-ts -t html
