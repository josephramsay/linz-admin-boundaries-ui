#!/bin/sh

bdir=`pwd`
adir=/opt/tomcat8/webapps/ab/WEB-INF/scripts

python $adir/download_admin_bdys.py load detect
