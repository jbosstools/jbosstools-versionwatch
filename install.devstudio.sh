#!/bin/bash

# Purpose: run this script in in a Jenkins job to install multiple versions of devstudio, then compare the versions 
# of plugins/features in those installations using https://github.com/jbosstools/jbosstools-versionwatch/tree/master

# If you want to use this script locally, you need to set some overrides - see commandline args below

# Usage: wget this script from raw.github, then call it from an "Execute shell" step in your job, before calling 
# mvn -f pom.xml clean test -DinstallationsDir="${INSTALL_FOLDER}" -DincludeIUs=".*jboss.*" -Dvwatch.md5check

# Here's an example to unpack a couple devstudio installers already on disk:
# ./install.devstudio.sh -JAVA /opt/sun-java2-8.0/bin/java -INSTALL_FOLDER /w/home-nboldt/tmp/devstudio-installs/ -INSTALLERS "/w/home-nboldt/tmp/devstudio_Installers/8.x/jboss-devstudio-8.0.2.GA-v20150114-2029-B382-installer-eap.jar, /w/home-nboldt/tmp/devstudio_Installers/8.x/jboss-devstudio-8.0.0.GA-v20141020-1042-B317-installer-standalone.jar"

# Jenkins variables:

# INSTALL_FOLDER :: Location where devstudio installations will be put; default: /home/hudson/static_build_env/devstudio/versionwatch/installations
INSTALL_FOLDER=/home/hudson/static_build_env/devstudio/versionwatch/installations

# INSTALLER_NIGHTLY_FOLDER :: /qa/services/http/binaries/RHDS/9.0/snapshots/builds/devstudio.product_master/installer/
# Folder from which to install the latest nightly devstudio build, and run the version watch comparing this latest against
# the baseline INSTALLERS. This will always overwrite if the version has changed since last time.

# INSTALLERS :: CSV list (with spaces!) of additional installer jars to use. Will also use list of installers in INSTALLERS_LISTFILE = install.devstudio.list.txt. 
# If the target folder already exists, installation will be skipped. 
# /qa/services/http/binaries/RHDS/builds/development/7.0.0.Beta2.installer/jbdevstudio-product-universal-7.0.0.Beta2-v20130626-0242-B345.jar, 
# /qa/services/http/binaries/RHDS/builds/development/7.0.0.Beta1.installer/jbdevstudio-product-universal-7.0.0.Beta1-v20130529-0631-B257.jar 

# INSTALLERS_LISTFILE :: used to find install.devstudio.list.txt; use either present working directory or passed in commandline arg
INSTALLERS_LISTFILE=`pwd`/install.devstudio.list.txt

# BASE_URL :: if path to installer is not found locally, set a base URL instead from which to download them; default: http://www.qa.jboss.com/binaries/RHDS/
BASE_URL=http://www.qa.jboss.com/binaries/RHDS

usage() {
  echo "$0"
  echo "  [ -JAVA /qa/tools/opt/jdk1.8.0_last/bin/java ]"
  echo "  [ -BASE_URL http://www.qa.jboss.com/binaries/RHDS ]" 
  echo "  [ -INSTALL_FOLDER /home/hudson/static_build_env/devstudio/versionwatch/installations ]"
  echo "  [ -INSTALLER_NIGHTLY_FOLDER /10.0/snapshots/builds/devstudio.product_master/latest/all/ ]"
  echo "  [ -INSTALLERS_LISTFILE /path/to/install.devstudio.list.txt ]"
  echo "  [ -INSTALLERS \"/path/to/jboss-devstudio-9.1.0.Beta1-v20151216-2040-B197-installer-standalone.jar, /path/to/jboss-devstudio-10.0.0.Alpha1-v20160105-0547-B4563-installer-standalone.jar\" ]"
  echo ""
  echo "Example:"
  echo "  ./install.devstudio.sh -JAVA /opt/jdk1.8.0/bin/java -INSTALL_FOLDER /tmp/versionwatch-installations -INSTALLERS_LISTFILE /dev/null \\"
  echo "    -INSTALLERS \"/tmp/jboss-devstudio-9.1.0.Beta1-v20151216-2040-B197-installer-standalone.jar, /tmp/jboss-devstudio-10.0.0.Alpha1-v20160105-0547-B4563-installer-standalone.jar\""

}

if [[ $# -eq 0 ]]; then
  usage; exit
fi

others=""
# read commandline args
while [[ "$#" -gt 0 ]]; do
  case $1 in
    '-JAVA') JAVA="$2"; shift 1;; # /path/to/bin/java8
    '-BASE_URL') BASE_URL="$2"; shift 1;; # if path to installer is not found locally, set a base URL instead from which to download; default: http://www.qa.jboss.com/binaries/RHDS
    '-INSTALL_FOLDER') INSTALL_FOLDER="$2"; shift 1;; # path to parent folder under which to perform installations, eg., /home/hudson/static_build_env/devstudio/versionwatch/installations
    '-INSTALLER_NIGHTLY_FOLDER') INSTALLER_NIGHTLY_FOLDER="$2"; shift 1;; # Folder from which to install the latest nightly devstudio build
    '-INSTALLERS') INSTALLERS="$2"; shift 1;; 
    '-INSTALLERS_LISTFILE') INSTALLERS_LISTFILE="$2"; shift 1;; # path to install.devstudio.list.txt or other file with CSV or one-per-line list of devstudio installers to run
    *) others="$others $1"; shift 0;;
    esac
  shift 1
done

# which version of Java are we using? 
if [[ ${JAVA} ]]; then
  if [[ ! -x ${JAVA} ]]; then 
    echo "Could not execute ${JAVA}! Please use -JAVA /path/to/bin/java"; exit 1
  fi
else
  if [[ ! ${NATIVE_TOOLS} ]]; then NATIVE_TOOLS=/qa/tools/opt; fi
  if [[ -x ${NATIVE_TOOLS}/jdk1.8.0_last/bin/java ]]; then 
    JAVA=${NATIVE_TOOLS}/jdk1.8.0_last/bin/java
  elif [[ -x /usr/bin/java ]]; then
    JAVA=/usr/bin/java
  elif [[ -x /bin/java ]]; then
    JAVA=/bin/java
  else
    echo "Could not find path to /bin/java! Please use -JAVA /path/to/bin/java"; exit 1
  fi
fi
${JAVA} -version

# get list of installers to install
INSTALLER_LIST="${INSTALLERS} `if [[ ${INSTALLERS_LISTFILE} ]] && [[ -f ${INSTALLERS_LISTFILE} ]]; then cat ${INSTALLERS_LISTFILE}; fi`"
if [[ ! ${INSTALLER_LIST} ]]; then 
  echo "No installers defined! Must specify installers to use as baseline for comparison using"
  echo "  -INSTALLERS \"/path/to/installer.jar, /path/to/installer2.jar\""
  echo "    and/or"
  echo "  -INSTALLERS_LISTFILE /path/to/listfile.txt"
  exit 1;
fi

if [[ ! -d ${INSTALL_FOLDER} ]]; then
  echo "Warning: INSTALL_FOLDER = ${INSTALL_FOLDER} does not exist, so creating it."
  mkdir -p ${INSTALL_FOLDER}
fi

# location for downloaded installers
TMPDIR=/tmp

# define install config file
installDevstudio() {
  version=${1}
  jar=${2}

  namespace=com.jboss.devstudio.core.installer
  echo "<?xml version='1.0' encoding='UTF-8' standalone='no'?>
<AutomatedInstallation langpack='eng'>
<${namespace}.HTMLInfoPanelWithRootWarning id='introduction'/>
<com.izforge.izpack.panels.HTMLLicencePanel id='licence'/>
<${namespace}.PathInputPanel id='target'>
<installpath>${INSTALL_FOLDER}/devstudio-${version}</installpath>
</${namespace}.PathInputPanel>
<${namespace}.JREPathPanel id='jre'/>
<${namespace}.JBossAsSelectPanel id='as'>
<installgroup>devstudio</installgroup>
</${namespace}.JBossAsSelectPanel>
<${namespace}.UpdatePacksPanel id='updatepacks'/>
<${namespace}.DiskSpaceCheckPanel id='diskspacecheck'/>
<com.izforge.izpack.panels.SummaryPanel id='summary'/>
<com.izforge.izpack.panels.InstallPanel id='install'/>
<${namespace}.CreateLinkPanel id='createlink'>
<jrelocation>$(which java)</jrelocation>
</${namespace}.CreateLinkPanel>
<com.izforge.izpack.panels.ShortcutPanel id='shortcut'/>
<${namespace}.ShortcutPanelPatch id='shortcutpatch'/>
<com.izforge.izpack.panels.SimpleFinishPanel id='finish'/>
</AutomatedInstallation>" > ${INSTALL_FOLDER}/devstudio-${version}.install.xml
  if [[ ! -f ${jar} ]] && [[ ${BASE_URL} ]]; then
    # download the installer 
    remoteJar=${BASE_URL}/${jar##/qa/services/http/binaries/RHDS/}
    jar=${TMPDIR}/${jar##*/}
    echo "${jar} not found, so download it from ${remoteJar}"
    pushd ${TMPDIR}/ >/dev/null; wget -nc ${remoteJar}; popd >/dev/null
  fi
  ${JAVA} ${others} -jar ${jar} ${INSTALL_FOLDER}/devstudio-${version}.install.xml
}

if [[ ${INSTALLER_NIGHTLY_FOLDER} ]] && [[ -d ${INSTALLER_NIGHTLY_FOLDER} ]]; then 
  # install the latest nightly, caching the last version used in devstudio-10.2.0.AM3/version.txt so we only ever have one nightly at a time
  # new query method for devstudio 8/9/10, eg., for devstudio-10.2.0.AM3-v20161109-2358-B6414-installer-standalone.jar
  for i in `find ${INSTALLER_NIGHTLY_FOLDER} -name "*devstudio-*-installer-standalone.jar" -a -not -name "*latest*"`; do
    ver=${i##*devstudio-}; ver=${ver%%-installer-standalone.jar*}; ver=${ver##*devstudio-} # 10.2.0.AM3-v20161109-2358-B6414
    f=${i##*devstudio-}; f=${f%%-*}; f=${f##*devstudio-} # 10.2.0.AM3
    LATEST=${INSTALL_FOLDER}/devstudio-${f}/version.txt
    if [[ -d ${INSTALL_FOLDER}/devstudio-${f} ]] && [[ -f ${LATEST} ]] && [[ `cat ${LATEST}` == $ver ]]; then 
      echo "Existing devstudio install in ${INSTALL_FOLDER}/devstudio-${f} (${ver})"
    else
      # wipe existing installation
      if [[ ${f} ]] && [[ -d ${INSTALL_FOLDER}/devstudio-${f} ]]; then rm -fr ${INSTALL_FOLDER}/devstudio-${f}; fi
      # echo "Install devstudio ${f} (${ver}) to ${INSTALL_FOLDER}/devstudio-${f} ..."
      installDevstudio ${f} ${i}
      echo "${ver}" > ${LATEST}
    fi
  done

  # install the latest nightly, caching the last version used in devstudio-7.0.0.CR1/version.txt so we only ever have one nightly at a time
  # old query method for devstudio 5/6/7, eg., jbdevstudio-product-universal-7.1.0.GA-v20131208-0703-B592.jar
  for i in `find ${INSTALLER_NIGHTLY_FOLDER} -name "jbdevstudio-product-universal-*.jar" -a -not -name "*latest*"`; do
    ver=${i##*-universal-}; ver=${ver%%.jar}; # 7.0.0.Beta2-v20130626-0242-B345
    f=${i##*-universal-}; f=${f%%-*}; # 7.0.0.Beta2
    LATEST=${INSTALL_FOLDER}/devstudio-${f}/version.txt
    if [[ -d ${INSTALL_FOLDER}/devstudio-${f} ]] && [[ -f ${LATEST} ]] && [[ `cat ${LATEST}` == $ver ]]; then 
      echo "Existing devstudio install in ${INSTALL_FOLDER}/devstudio-${f} (${ver})"
    else
      # wipe existing installation
      if [[ ${f} ]] && [[ -d ${INSTALL_FOLDER}/devstudio-${f} ]]; then rm -fr ${INSTALL_FOLDER}/devstudio-${f}; fi
      # echo "Install devstudio ${f} (${ver}) to ${INSTALL_FOLDER}/devstudio-${f} ..."
      installDevstudio ${f} ${i}
      echo "${ver}" > ${LATEST}
    fi
  done
else
  echo "[ERROR] No nightly devstudio install found in INSTALLER_NIGHTLY_FOLDER = ${INSTALLER_NIGHTLY_FOLDER}"
fi

# install stable releases + development milestones (baselines for comparison)
for i in ${INSTALLER_LIST}; do
  # if target folder does not exist, run the installer
  # 8.0.0.GA-v20141020-1042-B317
  # support old file formats (4, 5/6/7, and 8/9
  ver=${i##*jbdevstudio-product-linux-gtk-}; ver=${ver##*-universal-}; ver=${ver##*-devstudio-}; ver=${ver%%.jar*}; ver=${ver%%-installer-standalone};  
  if [[ -d ${INSTALL_FOLDER}/devstudio-${ver} ]]; then 
    echo "Existing devstudio install in ${INSTALL_FOLDER}/devstudio-${ver}"
  else
    # echo "Install devstudio ${ver} to ${INSTALL_FOLDER}/devstudio-${ver} ..."
    installDevstudio ${ver} ${i%%,}
  fi
done

echo "Now run this:"
echo ""
echo "mvn clean test -DinstallationsDir=${INSTALL_FOLDER} -DincludeIUs=\".*jboss.*\" -Dvwatch.md5check ${others}; firefox report_*.html &"
echo " - or, for a full report including upstream 3rd party IUs - "
echo "mvn clean test -DinstallationsDir=${INSTALL_FOLDER} -DincludeIUs=\".*\" -Dvwatch.md5check ${others}; firefox report_*.html &"
