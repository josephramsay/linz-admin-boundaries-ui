#!/bin/bash

export http_proxy=http://127.0.0.1:3128/
export https_proxy=http://127.0.0.1:3128/

path=/opt/tomcat8/webapps/ab/WEB-INF/scripts

#extract functions
update() {
        python $path/linz_admin_boundaries_uploader.py reject
        python $path/linz_admin_boundaries_uploader.py detect load
}


if [ "$1" = "test" ]
then
        echo "Test TBD"
else
        update
fi
exit
