# When updating from one version of SVNKit to the next, it can be painful to update
# the local Savana maven setup files in this 'lib' directory.  Here are the steps
# to run to do so.  It keeps the existing subversion history.  There's no error
# checking, so it is suggested that these commands are run one-by-one using copy
# and paste so any errors are discovered immediately.


# Adjust these variable values to match your local system:

# location of org.tmatesoft.svn_N.M.L.standalone.zip, unzipped
SVNKIT_NEW_DIST=/opt/svnkit-1.3.1.6109

# location of org.tmatesoft.svn_N.M.L.maven.zip, unzipped:
SVNKIT_NEW_MAVEN=/opt/svnkit-1.3.1.6109/maven

SVNKIT_OLD_VERSION=1.3.0.5847
SVNKIT_NEW_VERSION=1.3.1.6109
JNA_OLD_VERSION=3.0.9
JNA_NEW_VERSION=3.1.0


# UPDATE THE SVNKIT FILES
#
#  ./org/tmatesoft/svnkit/svnkit/${SVNKIT_NEW_VERSION}/svnkit-${SVNKIT_NEW_VERSION}.pom
#  ./org/tmatesoft/svnkit/svnkit/${SVNKIT_NEW_VERSION}/svnkit-${SVNKIT_NEW_VERSION}-sources.jar
#  ./org/tmatesoft/svnkit/svnkit/${SVNKIT_NEW_VERSION}/svnkit-${SVNKIT_NEW_VERSION}.jar
#
# move the old directory so we keep svn history
svn mv org/tmatesoft/svnkit/svnkit/${SVNKIT_OLD_VERSION} org/tmatesoft/svnkit/svnkit/${SVNKIT_NEW_VERSION}
pushd org/tmatesoft/svnkit/svnkit/${SVNKIT_NEW_VERSION}
for ext in .jar -sources.jar .pom
do
  # move the old file so we keep svn history
  svn mv svnkit-${SVNKIT_OLD_VERSION}${ext} svnkit-${SVNKIT_NEW_VERSION}${ext}
  # remove the old file so we don't check it in accidentally
  rm svnkit-${SVNKIT_NEW_VERSION}${ext}
done
# copy the new files from the distribution files.  don't use the maven files for .jar (maven version has javahl which Savana doesn't need) or -sources.jar (it has an extra level in the directory structure that makes it harder to use in an IDE).
cp ${SVNKIT_NEW_DIST}/svnkit.jar svnkit-${SVNKIT_NEW_VERSION}.jar
unzip -p ${SVNKIT_NEW_MAVEN}/org.tmatesoft.svnkit-${SVNKIT_NEW_VERSION}-bundle.jar pom.xml > svnkit-${SVNKIT_NEW_VERSION}.pom
cp ${SVNKIT_NEW_DIST}/svnkitsrc.zip svnkit-${SVNKIT_NEW_VERSION}-sources.jar
popd


# UPDATE THE SVNKIT-CLI FILES
#
#  ./org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_NEW_VERSION}/svnkit-cli-${SVNKIT_NEW_VERSION}-sources.jar
#  ./org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_NEW_VERSION}/svnkit-cli-${SVNKIT_NEW_VERSION}.jar
#  ./org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_NEW_VERSION}/svnkit-cli-${SVNKIT_NEW_VERSION}.pom
#
# move the old directory so we keep svn history
svn mv org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_OLD_VERSION} org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_NEW_VERSION}
pushd org/tmatesoft/svnkit/svnkit-cli/${SVNKIT_NEW_VERSION}
for ext in .jar -sources.jar .pom
do
  # move the old file so we keep svn history
  svn mv svnkit-cli-${SVNKIT_OLD_VERSION}${ext} svnkit-cli-${SVNKIT_NEW_VERSION}${ext}
  # remove the old file so we don't check it in accidentally
  rm svnkit-cli-${SVNKIT_NEW_VERSION}${ext}
done
# copy the new files from the distribution files.  there is no maven bundling for sources.
cp ${SVNKIT_NEW_DIST}/svnkit-cli.jar svnkit-cli-${SVNKIT_NEW_VERSION}.jar
perl -pi -e "s/${SVNKIT_OLD_VERSION}/${SVNKIT_NEW_VERSION}/g" svnkit-cli-${SVNKIT_NEW_VERSION}.pom
cp ${SVNKIT_NEW_DIST}/svnkitclisrc.zip svnkit-cli-${SVNKIT_NEW_VERSION}-sources.jar
popd


# UPDATE THE JNA FILES
#
#  ./com/sun/jna/jna/${JNA_NEW_VERSION}/jna-${JNA_NEW_VERSION}-sources.jar
#  ./com/sun/jna/jna/${JNA_NEW_VERSION}/jna-${JNA_NEW_VERSION}.jar
#  ./com/sun/jna/jna/${JNA_NEW_VERSION}/jna-${JNA_NEW_VERSION}.pom
#
# move the old directory so we keep svn history
svn mv com/sun/jna/jna/${JNA_OLD_VERSION} com/sun/jna/jna/${JNA_NEW_VERSION}
pushd com/sun/jna/jna/${JNA_NEW_VERSION}
for ext in .jar -sources.jar .pom
do
  # move the old file so we keep svn history
  svn mv jna-${JNA_OLD_VERSION}${ext} jna-${JNA_NEW_VERSION}${ext}
  # remove the old file so we don't check it in accidentally
  rm jna-${JNA_NEW_VERSION}${ext}
done
# copy the new files from the distribution files.  there is no maven bundling for sources.
cp ${SVNKIT_NEW_DIST}/jna.jar jna-${JNA_NEW_VERSION}.jar
unzip -p ${SVNKIT_NEW_MAVEN}/com.sun.jna-${JNA_NEW_VERSION}-bundle.jar pom.xml > jna-${JNA_NEW_VERSION}.pom
cp ${SVNKIT_NEW_DIST}/jnaclisrc.zip jna-${JNA_NEW_VERSION}-sources.jar
# process the jna-n.n.n-sources.jar file to pull out just the Java source
unzip -j ${SVNKIT_NEW_MAVEN}/com.sun.jna-${JNA_NEW_VERSION}-bundle.jar jna-src.zip
unzip -q jna-src.zip -d temp
pushd temp/src
zip -9rq ../../jna-${JNA_NEW_VERSION}-sources.jar *
popd
rm -r temp jna-src.zip
popd


# UPDATE THE TOP-LEVEL POM FILE
#
perl -pi -e "s/${SVNKIT_OLD_VERSION}/${SVNKIT_NEW_VERSION}/g" ../pom.xml
perl -pi -e "s/${JNA_OLD_VERSION}/${JNA_NEW_VERSION}/g" ../pom.xml

