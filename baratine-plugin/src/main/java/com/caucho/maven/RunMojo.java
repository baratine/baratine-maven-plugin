package com.caucho.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mojo(name = "run", defaultPhase = LifecyclePhase.PACKAGE,
      requiresProject = true, threadSafe = true,
      requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RunMojo extends AbstractMojo
{
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter(defaultValue = "${project.build.directory}", required = true)
  private File outputDirectory;

  @Parameter(alias = "barName", property = "bar.finalName",
             defaultValue = "${project.build.finalName}")
  private String barName;

  public void execute() throws MojoExecutionException, MojoFailureException
  {
    Map artifacts = project.getArtifactMap();

    Artifact baratineApi = (Artifact) artifacts.get("io.baratine:baratine-api");
    Artifact baratine = (Artifact) artifacts.get("io.baratine:baratine");

    String cp = baratine.getFile().getAbsolutePath();
    cp = cp + File.pathSeparatorChar;
    cp = cp + baratineApi.getFile().getAbsolutePath();

    String javaHome = System.getProperty("java.home");

    List<String> command = new ArrayList<>();
    command.add(javaHome + "/bin/java");
    command.add("-cp");
    command.add(cp);
    command.add("com.caucho.cli.baratine.BaratineCommandLine");

    ExecutorService x = Executors.newFixedThreadPool(3);

    try {
      ProcessBuilder processBuilder = new ProcessBuilder(command);
      Process process = processBuilder.start();

      InputStream in = process.getInputStream();
      InputStream err = process.getErrorStream();
      OutputStream out = process.getOutputStream();

      x.submit(new StreamPiper(in, System.out));
      x.submit(new StreamPiper(err, System.err));
      x.submit(new StreamPiper(System.in, out));

      out.write("start -bg\n".getBytes());
      out.flush();

      Thread.sleep(2 * 1000);

      out.write(("deploy " + getBarLocation() + "\n").getBytes());
      out.flush();

      getLog().info("Baratine terminated: " + process.waitFor());
    } catch (Exception e) {
      String message = String.format("exception running baratine %1$s",
                                     e.getMessage());
      throw new MojoExecutionException(message, e);
    } finally {
      x.shutdown();
      try {
        x.awaitTermination(1, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        e.printStackTrace();
      } finally {
        x.shutdownNow();
      }
    }
  }

  private String getBarLocation() throws MojoExecutionException
  {
    File bar;

    if (barName.endsWith("-SNAPSHOT")) {
      String copy = barName.substring(0, barName.length() - 9);
      Path source = FileSystems.getDefault()
                               .getPath(outputDirectory.getAbsolutePath(),
                                        barName + ".bar");
      Path to = FileSystems.getDefault()
                           .getPath(outputDirectory.getAbsolutePath(),
                                    copy + ".bar");
      try {
        Files.copy(source, to, StandardCopyOption.REPLACE_EXISTING);
      } catch (IOException e) {
        String message = String.format("error copying file %1$s %2$s",
                                       source,
                                       to);
        throw new MojoExecutionException(message, e);
      }

      bar = to.toFile();
    }
    else {
      bar = new File(outputDirectory, barName + ".bar");
    }

    return bar.getAbsolutePath();
  }
}

class StreamPiper implements Runnable
{
  InputStream _in;
  OutputStream _out;

  public StreamPiper(InputStream in, OutputStream out)
  {
    _in = in;
    _out = out;
  }

  @Override
  public void run()
  {
    byte[] buffer = new byte[256];

    int i;

    try {
      while ((i = _in.read(buffer)) > 0) {
        _out.write(buffer, 0, i);
        _out.flush();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}