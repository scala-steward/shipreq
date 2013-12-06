#!/bin/bash

cd "$(dirname "$0")/.."
unzip -oj vendor/bootstrap.zip 'js/bootstrap.js' -d src/main/javascript/vendor \
  && unzip -oj vendor/bootstrap.zip 'css/bootstrap.css' -d src/main/sass/vendor \
  && echo && bin/generate-css.sh

