package io.github.c_a_services.mule4.jar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.BuildPluginManager;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import org.twdata.maven.mojoexecutor.MojoExecutor;

import io.github.c_a_services.mule4.jar.impl.ZipCompressHelper;
import io.github.c_a_services.mule4.jar.impl.ZipContentReplacer;

/**
 * Re-fill previous dependencies.
 *
 * Usage:
 *
 * mvn dependency:copy-dependencies io.github.c-a-services:mule4-jar-size-reducer-maven-plugin:LATEST:jar-refill
 *
 */
@Mojo(name = "jar-refill", requiresProject = false)
public class MulestacMavenJarRefillPlugin extends AbstractMojo {

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
	 * Again compress Jar files with BEST_Compression to have smallest target size.
	 */
	@Parameter(property = "reCompressJarFiles", required = false, //
			defaultValue = "false")
	private boolean reCompressJarFiles;

	/**
	 * Default of dependency:go-offline
	 */
	@Parameter(defaultValue = "${localRepository}", readonly = true, required = true)
	private ArtifactRepository localRepository;

	@Override
	public void execute() throws MojoExecutionException {
		Log tempLog = getLog();
		tempLog.info("refill...");
		tempLog.info("System Charset=" + Charset.defaultCharset());
		tempLog.info("sourceFile=" + getSourceFile());
		tempLog.info("destinationFile=" + getTemporaryFile());
		tempLog.info("keepTemporaryFile=" + isKeepTemporaryFile());
		tempLog.info("dependencyFolder=" + getBasedir());
		try {
			doExecute();
		} catch (IOException e) {
			throw new MojoExecutionException("Error", e);
		}
		tempLog.info("refill finished...");
	}

	/**
	 *
	 */
	protected String getBasedir() {
		return localRepository.getBasedir();
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
			public InputStream replace(String aNameWithoutRepositoryPrefix, File aLocalFile, InputStream aIn) throws IOException, MojoExecutionException {
				int tempExpectedLength = tempReplacedBytes.length;
				PushbackInputStream tempPushbackInputStream = new PushbackInputStream(aIn, tempExpectedLength);
				byte[] tempProbe = new byte[tempExpectedLength];
				int tempRead = tempPushbackInputStream.read(tempProbe);
				if (tempRead > 0) {
					tempPushbackInputStream.unread(tempProbe, 0, tempRead);
				}
				if (tempRead == tempExpectedLength) {
					if (Arrays.equals(tempProbe, tempReplacedBytes)) {
						if (!aLocalFile.exists()) {
							downloadArtifact(aNameWithoutRepositoryPrefix, aLocalFile);
						}
						if (reCompressJarFiles && (aLocalFile.getName().endsWith(".jar") || aLocalFile.getName().endsWith(".zip"))) {
							File tempReCompressedJarFile = tempZipCompressHelper.reCompressJarFile(aLocalFile);
							if (tempReCompressedJarFile != null) {
								if (tempReCompressedJarFile.length() >= aLocalFile.length()) {
									getLog().info("Recrompression did not create smaller file: reCompressionLargerSize=" + tempReCompressedJarFile.length()
											+ ". Take original file.");
									tempReCompressedJarFile.delete();
								} else {
									getLog().info("OriginalFileLength:" + aNameWithoutRepositoryPrefix + " with " + aLocalFile.length() + " bytes.");
									getLog().info("ReCompressedJarFileLength:" + tempReCompressedJarFile.getName() + " with " + tempReCompressedJarFile.length()
											+ " bytes.");
									return new FileInputStream(tempReCompressedJarFile);
								}
							}
						}
						getLog().info("Refill content:" + aNameWithoutRepositoryPrefix + " with " + aLocalFile.length() + " bytes.");
						return new FileInputStream(aLocalFile);
					} else {
						if (getLog().isDebugEnabled()) {
							getLog().debug("Probe of " + aNameWithoutRepositoryPrefix + " does not match ReplacedBytes: ");
							for (int i = 0; i < tempExpectedLength; i++) {
								getLog().debug(i + ":" + tempProbe[i] + " <> " + tempReplacedBytes[i]);
							}
						}
					}
				}
				getLog().info("Keep content of " + aNameWithoutRepositoryPrefix);
				return tempPushbackInputStream;
			}

			@SuppressWarnings("unused")
			@Override
			public boolean skipEntry(String aName) {
				// nothing is skipped when re-fill
				return false;
			}
		};
		File tempSourceFile = getSourceFile();
		File tempDestinationFile = getTemporaryFile();
		tempZipCompressHelper.copyZip(tempSourceFile, tempDestinationFile, tempReplacer);
		getLog().info(tempSourceFile.getName() + " SourceJarSize=" + tempSourceFile.length());
		getLog().info(tempDestinationFile.getName() + " RefilledJarSize=" + tempDestinationFile.length());

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

	@Component
	private MavenProject mavenProject;

	@Component
	private MavenSession mavenSession;

	@Component
	private BuildPluginManager pluginManager;

	/**
	 * @param aNameWithoutRepositoryPrefix
	 * @throws MojoExecutionException
	 *
	 */
	protected void downloadArtifact(String aNameWithoutRepositoryPrefix, File aLocalFile) throws MojoExecutionException {
		String tempArtifact = guessGAV(aNameWithoutRepositoryPrefix);

		// https://maven.apache.org/plugins/maven-dependency-plugin/get-mojo.html
		// https://github.com/TimMoore/mojo-executor
		try {
			Plugin tempDependencyPlugin = MojoExecutor.plugin( //
					MojoExecutor.groupId("org.apache.maven.plugins"), //
					MojoExecutor.artifactId("maven-dependency-plugin"));
			tempDependencyPlugin = getVersionOfPlugin(tempDependencyPlugin);
			getLog().info("Manually download " + tempArtifact + " via " + tempDependencyPlugin + ":" + tempDependencyPlugin.getVersion() + " ... ");
			MojoExecutor.executeMojo( //
					tempDependencyPlugin, //
					MojoExecutor.goal("get"), //
					MojoExecutor.configuration( //
							//	A string of the form groupId:artifactId:version[:packaging[:classifier]].
							MojoExecutor.element(MojoExecutor.name("artifact"), tempArtifact)//
							, MojoExecutor.element(MojoExecutor.name("transitive"), "false")//
					), //
					MojoExecutor.executionEnvironment( //
							mavenProject, //
							mavenSession, //
							pluginManager //
					));
		} catch (MojoExecutionException e) {
			throw new RuntimeException("Error getting " + tempArtifact + " for " + aNameWithoutRepositoryPrefix + " to " + aLocalFile.getAbsolutePath(), e);
		}
		if (aLocalFile.exists()) {
			getLog().info("Downloaded " + aLocalFile.length() + " bytes of " + aLocalFile.getName());
		} else {
			throw new MojoExecutionException(
					"Cannot replace " + aNameWithoutRepositoryPrefix + " as file " + aLocalFile.getAbsolutePath() + " is missing and could not be downloaded.");
		}
	}

	/**
	 *
	 */
	private Plugin getVersionOfPlugin(Plugin aDependencyPlugin) {
		String tempVersionOfPlugin = getVersionOfPlugin(mavenProject, aDependencyPlugin);
		if (tempVersionOfPlugin != null) {
			getLog().debug("Found version " + tempVersionOfPlugin + " for " + aDependencyPlugin);
			aDependencyPlugin.setVersion(tempVersionOfPlugin);
		}
		return aDependencyPlugin;
	}

	/**
	 *
	 */
	private String getVersionOfPlugin(MavenProject aMavenProject, Plugin aDependencyPlugin) {
		PluginManagement pm = aMavenProject.getPluginManagement();
		if (pm != null) {
			for (Plugin p : pm.getPlugins()) {
				if (aDependencyPlugin.getGroupId().equals(p.getGroupId()) && aDependencyPlugin.getArtifactId().equals(p.getArtifactId())) {
					String tempVersion = p.getVersion();
					getLog().debug("Found " + tempVersion + " in PluginManagement " + aMavenProject);
					return tempVersion;
				}
			}
		}
		List<Plugin> tempBuildPlugins = aMavenProject.getBuildPlugins();
		if (tempBuildPlugins != null) {
			for (Plugin p : tempBuildPlugins) {
				if (aDependencyPlugin.getGroupId().equals(p.getGroupId()) && aDependencyPlugin.getArtifactId().equals(p.getArtifactId())) {
					String tempVersion = p.getVersion();
					getLog().debug("Found " + tempVersion + " in BuildPlugins " + aMavenProject);
					return tempVersion;
				}
			}
		}
		MavenProject tempParent = aMavenProject.getParent();
		if (tempParent != null) {
			return getVersionOfPlugin(tempParent, aDependencyPlugin);
		}
		return null;
	}

	/**
	 * Pfad: org/codehaus/mojo/animal-sniffer-annotations/1.17/animal-sniffer-annotations-1.17.jar
	 *
	 *		A string of the form groupId:artifactId:version[:packaging[:classifier]].
	
		 https://maven.apache.org/plugins/maven-dependency-plugin/get-mojo.html
	 */
	String guessGAV(String aNameWithoutRepositoryPrefix) {
		try {
			return guessGAVimpl(aNameWithoutRepositoryPrefix);
		} catch (RuntimeException e) {
			String tempMessage = "Error getting GAV for " + aNameWithoutRepositoryPrefix;
			getLog().error(tempMessage, e);
			throw new RuntimeException(tempMessage, e);
		}
	}

	/**
	 *
	 */
	private String guessGAVimpl(String aNameWithoutRepositoryPrefix) {
		int tempLast = aNameWithoutRepositoryPrefix.lastIndexOf('/');
		String tempPath = aNameWithoutRepositoryPrefix.substring(0, tempLast);
		String tempFileName = aNameWithoutRepositoryPrefix.substring(tempLast + 1);
		int tempVersionPos = tempPath.lastIndexOf('/');
		int tempArtifactFolderPos = tempPath.substring(0, tempVersionPos).lastIndexOf('/');

		String tempGroupId = tempPath.substring(0, tempArtifactFolderPos).replace('/', '.');
		String tempVersion = tempPath.substring(tempVersionPos + 1);

		int tempVersionInFileNamePos = tempFileName.indexOf("-" + tempVersion);
		if (tempVersionInFileNamePos == -1) {
			throw new IllegalArgumentException("Did not find '-" + tempVersion + "' in " + tempFileName);
		}

		String tempArtifactId = tempFileName.substring(0, tempVersionInFileNamePos);
		int tempFileNameDotPos = tempFileName.lastIndexOf('.');
		String tempClassifier;
		int tempClassifierPos = tempVersionInFileNamePos + tempVersion.length() + 2;
		if (tempClassifierPos < tempFileNameDotPos) {
			tempClassifier = tempFileName.substring(tempClassifierPos, tempFileNameDotPos);
		} else {
			tempClassifier = null;
		}
		String tempPackaging = tempFileName.substring(tempFileNameDotPos + 1);

		String tempArtifact = tempGroupId + ':' + tempArtifactId + ':' + tempVersion + ':' + tempPackaging;

		if (tempClassifier != null && tempClassifier.length() > 0) {
			tempArtifact += ':' + tempClassifier;
		}
		return tempArtifact;
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

}