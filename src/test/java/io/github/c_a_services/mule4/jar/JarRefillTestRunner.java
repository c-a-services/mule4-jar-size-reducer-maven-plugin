package io.github.c_a_services.mule4.jar;

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;

/**
 * TestRunner to run plugin without maven.
 */
public class JarRefillTestRunner {
	public static void main(String[] args) throws MojoExecutionException {
		MulestacMavenJarRefillPlugin tempPlugin = new MulestacMavenJarRefillPlugin() {
			/**
			 *
			 */
			@Override
			protected String getBasedir() {
				return "c:\\m2";
			}
		};
		String tempTargetDir = "C:\\java\\mule-7\\firm-system\\target";
		tempPlugin.setSourceFile(new File(tempTargetDir + "\\firm-system-m4-2019.9.1-3-SNAPSHOT-mule-application.zip"));
		tempPlugin.setTemporaryFile(new File(tempTargetDir + "\\firm-system-m4-2019.9.1-3-SNAPSHOT-mule-application.jar"));
		tempPlugin.setKeepTemporaryFile(true);
		tempPlugin.execute();
	}
}
