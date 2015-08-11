package com.caucho.maven;

import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.archiver.MavenArchiver;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Mojo(name = "baratine", defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BaratineMojo extends BaratineBaseMojo
{
  private static final String[] EXCLUDES = new String[]{"META-INF/baratine/**"};

  private static final String[] INCLUDES = new String[]{"**/**"};

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter
  private String[] includes;

  @Parameter
  private String[] excludes;

  @Parameter
  private boolean includeBaratine = false;

  @Component(role = Archiver.class, hint = "jar")
  private JarArchiver archiver;

  @Parameter()
  private MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

  @Component
  private MavenProjectHelper projectHelper;

  @Parameter(defaultValue = "${project.build.outputDirectory}", required = true)
  private File classesDirectory;

  @Parameter(required = false)
  private String executableName;

  @Parameter(defaultValue = "true", required = false)
  private boolean buildExecutable;

  protected File getBarFile()
  {
    return new File(outputDirectory, barName + ".bar");
  }

  protected File getExecutableBarFile()
  {
    return new File(outputDirectory, "executable-" + barName + ".jar");
  }

  protected File getClassesDirectory()
  {
    return classesDirectory;
  }

  public File createBar()
    throws MojoExecutionException
  {
    File bar = getBarFile();

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

      for (Object x : project.getArtifacts()) {
        Artifact a = (Artifact) x;

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

      File web = new File(project.getBasedir(),
                          "src"
                          + File.separatorChar
                          + "main"
                          + File.separatorChar
                          + "web");

      if (web.exists())
        this.archiver.addDirectory(web, "web/");

      archiver.createArchive(session, project, archive);

      return bar;
    } catch (Exception e) {
      throw new MojoExecutionException("Error assembling bar", e);
    }
  }

  private void buildExecutable(File bar) throws MojoExecutionException
  {
    String cp = getBaratine();
    cp = cp + File.pathSeparatorChar;
    cp = cp + getBaratineApi();

    String javaHome = System.getProperty("java.home");

    List<String> command = new ArrayList<>();
    command.add(javaHome + "/bin/java");
    command.add("-cp");
    command.add(cp);
    command.add("com.caucho.cli.baratine.BaratineCommandLine");

    command.add("package");
    command.add("-o");
    command.add(getExecutableBarFile().getAbsolutePath());
    command.add(bar.getAbsolutePath());

    Set artifacts = project.getDependencyArtifacts();

    for (Object a : artifacts) {
      Artifact artifact = (Artifact) a;
      if (!"bar".equals(artifact.getType()))
        continue;

      command.add(artifact.getFile().getAbsolutePath());
    }

    Process process = null;
    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);

      processBuilder.redirectError(ProcessBuilder.Redirect.INHERIT)
                    .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                    .redirectInput(ProcessBuilder.Redirect.INHERIT);

      process = processBuilder.start();

      process.waitFor(120, TimeUnit.SECONDS);

      if (!process.isAlive()) {
        getLog().info("Baratine terminated: " + process.exitValue());
      }
    } catch (Exception e) {
      String message = String.format("exception running Baratine package %1$s",
                                     e.getMessage());
      throw new MojoExecutionException(message, e);
    } finally {
      if (process.isAlive()) {
        getLog().info("Baratine package timed out, will terminate");
        process.destroyForcibly();
      }
    }
  }

  protected final MavenProject getProject()
  {
    return project;
  }

  public void execute() throws MojoExecutionException, MojoFailureException
  {
    File bar = createBar();

    if (buildExecutable)
      buildExecutable(bar);

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
