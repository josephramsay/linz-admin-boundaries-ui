#!/bin/sh

#Simple stub script used to test whether ProcessControl will run/read-from bash

echo $1 | awk '{print toupper($0)}'
