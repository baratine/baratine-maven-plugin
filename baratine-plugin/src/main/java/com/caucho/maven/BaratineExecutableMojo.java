package com.caucho.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class BaratineExecutableMojo extends AbstractMojo
{
  private static final String baratineGroupId = "io.baratine";
  private static final String baratineId = "baratine";
  private static final String baratineApiId = "baratine-api";

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  protected MavenProject project;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  protected File outputDirectory;

  @Parameter(alias = "barName", property = "bar.finalName",
    defaultValue = "${project.build.finalName}")
  protected String barName;

  @Parameter(defaultValue = "8085", property = "baratine.port")
  protected int port;

  @Parameter(defaultValue = "${java.io.tmpdir}/baratine",
    property = "baratine.workDir")
  protected String workDir;

  protected FileSystem _fileSystem = FileSystems.getDefault();

  protected String getBarLocation() throws MojoExecutionException
  {
    String id = project.getArtifactId();

    Path source
      = _fileSystem.getPath(outputDirectory.getAbsolutePath(),
                            barName + ".bar");
    Path to
      = _fileSystem.getPath(outputDirectory.getAbsolutePath(),
                            id + ".bar");
    try {
      Files.copy(source, to, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      String message = String.format("error copying file %1$s %2$s",
                                     source,
                                     to);
      throw new MojoExecutionException(message, e);
    }

    File bar = to.toFile();

    return bar.getAbsolutePath();
  }

  protected String getDeployableBar(Artifact artifact) throws IOException
  {
    final File sourceFile = artifact.getFile();
    final String source = sourceFile.getName();

    Pattern p = Pattern.compile("\\-[\\.\\d+]+[-SNAPSHOT\\.]+");

    Matcher m = p.matcher(source);

    String target = m.replaceAll(".");

    Path from = _fileSystem.getPath(sourceFile.getAbsolutePath());
    Path to = _fileSystem.getPath("/tmp", target);

    Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING);

    return to.toString();
  }

  public String getBaratine()
  {
    String path = getArtifact(baratineGroupId, baratineId);

    if (path == null)
      path = getDependency(baratineGroupId, baratineId);

    return path;
  }

  public String getBaratineApi()
  {
    String path = getArtifact(baratineGroupId, baratineApiId);

    if (path == null)
      path = getDependency(baratineGroupId, baratineApiId);

    return path;
  }

  public String getArtifact(String groupId, String artifactId)
  {
    Artifact a
      = (Artifact) project.getArtifactMap().get(groupId + ':' + artifactId);

    String path = null;
    if (a != null)
      path = a.getFile().getAbsolutePath();

    return path;
  }

  public String getDependency(String groupId, String artifactId)
  {
    List dependencies = project.getDependencies();

    for (int i = 0; i < dependencies.size(); i++) {
      Dependency dependency = (Dependency) dependencies.get(i);
      if (groupId.equals(dependency.getGroupId())
          && artifactId.equals(dependency.getArtifactId())) {
        return dependency.getSystemPath();
      }
    }

    return null;
  }

  public void cleanWorkDir() throws IOException
  {
    File workDir = new File(this.workDir);

    File dir = new File(workDir, "data-" + port);

    if (dir.exists())
      delete(dir);

    dir = new File(workDir, "log");

    if (dir.exists())
      delete(dir);
  }

  private void delete(File directory)
  {
    File[] files;
    Stack<File> stack = new Stack<>();
    stack.push(directory);
    while (!stack.isEmpty()) {
      if (stack.lastElement().isFile()) {
        stack.pop().delete();
      }
      else {
        files = stack.lastElement().listFiles();

        if (files.length == 0)
          stack.pop().delete();
        else
          for (File file : files) {
            stack.push(file);
          }
      }
    }
  }
}
