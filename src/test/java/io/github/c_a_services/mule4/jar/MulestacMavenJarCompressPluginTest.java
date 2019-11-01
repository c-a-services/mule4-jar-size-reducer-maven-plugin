package io.github.c_a_services.mule4.jar;

import static org.junit.Assert.*;

import java.io.InputStream;
import java.util.Set;

import org.junit.Test;

/**
 * 
 */
public class MulestacMavenJarCompressPluginTest {

	@Test
	public void testConvertGoOfflineOutputToFileName() {
		MulestacMavenJarCompressPlugin tempPlugin = new MulestacMavenJarCompressPlugin();

		InputStream tempIn = this.getClass().getResourceAsStream("/go-offline-example.txt");
		Set<String> tempRelativeRepositoryNames = tempPlugin.convertGoOfflineOutputToFiles(tempIn);
		fail("Not yet implemented");
	}

}
