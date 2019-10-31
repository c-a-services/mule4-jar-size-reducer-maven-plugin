package io.github.c_a_services.mule4.jar.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.attribute.FileTime;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
	}

	/**
	 *
	 */
	private static final byte[] REPLACED_BYTES = "REPLACED".getBytes();

	private String dependencyFolder;

	/**
	 *
	 */
	private boolean isReplaced(String aName) {
		return aName.startsWith("repository/") && aName.endsWith(".jar") && aName.indexOf("SNAPSHOT") == -1;
	}

	/**
	 * @throws IOException
	 *
	 */
	public void copyZip(File aSourceFile, File aDestinationFile, ZipContentReplacer aReplacer) throws IOException {
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
					} else {
						try {
							InputStream tempEntryStream;
							if (isReplaced(tempName)) {
								File tempLocalFile = new File(getDependencyFolder() + "/" + tempName.substring(tempName.indexOf("/") + 1));
								if (tempLocalFile.exists()) {
									log.info("Replace content:" + tempName);
									tempEntryStream = aReplacer.replace(tempName, tempLocalFile, tempIn);
								} else {
									log.info("Keep content:" + tempName + " as not existing: " + tempLocalFile.getAbsolutePath());
									tempEntryStream = tempIn;
								}
							} else {
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
		tempOutEntry.setMethod(ZipEntry.DEFLATED);
		return tempOutEntry;
	}

	/**
	 * @see #dependencyFolder
	 */
	public String getDependencyFolder() {
		return dependencyFolder;
	}

	/**
	 * @see #dependencyFolder
	 */
	public void setDependencyFolder(String aDependencyFolder) {
		dependencyFolder = aDependencyFolder;
	}

	/**
	 * @see #REPLACED_BYTES
	 */
	public static byte[] getReplacedBytes() {
		return REPLACED_BYTES;
	}
}
