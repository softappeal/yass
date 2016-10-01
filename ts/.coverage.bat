xcopy yass.*       node_modules\yass /I /Y
xcopy package.json node_modules\yass /I /Y

cmd /c node_modules\.bin\istanbul cover test\test-node.js

cmd /c node_modules\.bin\remap-istanbul -i ../build/coverage-js/coverage.json -o ../build/coverage-ts -t html
