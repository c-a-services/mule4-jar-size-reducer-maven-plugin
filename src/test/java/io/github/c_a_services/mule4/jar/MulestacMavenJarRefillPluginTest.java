package io.github.c_a_services.mule4.jar;

import static org.junit.Assert.*;

import org.junit.Test;

/**
 *
 */
public class MulestacMavenJarRefillPluginTest {

	@Test
	public void testGuessGAVWithoutClassifier() {
		String tempPath = "org/codehaus/mojo/animal-sniffer-annotations/1.17/animal-sniffer-annotations-1.17.jar";
		String tempGAV = new MulestacMavenJarRefillPlugin().guessGAV(tempPath);
		// A string of the form groupId:artifactId:version[:packaging[:classifier]].
		String tempArtifact = "org.codehaus.mojo:animal-sniffer-annotations:1.17:jar";
		assertEquals(tempArtifact, tempGAV);
	}

	/**
	Pfad: repository\org\mule\modules\mule-apikit-module\1.3.7\
	Name: mule-apikit-module-1.3.7-mule-plugin.jar
	 */
	@Test
	public void testGuessGAVWithClassifier() {
		String tempPath = "org/mule/modules/mule-apikit-module/1.3.7/mule-apikit-module-1.3.7-mule-plugin.jar";
		String tempGAV = new MulestacMavenJarRefillPlugin().guessGAV(tempPath);
		// A string of the form groupId:artifactId:version[:packaging[:classifier]].
		String tempArtifact = "org.mule.modules:mule-apikit-module:1.3.7:jar:mule-plugin";
		assertEquals(tempArtifact, tempGAV);
	}

	@Test
	public void testGuessGAVWithTextInVersion() {
		String tempPath = "org/springframework/spring-beans/5.1.0.RELEASE/spring-beans-5.1.0.RELEASE.jar";
		String tempGAV = new MulestacMavenJarRefillPlugin().guessGAV(tempPath);
		// A string of the form groupId:artifactId:version[:packaging[:classifier]].
		String tempArtifact = "org.springframework:spring-beans:5.1.0.RELEASE:jar";
		assertEquals(tempArtifact, tempGAV);
	}

}
