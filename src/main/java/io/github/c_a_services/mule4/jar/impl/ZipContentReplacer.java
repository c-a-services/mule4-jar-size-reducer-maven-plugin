package io.github.c_a_services.mule4.jar.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.apache.maven.plugin.MojoExecutionException;

/**
 *
 */
public interface ZipContentReplacer {

	/**
	 * @throws IOException
	 * @throws MojoExecutionException
	 *
	 */
	InputStream replace(String aNameWithoutRepositoryPrefix, File aLocalFile, InputStream aIn) throws IOException, MojoExecutionException;

	/**
	 *
	 */
	boolean skipEntry(String aName);

}
