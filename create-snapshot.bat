rem
rem pre-condition: signing
rem https://maven.apache.org/plugins/maven-gpg-plugin/usage.html 
rem
gpg --list-signatures

mvn clean deploy -P oss-publish