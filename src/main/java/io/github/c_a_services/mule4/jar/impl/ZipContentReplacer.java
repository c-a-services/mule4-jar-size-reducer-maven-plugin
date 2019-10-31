package io.github.c_a_services.mule4.jar.impl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public interface ZipContentReplacer {

	/**
	 * @throws IOException
	 *
	 */
	InputStream replace(String aName, File aLocalFile, InputStream aIn) throws IOException;

}
