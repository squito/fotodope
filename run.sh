#!/bin/bash

export LIBS=../Libraries/lib/
export CLASSPATH="./bin/:../CommonUtils/bin:../TagDag/bin/:$LIBS/flickrapi-1.1.jar:$LIBS/h2-1.1.103.jar:$LIBS/sanselan-0.94-incubator.jar:$LIBS/postgresql-8.2-504.jdbc3.jar:$LIBS/jheader-0.1.jar:$LIBS/TableLayout-bin-jdk1.5-2007-04-21.jar"

echo $CLASSPATH

java -Xms256M -Xmx256M -cp $CLASSPATH us.therashids.PictureDB.gui.TestFrame

