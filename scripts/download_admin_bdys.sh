#!/bin/sh

action=$1

case "$action" in
        load|reject|prepare|transfer ) 
        	python download_admin_bdys.py $action;;
        * ) 
        	echo "Empty request. Opening dialog"
			python download_admin_bdys.py
			;;
esac
