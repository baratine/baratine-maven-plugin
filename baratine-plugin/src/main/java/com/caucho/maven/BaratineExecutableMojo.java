package com.caucho.maven;

import jdk.nashorn.internal.runtime.regexp.joni.Regex;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

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

  protected String getBarLocation() throws MojoExecutionException
  {
    String id = project.getArtifactId();

    Path source = FileSystems.getDefault()
                             .getPath(outputDirectory.getAbsolutePath(),
                                      barName + ".bar");
    Path to = FileSystems.getDefault()
                         .getPath(outputDirectory.getAbsolutePath(),
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

  protected String getDeployableBar(Artifact artifact) {
    String source = artifact.getFile().getAbsolutePath();

    String target = source.replace("-SNAPSHOT.", ".");

    source =
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
}
