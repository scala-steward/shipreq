#!/bin/bash

sd=src/main/sass
td=src/main/webapp/css

for f in $sd/*.s?ss; do
  sf=${f##*/}
  tf=${sf%.s?ss}.css
  s=$sd/$sf
  t=$td/$tf
  echo "$s -> $t"
  style=compact
  [ "$tf" == 'all.css' ] && style=compressed
  sass $s --style $style > $t
done

echo
ls -l $td
