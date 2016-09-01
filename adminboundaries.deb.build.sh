#!/bin/bash

setup=true
project=AdminBoundaries
proj_abv=dab
proj_lcase="${project,,}"
proj_temp="$proj_lcase"_temp
release="1.0"
upstream="$proj_lcase"_$release.orig.tar

ddate=`date --rfc-2822`
desc1="Downloads and imports AdminBoundary shapefile data"
desc2="Internal download and import script for AdminBoundary data as supplied by external agencies."
pydir="$project/scripts"
#exculde filled ini
pybins="download_admin_bdys.py download_admin_bdys.sh"
#files in this list will be coped from a matching .template file (TODO)
pyrepl="download_admin_bdys.ini"
wardir="$project/build/libs"
warlist="ab.war"
debdir="debian"

#-----------------------
function setupdebian {
	cd $1
	mkdir $debdir
	
	# CHANGELOG
	#dch --create -v 1.0-1 --package $proj_lcase
	cat << EOF > $debdir/changelog
$proj_lcase ($release) UNRELEASED; urgency=low

  * Initial release. (Closes: #XXXXXX)

 -- $DEBFULLNAME <$DEBEMAIL>  $ddate

EOF
	# RULES
	cat << EOF > debian/rules 
#!/usr/bin/make -f
export DH_VERBOSE = 1
%:
	dh \$@
	
EOF

	# CONTROL
	cat << EOF > $debdir/control 
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
	cat << EOF > $debdir/copyright
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
       notice, this list of conditions and the following disclaimer in the
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
	echo 9 > $debdir/compat
	
	# INSTALL
	cat << EOF > $debdir/install 
data/$proj_abv.desktop usr/share/applications
data/$proj_abv.png usr/share/pixmaps
scripts/download_admin_bdys.sh usr/bin
scripts/download_admin_bdys.py usr/bin
scripts/$pyrepl usr/conf
build/libs/ab.war $CATALINA_BASE/webapps
EOF

	#-------------------
	mkdir $debdir/source
	
	# SOURCE/FORMAT
	cat << EOF > $debdir/source/format
3.0 (quilt)
EOF

	# SOURCE/INC-BIN
	cat << EOF > $debdir/source/include-binaries
$debdir/$proj_lcase/usr/share/pixmaps/$proj_abv.png
$debdir/$proj_lcase/usr/share/doc/$proj_lcase/changelog.gz
EOF

	cd ..
}
#-----------------------


function appwrapper {
	# DESKTOP
	mkdir $proj_temp/data
	cp $project/"$proj_abv".png $proj_temp/data/
	cat << EOF > $proj_temp/data/$proj_abv.desktop
[Desktop Entry]
Name=$project
Comment=Trigger download and import of $project
Type=Application
Keywords=$proj_abv
Exec=/usr/bin/download_admin_bdys.sh prepare
Terminal=true
Icon=/usr/share/pixmaps/$proj_abv.png
Categories=Utility;
EOF

}


echo
echo "*** Debian Package Builder for $project ***"
echo

cd ~/git/
pwd

echo "* 1. setup upstream package"

mkdir $proj_temp
mkdir $proj_temp/scripts

for p in $pybins
do
	cp -R $pydir/$p $proj_temp/scripts
done
cp -R $pydir/$pyrepl.template $proj_temp/scripts/$pyrepl
cp -R $wardir $proj_temp


if [ $setup ]
then
	echo "* 1b. setup debian"
	setupdebian $proj_temp
	appwrapper
fi

echo "* 2. archive upstream package"

tar -cf $upstream $proj_temp
gzip $upstream

#mkdir "$project"_temp
#cd "$project"_temp
#tar -xvf ../$upstream

cd $proj_temp
echo "* 3. build debian package"
debuild -us -uc
echo "* 4. tidy up"
