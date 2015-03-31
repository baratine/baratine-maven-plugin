package com.caucho.maven;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.codehaus.plexus.archiver.Archiver;
import org.codehaus.plexus.archiver.jar.JarArchiver;

import java.io.File;

@Mojo(name = "baratine", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BarMojo extends AbstractMojo
{
  private static final String[] excludes = new String[]{};

  private static final String[] includes = new String[]{"**/**"};

  @Parameter
  private String[] _includes;

  @Parameter
  private String[] _excludes;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File _outputDirectory;

  @Parameter(alias = "barName", property = "bar.finalName", defaultValue = "${project.build.finalName}")
  private String _finalName;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver _jarArchiver;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject _project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession _session;

  @Parameter()
  private MavenArchiveConfiguration _archive = new MavenArchiveConfiguration();

  @Parameter(defaultValue = "${project.build.outputDirectory}/META-INF/MANIFEST.MF", required = true,
    readonly = true)
  private File _defaultManifestFile;

  @Parameter(property = "jar.useDefaultManifestFile", defaultValue = "false")
  private boolean _useDefaultManifestFile;

  @Component
  private MavenProjectHelper _projectHelper;

  @Parameter(property = "jar.forceCreation", defaultValue = "false")
  private boolean _forceCreation;

  @Parameter(property = "jar.skipIfEmpty", defaultValue = "false")
  private boolean _skipIfEmpty;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File _classesDirectory;

  protected static File getJarFile(File basedir,
                                   String finalName,
                                   String classifier)
  {
    if (classifier == null) {
      classifier = "";
    }
    else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
      classifier = "-" + classifier;
    }

    return new File(basedir, finalName + classifier + ".jar");
  }

  protected File getClassesDirectory()
  {
    return _classesDirectory;
  }

  protected File getDefaultManifestFile()
  {
    return _defaultManifestFile;
  }

  public File createBar()
    throws MojoExecutionException
  {
    File jarFile = getJarFile(_outputDirectory, _finalName, null);

    MavenArchiver archiver = new MavenArchiver();

    archiver.setArchiver(_jarArchiver);

    archiver.setOutputFile(jarFile);

    _archive.setForced(_forceCreation);

    try {
      File contentDirectory = getClassesDirectory();
      if (!contentDirectory.exists()) {
        getLog().warn("JAR will be empty - no content was marked for inclusion!");
      }
      else {
        archiver.getArchiver().addDirectory(contentDirectory,
                                            "classes/",
                                            getIncludes(),
                                            getExcludes());
      }

      File existingManifest = getDefaultManifestFile();

      if (_useDefaultManifestFile
          && existingManifest.exists()
          && _archive.getManifestFile() == null) {
        getLog().info("Adding existing MANIFEST to archive. Found under: "
                      + existingManifest.getPath());
        _archive.setManifestFile(existingManifest);
      }

      for (Object obj : _project.getArtifacts()) {
        Artifact a = (Artifact) obj;
        File file = a.getFile();

        if ("jar".equals(a.getType())) {
          String name = file.getName();

          int lastSlash = name.lastIndexOf(File.separator);

          if (lastSlash > -1)
            name = name.substring(lastSlash + 1);

          archiver.getArchiver().addFile(file, name);
        }
      }

      archiver.createArchive(_session, _project, _archive);

      return jarFile;
    } catch (Exception e) {
      throw new MojoExecutionException("Error assembling bar", e);
    }
  }

  protected final MavenProject getProject()
  {
    return _project;
  }

  public void execute() throws MojoExecutionException, MojoFailureException
  {
    File bar = createBar();

    getProject().getArtifact().setFile(bar);
  }

  private String[] getIncludes()
  {
    if (_includes != null && _includes.length > 0) {
      return _includes;
    }
    return includes;
  }

  private String[] getExcludes()
  {
    if (_excludes != null && _excludes.length > 0) {
      return _excludes;
    }
    return excludes;
  }
}
