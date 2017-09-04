#!/bin/bash

setup=true
project=AdminBoundaries
proj_abv=dab
proj_lcase="${project,,}"
proj_temp="$proj_lcase"_temp
release="1.0"
upstream="$proj_lcase"_$release.orig.tar
org=linz

ddate=`date --rfc-2822`
desc1="Downloads and imports AdminBoundary shapefile data"
desc2="Internal download and import script for AdminBoundary data as supplied by external agencies."

#root_dir=`pwd`
root_dir=~/git
temp_dir=$root_dir
temp_dir=/tmp

#code source dir
src_dir=$root_dir/$project
#project build dir
###proj_dir=$root_dir/$proj_temp
proj_dir=$temp_dir/$project-$release

#code source for python scripts
python_dir="$src_dir/scripts"
#code source for web app
war_dir="$src_dir/build/libs"
#debian project sub dir where debian files are built
debian_dir="$proj_dir/debian"
data_dir="$proj_dir/data"
#installation dir where package is extracted
install_dir="/usr/local/share/$project"

war_list="ab.war"
#exculde filled ini
script_list="download_admin_bdys.py download_admin_bdys.sh"
#files in this list will be coped from a matching .template file (TODO)
conf_replace="download_admin_bdys.ini"

#---------------------------------------------------------------------

if [ $CATALINA_BASE ]
then
	cbase=$CATALINA_BASE
else
	#cbase=/opt/apache-tomcat/webapps
	cbase=/var/lib/tomcat/webapps
	echo "*WARN* Please set CATALINA_BASE, defaulting to $cbase"
fi

#---------------------------------------------------------------------

function setupdebian {
	# DEBIAN
	deb_dir=$1
	mkdir $deb_dir
	
	# CHANGELOG
	#dch --create -v 1.0-1 --package $proj_lcase
	cat << EOF > $deb_dir/changelog
$proj_lcase ($release) UNRELEASED; urgency=low

  * Initial release. (Closes: #XXXXXX)

 -- $DEBFULLNAME <$DEBEMAIL>  $ddate

EOF

	# RULES
	cat << EOF > $deb_dir/rules 
#!/usr/bin/make -f
export DH_VERBOSE = 1
%:
	dh \$@
	
override_dh_usrlocal:
	
EOF

	# CONTROL
	cat << EOF > $deb_dir/control 
Source: $proj_lcase
Maintainer: $DEBFULLNAME <$DEBEMAIL>
Section: misc
Priority: optional
Standards-Version: 3.9.3
Build-Depends: debhelper (>= 9)
Homepage: http://www.linz.govt.nz/

Package: $proj_lcase
Architecture: all
Multi-Arch: foreign
Depends: ${shlibs:Depends}, ${misc:Depends}
Description: $desc1
 $desc2 
EOF

	# COPYRIGHT
	cat << EOF > $deb_dir/copyright
Copyright:

    Copyright 2011 Crown copyright (c) Land Information New Zealand and the New
    Zealand Government. All rights reserved

License:

    This software is provided as a free download under the 3-clause BSD License
    as follows:
    
    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions are met:
    
    1. Redistributions of source code must retain the above copyright notice, this
       list of conditions and the following disclaimer. 
    2. Redistributions in binary form must reproduce the above copyright
       notice, this list of conditions and the deb_dirfollowing disclaimer in the
       documentation and/or other materials provided with the distribution.
    3. Neither the name of Land Information New Zealand nor any of its contributors
       may be used to endorse or promote products derived from this software
       without specific prior written permission.
    
    THIS SOFTWARE IS PROVIDED BY LAND INFORMATION NEW ZEALAND AND CONTRIBUTORS
    "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
    THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
    ARE DISCLAIMED. IN NO EVENT SHALL LAND INFORMATION NEW ZEALAND OR THE NEW
    ZEALAND GOVERNMENT OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
    INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
    LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
    PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
    LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
    OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
    ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
EOF

	# COMPAT
	echo 9 > $deb_dir/compat
	
	# INSTALL
	ins_dir=${install_dir#"/"}
	cat << EOF > $deb_dir/install 
data/$proj_abv.desktop usr/share/applications
data/$proj_abv.png usr/share/pixmaps
scripts/download_admin_bdys.sh $ins_dir
scripts/download_admin_bdys.py $ins_dir
scripts/$conf_replace $ins_dir
libs/$war_list ${cbase#"/"}
EOF

	#-------------------
	mkdir $deb_dir/source
	
	# SOURCE/FORMAT
	cat << EOF > $deb_dir/source/format
3.0 (quilt)
EOF

	# SOURCE/INC-BIN
	cat << EOF > $deb_dir/source/include-binaries
data/$proj_abv.png
libs/$war_list
EOF

}

#---------------------------------------------------------------------


function setupdesktop {
	# DESKTOP
	dat_dir=$1
	mkdir $dat_dir
	cp "$src_dir/src/main/webapp/$org.$proj_abv.png" "$dat_dir/$proj_abv.png"
	cat << EOF > $dat_dir/$proj_abv.desktop
[Desktop Entry]
Name=$project
Comment=Trigger download and import of $project
Type=Application
Keywords=$proj_abv
StartupNotify=true
Exec=$install_dir/download_admin_bdys.py
Path=$install_dir/
Terminal=false
Icon=$dat_dir/$proj_abv.png
Categories=Utility;
EOF
}

function clean {
	rm -R $proj_dir
	rm $temp_dir/$proj_lcase*
}

#---------------------------------------------------------------------

# ***************
# *  *********  *
# *  * START *  *
# *  *********  *
# ***************

echo
echo "*** Debian Package Builder for $project ***"
echo

clean

cd $root_dir

echo "* 1. setup upstream package"

mkdir $proj_dir
mkdir $proj_dir/scripts

for p in $script_list
do
	cp -R $python_dir/$p $proj_dir/scripts
done

cp -R $python_dir/$conf_replace.template $proj_dir/scripts/$conf_replace
cp -R $war_dir $proj_dir

if [ $setup ]
then
	echo "* 1a. setup debian in $debian_dir"
	setupdebian $debian_dir
	echo "* 1b. setup desktop in $data_dir"
	setupdesktop $data_dir
fi

echo "* 2. archive upstream package"
pwd
tar -cf $temp_dir/$upstream $proj_dir
gzip $temp_dir/$upstream

#now unpack it all again?
#rm -R $proj_dir
#gunzip -k "$temp_dir/$upstream.gz"
#tar -C / -xf $temp_dir/$upstream


cd $proj_dir

echo "* 3. build debian package"
dpkg-buildpackage -b
#debuild -us -uc
echo "* 4. tidy up"
