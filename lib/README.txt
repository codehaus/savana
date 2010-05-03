# When updating from one version of SVNKit to the next, it can be painful to update
# the local Savana maven setup files in this 'lib' directory.  Here are the steps
# to run to do so.  It keeps the existing subversion history.  There's no error
# checking, so it is suggested that these commands are run one-by-one using copy
# and paste so any errors are discovered immediately.


# Adjust these variable values to match your local system:

# location of org.tmatesoft.svn_N.M.L.standalone.zip, unzipped
SVNKIT_NEW_DIST=/opt/svnkit-1.3.2.6267

SVNKIT_OLD_VERSION=1.3.1
SVNKIT_NEW_VERSION=1.3.2


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
  # remove the old file so we don't check it in accidentally (except preserve .pom file)
  [ $ext != .pom ] && rm svnkit-cli-${SVNKIT_NEW_VERSION}${ext}
done
# copy the new files from the distribution files.  there is no maven bundling for sources.
cp ${SVNKIT_NEW_DIST}/svnkit-cli.jar svnkit-cli-${SVNKIT_NEW_VERSION}.jar
perl -pi -e "s/${SVNKIT_OLD_VERSION}/${SVNKIT_NEW_VERSION}/g" svnkit-cli-${SVNKIT_NEW_VERSION}.pom
cp ${SVNKIT_NEW_DIST}/svnkitclisrc.zip svnkit-cli-${SVNKIT_NEW_VERSION}-sources.jar
popd


# UPDATE THE TOP-LEVEL POM FILE
#
perl -pi -e "s/${SVNKIT_OLD_VERSION}/${SVNKIT_NEW_VERSION}/g" ../pom.xml
