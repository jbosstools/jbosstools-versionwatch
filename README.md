# JBossTools VersionWatch #

## Description ##
VersionWatch (vwatch) is application for comparing bundles across several eclipse installations based on several predefined criteria. If criterium is not fulfilled an issue is raised. When evaluation is completed, reports are generated.

  * **BundleVersionReport** (report_detailed.html)
  * **ProductReport** (report_summary.html)

## Critera ##

  * **FolderAndJarIssue** - there are unexpected both foder and jar file for some bundle
  * **MD5Issue** - same bundle doesn’t have the same MD5 between installation (deep file scan is performed if needed), disabled by default
  * **MultipleVersionIssue** - bundle is present in multiple versions (ignored cases can be defined in multiple-version-ignore.properties)
  * **OkIssue** - reports that previous issue was fixed
  * **VersionDecreasedIssue** - bundle version is lower than bundle version in previous installation


## Prerequisites ##
Version watch requires one folder containing several Eclipse installation, for example:

* /opt/vw/
    * eclipse-4.2.0-final
    * eclispe-4.2.1-m1
    * eclipse-4.2.2-m2

## Execution ##

### Executing as JUnit test via maven ###

    mvn -fae clean test [-Dparameter[=value]...]
      
For example:

    mvn clean test -Dvwatch.installationsDir="/tmp/vw"
    
### Executing as JAR application ###

#### Install devstudio

First, perform 2 or more devstudio installs using `install.devstudio.sh` (or install by hand).

You can also do a headless install using a script like this to invoke a console install into ${HOME}/devstudio:

    # move any existing install in ${HOME}/devstudio first
    if [[ -d ${HOME}/devstudio ]]; then mv ${HOME}/devstudio{,.PREVIOUS}; echo "Old devstudio install in ${HOME}/devstudio moved to ${HOME}/devstudio.PREVIOUS"; fi

    # pipe "1" to the console install to accept the license terms and install into ${HOME}/devstudio
    echo 1 | java -jar devstudio-*installer*.jar -console; echo "Installed to ${HOME}/devstudio"

NOTE: If you did a headless or by-hand install, move those installs into whatever folder you set with `-Dvwatch.installationsDir` below. 

NOTE: If you used `install.devstudio.sh` then the `-INSTALL_FOLDER` parameter should be used to set the folder into which you're installing. That folder should be the same as the one you specify with `-Dvwatch.installationsDir` below.

#### Run versionwatch

Next, use versionwatch to compare those installs:

    # fetch sources and build it
    git clone https://github.com/jbosstools/jbosstools-versionwatch.git
    cd *versionwatch; mvn package -DskipTests=true; ls -la target

    # if you have installed some devstudio installations in /tmp/vw, you can run the tests. Otherwise they will fail.
    mvn package -DskipTests=false "-Dvwatch.installationsDir=/tmp/vw" 

    # or a as maven job, run first in "all" mode then in "filtered" mode to generate 4 report*.html files:
    mvn test -q "-Dvwatch.installationsDir=/tmp/vw" "-Dvwatch.md5check" "-Dvwatch.includeIUs=.*" | tee log.txt
    mvn test -q "-Dvwatch.installationsDir=/tmp/vw" "-Dvwatch.md5check" "-Dvwatch.includeIUs=.*(jboss|hibernate).*" | tee -a log.txt

    # or similarly, you can run as a java process:
    java -jar "-Dvwatch.installationsDir=/tmp/vw" "-Dvwatch.md5check" target/vwatch-*-jar-with-dependencies.jar | tee log.txt
    java -jar "-Dvwatch.installationsDir=/tmp/vw" "-Dvwatch.md5check" "-Dvwatch.includeIUs=.*(jboss|hibernate).*" target/vwatch-*-jar-with-dependencies.jar | tee -a log.txt


## Parameters ##
**vwatch.loglevel** - specify log4j loglevel for vw logs
values: 7 -debug, 6- info, 4- warn, 3 - error, 0 - fatal
default: all
example: -Dvwatch.loglevel=6

**vwatch.installationsDir** - directory where eclipse installations are located
default: /opt/vw
example: -Dvwatch.installationsDir=/opt/my_ide_container_dir

**vwatch.product** - product for which the specific product report is generated
default: last product (product with the highest version)
example:

**vwatch.md5check** - enableds MD5 check comparing related bundles accross installations
defaut: disabled
example: -Dvwatch.md5check

**vwatch.includeVersions** - allows to list only selected installations, eg., (10|11)
defaut: \d+\.\d+\.\d+

**vwatch.excludeVersions** - allows to exclude selected installations
default: none

**vwatch.includeIUs** - allows to include specific bundles, eg., .*(jboss|hibernate).*
default: ".*"

**vwatch.excludeIUs** - allows to exclude specific bundles
default: none;

**vwatch.filenameSuffix** - allows to rename report files, eg., .something.html
default: .html;

**vwatch.setLogLevel** - allows to set log level
default: Level.WARN (4)
