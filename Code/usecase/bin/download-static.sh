#!/bin/bash

v=2.1.2
echo "Downloading v$v of normalize.css -- http://necolas.github.io/normalize.css/"
curl -s http://necolas.github.io/normalize.css/$v/normalize.css -o src/main/webapp/assets/normalize.css

v=2.2.1
echo "Downloading v$v of knockout.js -- http://knockoutjs.com/"
curl -s http://knockoutjs.com/downloads/knockout-$v.js -o src/main/javascript/vendor/knockout.js

v=2.4.1
echo "Downloading v$v of ko mapping -- https://github.com/SteveSanderson/knockout.mapping/tree/master/build/output"
curl -s https://raw.github.com/SteveSanderson/knockout.mapping/$v/build/output/knockout.mapping-latest.js -o src/main/javascript/vendor/knockout-mapping.js
