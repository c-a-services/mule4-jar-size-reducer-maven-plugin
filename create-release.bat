rem
rem pre-condition: signing
rem https://maven.apache.org/plugins/maven-gpg-plugin/usage.html 
rem
rem upload the ascii key to https://keyserver.ubuntu.com/
rem gpg --export -a YOUR_KEY

gpg --list-signatures

start gpg-agent

echo on
git pull
if errorlevel 1 goto ende
call mvn clean build-helper:parse-version release:prepare release:perform -DdevelopmentVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}-SNAPSHOT -DreleaseVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -Dtag=mule4-jar-size-reducer-maven-plugin-${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -DpreparationGoals=clean -Darguments="-Dmaven.test.skip=true -P oss-publish" -P oss-publish
if errorlevel 1 goto ende

rem call mvn nexus-staging:release
echo Close and Release Repository at https://oss.sonatype.org/#stagingRepositories

:ende
