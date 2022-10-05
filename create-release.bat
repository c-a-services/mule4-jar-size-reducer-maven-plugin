rem
rem pre-condition: signing
rem https://maven.apache.org/plugins/maven-gpg-plugin/usage.html 
rem
gpg --list-signatures

echo on
git pull
if errorlevel 1 goto ende
call mvn clean build-helper:parse-version release:prepare release:perform -DdevelopmentVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.nextIncrementalVersion}-SNAPSHOT -DreleaseVersion=${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -Dtag=mule-metrics-report-${parsedVersion.majorVersion}.${parsedVersion.minorVersion}.${parsedVersion.incrementalVersion} -DpreparationGoals=clean -Darguments="-Dmaven.test.skip=true -P oss-publish" -P oss-publish
if errorlevel 1 goto ende

rem call mvn nexus-staging:release
echo Close and Release Repository at https://oss.sonatype.org/#stagingRepositories

:ende
