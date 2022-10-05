package io.github.c_a_services.mule4.jar.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.attribute.FileTime;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

/**
 *
 */
public class ZipCompressHelper {

	private Log log;

	/**
	 * @param aLog
	 *
	 */
	public ZipCompressHelper(Log aLog) {
		super();
		log = aLog;
		try {
			REPLACED_BYTES = "REPLACED".getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			log.error("UTF-8 not available, try system charset " + Charset.defaultCharset() + " for REPLACED_BYTES", e);
			REPLACED_BYTES = "REPLACED".getBytes();
		}
	}

	/**
	 *
	 */
	private byte[] REPLACED_BYTES;

	private String mavenLocalRepositoryFolder;

	/**
	 *
	 */
	private boolean isReplaced(String aName) {
		return aName.startsWith("repository/") && aName.endsWith(".jar") && aName.indexOf("SNAPSHOT") == -1;
	}

	/**
	 * @throws IOException
	 * @throws MojoExecutionException
	 *
	 */
	public void copyZip(File aSourceFile, File aDestinationFile, ZipContentReplacer aReplacer) throws IOException, MojoExecutionException {
		File tempDestinationDir = aDestinationFile.getParentFile();
		if (!tempDestinationDir.exists()) {
			if (!tempDestinationDir.mkdirs()) {
				throw new IOException("Cannot create directory " + tempDestinationDir + " for file " + aDestinationFile.getAbsolutePath());
			}
		}
		byte[] tempBuff = new byte[1024 * 1024];
		try (ZipInputStream tempIn = new ZipInputStream(new FileInputStream(aSourceFile))) {
			try (ZipOutputStream tempOut = new ZipOutputStream(new FileOutputStream(aDestinationFile))) {
				tempOut.setLevel(Deflater.BEST_COMPRESSION);
				ZipEntry tempEntry = tempIn.getNextEntry();
				while (tempEntry != null) {
					String tempName = tempEntry.getName();
					log.debug("Entry=" + tempName);
					if (tempEntry.isDirectory()) {
						// skip no content
					} else if (aReplacer.skipEntry(tempName)) {
						log.debug("Skip entry: " + tempName);
					} else {
						try {
							InputStream tempEntryStream;
							if (isReplaced(tempName)) {
								String tempWithoutRepositoryPrefix = tempName.substring(tempName.indexOf("/") + 1);
								// files in the jar are placed in repository/ folder.
								log.debug("Replace content: " + tempName);
								File tempLocalFile = new File(getMavenLocalRepositoryFolder() + "/" + tempWithoutRepositoryPrefix);

								// see MulestacMavenJarCompressPlugin.java
								// see MulestacMavenJarRefillPlugin.java
								tempEntryStream = aReplacer.replace(tempWithoutRepositoryPrefix, tempLocalFile, tempIn);
							} else {
								log.debug("Not matching replace pattern. Keep content: " + tempName);
								tempEntryStream = tempIn;
							}
							ZipEntry tempOutEntry = copyZipEntry(tempEntry);
							tempOut.putNextEntry(tempOutEntry);
							int tempRead = tempEntryStream.read(tempBuff);
							while (tempRead > 0) {
								tempOut.write(tempBuff, 0, tempRead);
								tempRead = tempEntryStream.read(tempBuff);
							}
							tempOut.closeEntry();
						} catch (IOException | RuntimeException e) {
							throw new RuntimeException("Error processing " + tempEntry, e);
						}
					}
					tempEntry = tempIn.getNextEntry();
				}
			}
		}
	}

	/**
	 *
	 */
	private ZipEntry copyZipEntry(ZipEntry anEntry) {
		ZipEntry tempOutEntry = new ZipEntry(anEntry.getName());
		String tempComment = anEntry.getComment();
		if (tempComment != null) {
			tempOutEntry.setComment(tempComment);
		}
		FileTime tempCreationTime = anEntry.getCreationTime();
		if (tempCreationTime != null) {
			tempOutEntry.setCreationTime(tempCreationTime);
		}
		FileTime tempLastAccessTime = anEntry.getLastAccessTime();
		if (tempLastAccessTime != null) {
			tempOutEntry.setLastAccessTime(tempLastAccessTime);
		}
		FileTime tempLastModifiedTime = anEntry.getLastModifiedTime();
		if (tempLastModifiedTime != null) {
			tempOutEntry.setLastModifiedTime(tempLastModifiedTime);
		}
		tempOutEntry.setTime(anEntry.getTime());
		tempOutEntry.setExtra(anEntry.getExtra());
		tempOutEntry.setMethod(ZipEntry.DEFLATED);
		return tempOutEntry;
	}

	/**
	 * @see #mavenLocalRepositoryFolder
	 */
	public String getMavenLocalRepositoryFolder() {
		return mavenLocalRepositoryFolder;
	}

	/**
	 * @see #mavenLocalRepositoryFolder
	 */
	public void setMavenLocalRepositoryFolder(String aDependencyFolder) {
		mavenLocalRepositoryFolder = aDependencyFolder;
	}

	/**
	 * @see #REPLACED_BYTES
	 */
	public byte[] getReplacedBytes() {
		return REPLACED_BYTES;
	}
}
