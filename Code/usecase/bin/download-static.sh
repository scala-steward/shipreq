#!/bin/bash

v=2.1.2
echo "Downloading v$v of normalize.css -- http://necolas.github.io/normalize.css/"
curl http://necolas.github.io/normalize.css/$v/normalize.css -o src/main/webapp/assets/normalize.css

