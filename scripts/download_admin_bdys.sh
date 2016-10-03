#!/bin/sh

action=$1
pwddir=`pwd`
subdir=scripts

cd $subdir

case "$action" in
        load|map|transfer|reject ) 
			echo "Running action $action in $pwddir/$subdir"
        	python $subdir/download_admin_bdys.py $action
		;;
        * ) 
        	echo "Unknown request $action"
		;;
esac
