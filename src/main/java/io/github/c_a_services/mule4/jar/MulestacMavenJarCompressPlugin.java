package io.github.c_a_services.mule4.jar;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;

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

	/**
	 * Default of dependency:go-offline
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
		tempLog.info("compress...");
		tempLog.info("sourceFile=" + getSourceFile());
		tempLog.info("destinationFile=" + getTemporaryFile());
		tempLog.info("keepTemporaryFile=" + keepTemporaryFile);
		tempLog.info("dependencyFolder=" + getBasedir());
		try {
			doExecute();
		} catch (IOException e) {
			throw new MojoExecutionException("Error", e);
		}
		tempLog.info("compress finished...");
	}

	/**
	 * @throws ParserConfigurationException
	 * @throws TransformerException
	 * @throws IOException
	 * @throws SAXException
	 *
	 */
	private void doExecute() throws IOException {
		ZipCompressHelper tempZipCompressHelper = new ZipCompressHelper(getLog());
		tempZipCompressHelper.setDependencyFolder(getBasedir());
		ZipContentReplacer tempReplacer = new ZipContentReplacer() {
			@Override
			@SuppressWarnings("unused")
			public InputStream replace(String aName, File aLocalFile, InputStream aIn) {
				return new ByteArrayInputStream(ZipCompressHelper.REPLACED_BYTES);
			}
		};
		File tempSourceFile = getSourceFile();
		File tempDestinationFile = getTemporaryFile();
		tempZipCompressHelper.copyZip(tempSourceFile, tempDestinationFile, tempReplacer);
		getLog().info(tempSourceFile.getName() + " Size=" + tempSourceFile.length());
		getLog().info(tempDestinationFile.getName() + " Size=" + tempDestinationFile.length());

		if (keepTemporaryFile) {
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

}