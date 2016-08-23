#!/bin/sh

action=$1

case "$action" in
        reject|prepare|transfer ) python download_admin_bdys.py $action;;
        * ) echo "INVALID Request $action";;
esac
