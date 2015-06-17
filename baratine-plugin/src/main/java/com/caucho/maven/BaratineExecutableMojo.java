package com.caucho.maven;

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

public abstract class BaratineExecutableMojo extends AbstractMojo
{
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

}
