#!/bin/bash

# script to run a jenkins job for versionwatch

#################################################################

# configuration / defaults
if [[ ! ${WORKSPACE} ]]; then WORKSPACE=/tmp; fi

# where is java?
if [[ ! ${NATIVE_TOOLS} ]] && [[ -d /qa/tools/opt ]]; then NATIVE_TOOLS=/qa/tools/opt; fi
if [[ ! ${JAVA_HOME} ]] && [[ ${NATIVE_TOOLS} ]]; then JAVA_HOME=${NATIVE_TOOLS}/jdk1.8.0_last; fi

# where is maven?
M2_HOME=/qa/tools/opt/apache-maven-3.3.9 # don't use NATIVE_TOOLS because it might be /qa/tools/opt/amd64 and there's no /qa/tools/opt/amd64/apache-maven-3.3.9
if [[ ! -f ${M2_HOME}/bin/mvn ]]; then echo "[ERROR] Can't find mvn in ${M2_HOME}"; exit 1; fi

PATH=$PATH:$M2_HOME/bin:$JAVA_HOME/bin
MVN=${M2_HOME}/bin/mvn

# default upstream job = devstudio.product_master, devstudio.product_8.0.luna, etc.
UPSTREAM_JOB=""

# CSV list of additional installer jars to use. Will also use list of installers in install.devstudio.list.txt.
# If the target folder already exists, installation will be skipped.<br/>
#  eg., http://path/to/*-installer-standalone.jar
INSTALLERS=""

INSTALL_FOLDER=${WORKSPACE}/devstudio/versionwatch/installations

# To generate a report containing fewer bundles/features, set a regex that will match only those you want in the report, eg., .*(hibernate|jboss|xulrunner).* or match everything with .*
INCLUDE_IUS=".*(hibernate|jboss).*"

DESCRIPTION=""

DESTINATION=devstudio@10.5.105.197:/www_htdocs/devstudio # or tools@filemgmt.jboss.org:/downloads_htdocs/tools

# include and exclude patterns for which devstudio installs to use when producing the version diff report
INCLUDE_VERSIONS="\d+\.\d+\.\d+"
EXCLUDE_VERSIONS=""
INCLUDE_IUS=".*"
EXCLUDE_IUS=""
STREAM_NAME="11" # for devstudio, use 11, 10.0, 9.0; for JBT, use oxygen, neon, mars
others=""

BUILD_TIMESTAMP=`date -u +%Y-%m-%d_%H-%M-%S`
SRC_PATH=${WORKSPACE}/sources
TRG_PATH=${STREAM_NAME}/snapshots/builds/${JOB_NAME}/${BUILD_TIMESTAMP}-B${BUILD_NUMBER}

# location for downloaded installers
TMPDIR=/tmp

# file from which to pull a list of devstudio installers to install
INSTALLERS_LISTFILE=${SRC_PATH}/install.devstudio.list.txt

# read commandline args
while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-JAVA_HOME') JAVA_HOME="$2"; shift 1;;
    '-M2_HOME') M2_HOME="$2"; shift 1;;
    '-UPSTREAM_JOB') UPSTREAM_JOB="$2"; shift 1;;
    '-INSTALLERS') INSTALLERS="-INSTALLERS $2"; shift 1;;
    '-INSTALL_FOLDER') INSTALL_FOLDER="$2"; shift 1;;
    '-INSTALLER_NIGHTLY_FOLDER') INSTALLER_NIGHTLY_FOLDER="$2"; shift 1;;
    '-INSTALLERS_LISTFILE') INSTALLERS_LISTFILE="$2"; shift 1;;
    '-INCLUDE_VERSIONS') INCLUDE_VERSIONS="$2"; shift 1;;
    '-EXCLUDE_VERSIONS') EXCLUDE_VERSIONS="$2"; shift 1;;
    '-INCLUDE_IUS') INCLUDE_IUS="$2"; shift 1;;
    '-EXCLUDE_IUS') EXCLUDE_IUS="$2"; shift 1;;
    '-STREAM_NAME') STREAM_NAME="$2"; shift 1;;
    '-DESTINATION') DESTINATION="$2"; shift 1;; # override for devstudio publishing, eg., devstudio@filemgmt.jboss.org:/www_htdocs/devstudio
    '-TMPDIR') TMPDIR="$2"; shift 1;; # path to where we're temporarily storing installers (eg., in WORKSPACE/devstudio_installers)
    *) others="$others $1"; shift 0;;
  esac
  shift 1
done

if [[ ${DESTINATION} == "tools@"* ]]; then # JBT public
  URL=http://download.jboss.org/jbosstools/${STREAM_NAME}/snapshots/builds/${JOB_NAME}/${BUILD_TIMESTAMP}-B${BUILD_NUMBER}
elif [[ ${DESTINATION} == "devstudio@"* ]]; then # devstudio public
  URL=https://devstudio.redhat.com/${STREAM_NAME}/snapshots/builds/${JOB_NAME}/${BUILD_TIMESTAMP}-B${BUILD_NUMBER}
else # local file in workspace
  URL="ws/sources/target";
fi

# if not set commandline, use default upstream job based on this job's name -> devstudio.product_master, devstudio.product_8.0.luna, etc.
if [[ ! ${UPSTREAM_JOB} ]]; then
  if [[ ${JOB_NAME} ]]; then
    UPSTREAM_JOB=${JOB_NAME/versionwatch/product}
  else
    UPSTREAM_JOB=devstudio.product_master
  fi
fi

if [[ ! ${INSTALLER_NIGHTLY_FOLDER} ]] && [[ ! ${INSTALLERS} ]]; then
  echo "[WARNING] No devstudio CI folder defined in INSTALLER_NIGHTLY_FOLDER = ${INSTALLER_NIGHTLY_FOLDER}"
  echo "[WARNING] No devstudio CI build defined in INSTALLERS = ${INSTALLERS}"
  echo "[WARNING] So, versionwatch will only compare previous installs, not the latest CI."
else
  if [[ ${INSTALLER_NIGHTLY_FOLDER} ]]; then
    echo "[INFO] [0] use INSTALLER_NIGHTLY_FOLDER = ${INSTALLER_NIGHTLY_FOLDER}"
  fi
  if [[ ${INSTALLERS} ]]; then
    echo "[INFO] [1] use INSTALLERS = ${INSTALLERS}"
  fi
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
  calltoaction=":: See ${label} Reports: ${URL}/report_detailed_${name}.html and ${URL}/report_summary_${name}.html"
  if [[ ! `egrep -l "<td>|<tr>" ${SRC_PATH}/target/report_detailed_${name}.html` ]]; then
    echo "FAILURE IN OUTPUT: Empty results in report_detailed_${name}.html"
    echo $calltoaction
  fi
  if [[ `egrep -l "ERROR:" ${SRC_PATH}/target/report_detailed_${name}.html` ]]; then
    echo "FAILURE IN OUTPUT: Errors found in report_detailed_${name}.html"
    echo $calltoaction
  fi
}

publish ()
{
  label=$1 # Title Case
  name=${label,,} # lowercase
  # rename in workspace
  mkdir -p ${SRC_PATH}/target/

  # publish now depends on having publish/rsync.sh fetched to workspace already -- see 
  # https://repository.jboss.org/nexus/content/groups/public/org/jboss/tools/releng/jbosstools-releng-publish/
  . ${WORKSPACE}/jbosstools-build-ci/publish/rsync.sh -s ${SRC_PATH}/target/ -t ${TRG_PATH}/ -DESTINATION ${DESTINATION} -i "*report_*.html"
  . ${WORKSPACE}/jbosstools-build-ci/publish/rsync.sh -s ${SRC_PATH}/target/ -t ${TRG_PATH}/ -DESTINATION ${DESTINATION} -i "*.png"
  . ${WORKSPACE}/jbosstools-build-ci/publish/rsync.sh -s ${SRC_PATH}/target/ -t ${TRG_PATH}/ -DESTINATION ${DESTINATION} -i "*.css"

  # create links to html files (must be all on one line)
  DESCRIPTION="${DESCRIPTION}"'<li>'${label}' <a href="'${URL}'/report_detailed_'${name}'.html">Details</a>,\
   <a href="'${URL}'/report_summary_'${name}'.html">Summary</a></li>'
}

#################################################################


pushd ${SRC_PATH}
  # do devstudio installs so we can compare them
  INFCMD=""
  if [[ ${INSTALLER_NIGHTLY_FOLDER} ]]; then INFCMD="-INSTALLER_NIGHTLY_FOLDER ${INSTALLER_NIGHTLY_FOLDER}"; fi
  . ${SRC_PATH}/install.devstudio.sh -INSTALLERS_LISTFILE ${INSTALLERS_LISTFILE} ${INSTALLERS} \
    -INSTALL_FOLDER ${INSTALL_FOLDER} -JAVA ${JAVA_HOME}/bin/java ${others} -TMPDIR ${TMPDIR} ${INFCMD}
popd

# wipe any old builds
rm -fr ${INSTALL_FOLDER}/../devstudio-*_PREV 2>&1 1>/dev/null &

# generate reports and publish them
pushd ${WORKSPACE}
  ${MVN} -f ${SRC_PATH}/pom.xml clean
  ${MVN} -f ${SRC_PATH}/pom.xml test -q -fn -Dmaven.repo.local=${WORKSPACE}/.repository -DexcludeVersions="${EXCLUDE_VERSIONS}" \
  -DincludeVersions="${INCLUDE_VERSIONS}" -DexcludeIUs="${EXCLUDE_IUS}" -DincludeIUs="${INCLUDE_IUS}" \
  -DinstallationsDir="${INSTALL_FOLDER}" && publish Filtered && check_results Filtered
  ${MVN} -f ${SRC_PATH}/pom.xml test -q -fn -Dmaven.repo.local=${WORKSPACE}/.repository -DexcludeVersions="${EXCLUDE_VERSIONS}" \
  -DincludeVersions="${INCLUDE_VERSIONS}" -DexcludeIUs="${EXCLUDE_IUS}" -DincludeIUs=".*" \
  -DinstallationsDir="${INSTALL_FOLDER}" && publish All && check_results All
popd

# set build description (Jenkins only sees the first occurrence)
BUILD_DESCRIPTION="${DESCRIPTION}"
