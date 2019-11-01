# mule4-jar-size-reducer-maven-plugin
maven plugin which reduces the mule-application.jar files created by
[mule4 applications](https://www.mulesoft.com/platform/mule)

## Usage:

Add following section to your pom  (maybe with up-to-date versions):
```
...
	<build>
...
		<plugin>
			<groupId>io.github.c-a-services</groupId>
			<artifactId>mule4-jar-size-reducer-maven-plugin</artifactId>
			<executions>
				<execution>
					<id>compress-jar</id>
					<phase>package</phase>
					<goals>
						<goal>jar-compress</goal>
					</goals>
				</execution>
			</executions>
		</plugin>
...
		<pluginManagement>
...
			<plugins>
				<plugin>
					<groupId>org.apache.maven.plugins</groupId>
					<artifactId>maven-dependency-plugin</artifactId>
					<version>3.1.1</version>
				</plugin>
				<plugin>
					<groupId>io.github.c-a-services</groupId>
					<artifactId>mule4-jar-size-reducer-maven-plugin</artifactId>
					<version>2019.10.2</version>
				</plugin>
			</plugins>
		</pluginManagement>
	</build>
...
```

and then during building the repository/**/jar files will be replaced by placeholders.
(you may set the plugin/execution section into a profile to not replace the jar content always).
Another possibility is to call
mvn package -Dcompress-jar-skip=true

Add this profile to the pom.xml
```
		<profile>
			<id>deploy-to-cloud-z-os</id>
            ...
			<build>
				<plugins>
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-dependency-plugin</artifactId>
						<executions>
							<execution>
								<id>download-all-dependencies-for-refill</id>
								<!-- phase is clean to hook into:
									https://mantis.retail-sc.com/view.php?id=878065
									(validate requires maven >= 3.3.3)
								 -->
								<phase>clean</phase>
								<goals>
									<!-- resolve as many artifacts before running jar-refill to use normal dependency resolution
										and not internal downloadArtifact via org.twdata.maven.mojoexecutor.MojoExecutor -->
									<goal>resolve</goal>
									<goal>tree</goal>
								</goals>
							</execution>
						</executions>
						<configuration>
						   	<verbose>true</verbose>
						</configuration>
					</plugin>
					<plugin>
						<groupId>io.github.c-a-services</groupId>
						<artifactId>mule4-jar-size-reducer-maven-plugin</artifactId>
						<executions>
							<execution>
								<id>refill-jar</id>
								<!-- phase is clean to hook into:
									https://mantis.retail-sc.com/view.php?id=878065
									(validate requires maven >= 3.3.3)
								 -->
								<phase>clean</phase>
								<goals>
									<goal>jar-refill</goal>
								</goals>
							</execution>
						</executions>
					</plugin>
				</plugins>
			</build>
		</profile>
```

to ensure the placeholders are removed before
```
mvn clean mule:deploy -P deploy-to-cloud-z-os
```
is executed.
(!) you need to execute clean and mule:deploy for above configuration.

## Manual usage:

```
mvn io.github.c-a-services:mule4-jar-size-reducer-maven-plugin:LATEST:jar-compress
```
and
```
mvn io.github.c-a-services:mule4-jar-size-reducer-maven-plugin:LATEST:jar-refill
```

## Known workarounds

In case go-offline fails with
```
Could not find artifact org.apache.maven.plugins:maven-site-plugin:jar:3.6.1
```
you need to add an existing version to your pluginManagement section:
See (https://github.com/mulesoft/mule-maven-plugin/issues/336)


Homepage: (https://c-a-services.github.io/mule4-jar-size-reducer-maven-plugin/)
