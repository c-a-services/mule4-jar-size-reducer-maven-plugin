# mule4-jar-size-reducer-maven-plugin
maven plugin which reduces the mule-application.jar files created by
[mule4 applications](https://www.mulesoft.com/platform/mule)

Usage:

Add following section to your pom:
```
					<plugin>
						<groupId>org.apache.maven.plugins</groupId>
						<artifactId>maven-dependency-plugin</artifactId>
						<version>3.1.1</version>
						<executions>
							<execution>
								<id>download-all-dependencies-for-compress</id>
								<phase>package</phase>
								<goals>
									<goal>go-offline</goal>
								</goals>
							</execution>
						</executions>
					</plugin>
					<plugin>
						<groupId>io.github.c-a-services</groupId>
						<artifactId>mule4-jar-size-reducer-maven-plugin</artifactId>
						<version>2019.10.1-SNAPSHOT</version>
						<executions>
							<execution>
								<id>compress-and-copy-jar-to-zip</id>
								<phase>package</phase>
								<goals>
									<goal>jar-compress</goal>
								</goals>
							</execution>
						</executions>
					</plugin>
```

and then during building the repository/**/jar files will be replaced by placeholders.


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
						<version>3.1.1</version>
						<executions>
							<execution>
								<id>download-all-dependencies-for-refill</id>
								<!-- phase is clean to hook into:
									https://mantis.retail-sc.com/view.php?id=878065

									(validate requires maven >= 3.3.3)
								 -->
								<phase>clean</phase>
								<goals>
									<goal>go-offline</goal>
								</goals>
							</execution>
						</executions>
					</plugin>
					<plugin>
						<groupId>io.github.c-a-services</groupId>
						<artifactId>mule4-jar-size-reducer-maven-plugin</artifactId>
						<version>2019.10.1-SNAPSHOT</version>
						<executions>
							<execution>
								<id>refill-and-copy-zip-to-jar</id>
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
clean mule:deploy -P deploy-to-cloud-z-os
```
is executed.

Homepage: (https://c-a-services.github.io/mule4-jar-size-reducer-maven-plugin/)
