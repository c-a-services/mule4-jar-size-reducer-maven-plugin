package io.github.c_a_services.mule4.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;

import io.github.c_a_services.mule4.jar.impl.ZipCompressHelper;
import io.github.c_a_services.mule4.jar.impl.ZipContentReplacer;

/**
 * Format all xml files for easier compare e.g. mule3 vs mule4 or feature branches.
 *
 * Usage:
 *
 * mvn dependency:go-offline io.github.c-a-services:mule4-jar-size-reducer-maven-plugin:LATEST:jar-compress
 *
 */
@Mojo(name = "jar-compress")
public class MulestacMavenJarCompressPlugin extends AbstractMojo {

	/*
	<configuration>
		<sourceFile>${basedir}/target/${project.artifactId}-${project.version}-mule-application.jar</sourceFile>
		<destinationFile>${basedir}/target/${project.artifactId}-${project.version}-mule-application.zip</destinationFile>
	</configuration>
	*/
	@Parameter(property = "sourceFile", required = true, //
			defaultValue = "${basedir}/target/${project.artifactId}-${project.version}-mule-application.jar")
	private File sourceFile;

	@Parameter(property = "temporaryFile", required = true, //
			defaultValue = "${basedir}/target/${project.artifactId}-${project.version}-mule-application-temp.jar")
	private File temporaryFile;

	@Parameter(property = "keepTemporaryFile", required = true, //
			defaultValue = "false")
	private boolean keepTemporaryFile;

	@Parameter(property = "compress-jar-skip", required = false, //
			defaultValue = "false")
	private boolean skip;

	/**
	 * Leave out all files that match on of the specified regEx.
	 */
	@Parameter
	private List<String> stripMatchingFiles;

	/**
	 * Default localRepository
	 */
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	private ArtifactRepository localRepository;

	/**
	 *
	 */
	protected String getBasedir() {
		return localRepository.getBasedir();
	}

	@Override
	public void execute() throws MojoExecutionException {
		Log tempLog = getLog();
		tempLog.info("System Charset=" + Charset.defaultCharset());
		tempLog.info("sourceFile=" + getSourceFile());
		tempLog.info("destinationFile=" + getTemporaryFile());
		tempLog.info("keepTemporaryFile=" + isKeepTemporaryFile());
		tempLog.info("dependencyFolder=" + getBasedir());
		tempLog.info("stripFilesMatching=" + getStripMatchingFiles());
		if (skip) {
			tempLog.warn("Compress is skipped.");
		} else {
			tempLog.info("compress...");
			try {
				doExecute();
			} catch (IOException e) {
				throw new MojoExecutionException("Error", e);
			}
			tempLog.info("compress finished...");
		}
	}

	/**
	 * @throws IOException
	 * @throws MojoExecutionException
	 *
	 */
	private void doExecute() throws IOException, MojoExecutionException {
		ZipCompressHelper tempZipCompressHelper = new ZipCompressHelper(getLog());
		tempZipCompressHelper.setMavenLocalRepositoryFolder(getBasedir());
		byte[] tempReplacedBytes = tempZipCompressHelper.getReplacedBytes();
		ZipContentReplacer tempReplacer = new ZipContentReplacer() {
			@Override
			public InputStream replace(String aName, File aLocalFile, InputStream aIn) {
				if (aLocalFile.exists()) {
					getLog().info("Replace content:" + aName);
					return new ByteArrayInputStream(tempReplacedBytes);
				}
				// normally should be available as it was packaged a few minutes before.
				getLog().warn("Keep content: " + aName + " as not existing: " + aLocalFile.getAbsolutePath());
				return aIn;
			}

			/**
			 *
			 */
			@Override
			public boolean skipEntry(String aName) {
				List<String> tempStripMatchingFiles = getStripMatchingFiles();
				if (tempStripMatchingFiles != null) {
					for (String tempRegex : tempStripMatchingFiles) {
						if (aName.matches(tempRegex)) {
							getLog().info("Remove entry " + aName + " as matching " + tempRegex);
							return true;
						}
					}
				}
				return false;
			}
		};
		File tempSourceFile = getSourceFile();
		File tempDestinationFile = getTemporaryFile();
		tempZipCompressHelper.copyZip(tempSourceFile, tempDestinationFile, tempReplacer);
		getLog().info(tempSourceFile.getName() + " Size=" + tempSourceFile.length());
		getLog().info(tempDestinationFile.getName() + " Size=" + tempDestinationFile.length());

		if (isKeepTemporaryFile()) {
			// overwrite the original jar as it is pushed to nexus, too.
			FileUtils.copyFile(tempDestinationFile, tempSourceFile);
			getLog().info("Copied " + tempDestinationFile + " to " + tempSourceFile);
		} else {
			if (tempSourceFile.delete()) {
				if (tempDestinationFile.renameTo(tempSourceFile)) {
					getLog().info("Renamed " + tempDestinationFile + " to " + tempSourceFile);
				} else {
					throw new IOException("Could not rename " + tempDestinationFile + " to " + tempSourceFile);
				}
			} else {
				throw new IOException("Could not delete " + tempSourceFile);
			}
		}
	}

	/**
	 * @see #sourceFile
	 */
	public File getSourceFile() {
		return sourceFile;
	}

	/**
	 * @see #sourceFile
	 */
	public void setSourceFile(File aSourceFile) {
		sourceFile = aSourceFile;
	}

	/**
	 * @see #destinationFile
	 */
	public File getTemporaryFile() {
		return temporaryFile;
	}

	/**
	 * @see #destinationFile
	 */
	public void setTemporaryFile(File aDestinationFile) {
		temporaryFile = aDestinationFile;
	}

	/**
	 * @see #keepTemporaryFile
	 */
	public boolean isKeepTemporaryFile() {
		return keepTemporaryFile;
	}

	/**
	 * @see #keepTemporaryFile
	 */
	public void setKeepTemporaryFile(boolean aKeepTemporaryFile) {
		keepTemporaryFile = aKeepTemporaryFile;
	}

	/**
	 * @see #stripMatchingFiles
	 */
	public List<String> getStripMatchingFiles() {
		return stripMatchingFiles;
	}

	/**
	 * @see #stripMatchingFiles
	 */
	public void setStripMatchingFiles(List<String> aStripFilesMatching) {
		stripMatchingFiles = aStripFilesMatching;
	}

	/**
	 * @see #localRepository
	 */
	public ArtifactRepository getLocalRepository() {
		return localRepository;
	}

	/**
	 * @see #localRepository
	 */
	public void setLocalRepository(ArtifactRepository aLocalRepository) {
		localRepository = aLocalRepository;
	}

}