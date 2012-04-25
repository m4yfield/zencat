#!/bin/sh
#
# Run zencat.
#

cp="zencat.jar"
for jar in libs/*.jar
do
    cp="$cp:$jar"
done

exec java -cp "build/:$cp" com.ls.zencat.ZenCat "${1:-zencat.xml}"
