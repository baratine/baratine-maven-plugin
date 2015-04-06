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
public class BaratineMojo extends AbstractMojo
{
  private static final String[] EXCLUDES = new String[]{"META-INF/baratine/**"};

  private static final String[] INCLUDES = new String[]{"**/**"};

  @Parameter
  private String[] includes;

  @Parameter
  private String[] excludes;

  @Parameter
  private boolean includeBaratine = false;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File outputDirectory;

  @Parameter(alias = "barName", property = "bar.finalName", defaultValue = "${project.build.finalName}")
  private String barName;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver archiver;

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter()
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  @Component
  private MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  protected static File getBarFile(File basedir,
                                   String finalName)
  {
    return new File(basedir, finalName + ".bar");
  }

  protected File getClassesDirectory()
  {
    return classesDirectory;
  }

  public File createBar()
    throws MojoExecutionException
  {
    File bar = getBarFile(outputDirectory, barName);

    MavenArchiver archiver = new MavenArchiver();

    archiver.setArchiver(this.archiver);

    archiver.setOutputFile(bar);

    try {
      File contentDirectory = getClassesDirectory();
      this.archiver.addDirectory(contentDirectory,
                                 "classes/",
                                 getIncludes(),
                                 getExcludes());

      String baratineMetaName = "META-INF"
                                + File.separatorChar
                                + "baratine";

      File baratineMeta = new File(contentDirectory, baratineMetaName);

      if (baratineMeta.exists())
        this.archiver.addDirectory(baratineMeta,
                                   baratineMetaName
                                   + File.separatorChar);

      for (Object obj : project.getArtifacts()) {
        Artifact a = (Artifact) obj;

        if (!"jar".equals(a.getType()))
          continue;

        if (!includeBaratine && "io.baratine".equals(a.getGroupId())) {
          getLog().info("skipping artifact "
                        + a.getId()
                        + ':'
                        + a.getArtifactId()
                        + ':'
                        + a.getSelectedVersion());

          continue;
        }

        File file = a.getFile();
        String name = file.getName();

        int lastSlash = name.lastIndexOf(File.separator);

        if (lastSlash > -1)
          name = name.substring(lastSlash + 1);

        this.archiver.addFile(file, "lib/" + name);
      }

      archiver.createArchive(session, project, archive);

      return bar;
    } catch (Exception e) {
      throw new MojoExecutionException("Error assembling bar", e);
    }
  }

  protected final MavenProject getProject()
  {
    return project;
  }

  public void execute() throws MojoExecutionException, MojoFailureException
  {
    File bar = createBar();

    getProject().getArtifact().setFile(bar);
  }

  private String[] getIncludes()
  {
    if (includes != null && includes.length > 0) {
      return includes;
    }
    return INCLUDES;
  }

  private String[] getExcludes()
  {
    if (excludes != null && excludes.length > 0) {
      return excludes;
    }
    return EXCLUDES;
  }
}
