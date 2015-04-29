#!/bin/bash

# script to run a jenkins job for versionwatch

#################################################################

# configuration / defaults

# where is maven?
if [[ ! ${NATIVE_TOOLS} ]]; then NATIVE_TOOLS=/qa/tools/opt; fi
JAVA_HOME=${NATIVE_TOOLS}/jdk1.8.0_last
M2_HOME=/qa/tools/opt/apache-maven-3.1.1 # don't use NATIVE_TOOLS because it might be /qa/tools/opt/amd64 and there's no /qa/tools/opt/amd64/apache-maven-3.1.1
PATH=$PATH:$M2_HOME/bin:$JAVA_HOME/bin
MVN=${M2_HOME}/bin/mvn

# default upstream job = devstudio.product_master, devstudio.product_8.0.luna, etc.
UPSTREAM_JOB=""

# CSV list of additional installer jars to use. Will also use list of installers in install.jbds.list.txt.
# If the target folder already exists, installation will be skipped.<br/>
#  eg., /qa/services/http/binaries/RHDS/builds/stable/8.0.0.GA-build-core/jboss-devstudio-8.0.0.GA-v20141020-1042-B317-installer-standalone.jar
JBDS_INSTALLERS=

# Location where JBDS installations will be put
INSTALL_FOLDER=/home/hudson/static_build_env/jbds/versionwatch/installations

# To generate a report containing fewer bundles/features, set a regex that will match only those you want in the report, eg., .*(hibernate|jboss|xulrunner).* or match everything with .*
INCLUDE_IUS=".*(hibernate|jboss|xulrunner).*"

FROM=${WORKSPACE}/sources
SNAPSHOTS=tools@filemgmt.jboss.org:/downloads_htdocs/tools/mars/snapshots/builds/
DEST=${SNAPSHOTS}/${JOB_NAME}
URL=http://download.jboss.org/jbosstools/mars/snapshots/builds/${JOB_NAME}
DESCRIPTION=""

# file from which to pull a list of JBDS installers to install
JBDS_INSTALLERS_LISTFILE=${FROM}/install.jbds.list.txt

# include and exclude patterns for which JBDS installs to use when producing the version diff report
INCLUDE_VERSIONS="\d+\.\d+\.\d+"
EXCLUDE_VERSIONS=""
INCLUDE_IUS=".*"
EXCLUDE_IUS=""
others=""

# read commandline args
while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-JAVA_HOME') JAVA_HOME="$2"; shift 1;;
    '-M2_HOME') M2_HOME="$2"; shift 1;;
    '-UPSTREAM_JOB') UPSTREAM_JOB="$2"; shift 1;;
    '-JBDS_INSTALLERS') JBDS_INSTALLERS="$2"; shift 1;;
    '-INSTALL_FOLDER') INSTALL_FOLDER="$2"; shift 1;;
    '-JBDS_INSTALLER_NIGHTLY_FOLDER') JBDS_INSTALLER_NIGHTLY_FOLDER="$2"; shift 1;;
    '-JBDS_INSTALLERS_LISTFILE') JBDS_INSTALLERS_LISTFILE="$2"; shift 1;;
    '-INCLUDE_VERSIONS') INCLUDE_VERSIONS="$2"; shift 1;;
    '-EXCLUDE_VERSIONS') EXCLUDE_VERSIONS="$2"; shift 1;;
    '-INCLUDE_IUS') INCLUDE_IUS="$2"; shift 1;;
    '-EXCLUDE_IUS') EXCLUDE_IUS="$2"; shift 1;;
    *) others="$others $1"; shift 0;;
  esac
  shift 1
done

# if not set commandline, use default upstream job based on this job's name -> devstudio.product_master, devstudio.product_8.0.luna, etc.
if [[ ! ${UPSTREAM_JOB} ]]; then
  if [[ ${JOB_NAME} ]]; then
    UPSTREAM_JOB=${JOB_NAME/versionwatch/product}
  else
    UPSTREAM_JOB=devstudio.product_master
  fi
fi

if [[ ! ${JBDS_INSTALLER_NIGHTLY_FOLDER} ]]; then
  # Folder from which to install the latest nightly JBDS build, and run the version watch comparing this latest against
  # the baseline JBDS_INSTALLERS. This will always overwrite if the version has changed since last time.
  if [[ -f $(find /qa/services/http/binaries/RHDS/9.0/snapshots/builds/${UPSTREAM_JOB}/latest/all/ -maxdepth 1 -type f -name "*installer*.jar" | head -1) ]]; then # JBDS 9
    JBDS_INSTALLER_NIGHTLY_FOLDER=/qa/services/http/binaries/RHDS/9.0/snapshots/builds/${UPSTREAM_JOB}/latest/all/
  elif [[ -f $(find /qa/services/http/binaries/RHDS/9.0/snapshots/builds/${UPSTREAM_JOB}/installer/ -maxdepth 1 -type f -name "*installer*.jar" | head -1) ]]; then # JBDS 8 and earlier
  JBDS_INSTALLER_NIGHTLY_FOLDER=/qa/services/http/binaries/RHDS/9.0/snapshots/builds/${UPSTREAM_JOB}/installer/
  fi
fi
if [[ ! ${JBDS_INSTALLER_NIGHTLY_FOLDER} ]]; then
  echo "[ERROR] No JBDS nightly folder defined in JBDS_INSTALLER_NIGHTLY_FOLDER = ${JBDS_INSTALLER_NIGHTLY_FOLDER}"
fi

# define globals in case they were overridden above
export JAVA_HOME
export M2_HOME
export PATH

# add more auditing/tests here as needed
check_results ()
{
  label=$1 # Title Case
  name=${label,,} # lowercase
  calltoaction=":: See ${label} Reports: ${URL}/${BUILD_ID}_B${BUILD_NUMBER}_report_detailed_${name}.html and ${URL}/${BUILD_ID}_B${BUILD_NUMBER}_report_summary_${name}.html"
  if [[ ! `egrep -l "<td>|<tr>" ${FROM}/report_detailed_${name}.html` ]]; then
    echo "FAILURE IN OUTPUT: Empty results in report_detailed_${name}.html"
    echo $calltoaction
  fi
  if [[ `egrep -l "ERROR:" ${FROM}/report_detailed_${name}.html` ]]; then
    echo "FAILURE IN OUTPUT: Errors found in report_detailed_${name}.html"
    echo $calltoaction
  fi
}

publish ()
{
  label=$1 # Title Case
  name=${label,,} # lowercase
  # rename in workspace
  mv ${FROM}/report_detailed.html ${FROM}/report_detailed_${name}.html
  mv ${FROM}/report_summary.html ${FROM}/report_summary_${name}.html
  # publish
  echo "mkdir ${JOB_NAME}" | sftp ${SNAPSHOTS}
  rsync -Pzrlt --rsh=ssh --protocol=28 ${FROM}/report_detailed_${name}.html ${DEST}/${BUILD_ID}_B${BUILD_NUMBER}_report_detailed_${name}.html
  rsync -Pzrlt --rsh=ssh --protocol=28 ${FROM}/report_summary_${name}.html ${DEST}/${BUILD_ID}_B${BUILD_NUMBER}_report_summary_${name}.html
  rsync -Pzrlt --rsh=ssh --protocol=28 ${FROM}/target/*.css ${FROM}/target/*.png ${DEST}/target/
  # create links to html files (must be all on one line)
  DESCRIPTION="${DESCRIPTION}"'<li>'${label}' <a href="'${URL}'/'${BUILD_ID}'_B'${BUILD_NUMBER}'_report_detailed_'${name}'.html">Details</a>,\
   <a href="'${URL}'/'${BUILD_ID}'_B'${BUILD_NUMBER}'_report_summary_'${name}'.html">Summary</a></li>'
}

#################################################################

# do JBDS installs so we can compare them
pushd ${FROM}
. ${FROM}/install.jbds.sh -JBDS_INSTALLERS_LISTFILE ${JBDS_INSTALLERS_LISTFILE} -JAVA ${JAVA_HOME}/bin/java ${others}
popd

# clean up leftovers from previous builds
pushd ${FROM}; rm -f output.html product.html *report*.html; popd

# generate reports and publish them
pushd ${WORKSPACE}
  ${MVN} -f ${FROM}/pom.xml clean test -fn -Dmaven.repo.local=${WORKSPACE}/.repository -DexcludeVersions="${EXCLUDE_VERSIONS}" -DincludeVersions="${INCLUDE_VERSIONS}" \
  -DexcludeIUs="${EXCLUDE_IUS}" -DincludeIUs="${INCLUDE_IUS}" \
  -DinstallationsDir="${INSTALL_FOLDER}" && publish Filtered && check_results Filtered
  ${MVN} -f ${FROM}/pom.xml clean test -fn -Dmaven.repo.local=${WORKSPACE}/.repository -DexcludeVersions="${EXCLUDE_VERSIONS}" -DincludeVersions="${INCLUDE_VERSIONS}" \
  -DexcludeIUs="${EXCLUDE_IUS}" -DincludeIUs=".*" \
  -DinstallationsDir="${INSTALL_FOLDER}" && publish All && check_results All
popd

# set build description (Jenkins only sees the first occurrence)
BUILD_DESCRIPTION="${DESCRIPTION}"
