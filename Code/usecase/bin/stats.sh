#!/bin/bash

echo "main.html       = $(find src/main -name "*.html" | xargs wc -l | tail -1 | perl -pe 's/\D+//g')"
echo "main.javascript = $(find src/main/javascript -type f | fgrep -v vendor | xargs wc -l | tail -1 | perl -pe 's/\D+//g')"
echo "main.sass       = $(find src/main/sass -type f | xargs wc -l | tail -1 | perl -pe 's/\D+//g')"
echo "main.scala      = $(find src/main/scala -type f | xargs wc -l | tail -1 | perl -pe 's/\D+//g')"
echo "test.scala      = $(find src/test/scala -type f | xargs wc -l | tail -1 | perl -pe 's/\D+//g')"
echo "main.TODOs      = $(find src/main -type f | egrep -v '/_scalate/|vendor' | xargs fgrep TODO | wc -l)"
echo "test.TODOs      = $(find src/test -type f | egrep -v '/_scalate/|vendor' | xargs fgrep TODO | wc -l)"

