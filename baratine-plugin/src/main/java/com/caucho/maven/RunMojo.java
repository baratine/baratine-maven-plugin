package com.caucho.maven;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Mojo(name = "run", defaultPhase = LifecyclePhase.NONE, requiresProject = true,
  threadSafe = true, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class RunMojo extends BaratineExecutableMojo
{
  @Parameter(defaultValue = "${session}", readonly = true, required = true)
  private MavenSession session;

  @Parameter
  private String script;

  @Parameter(property = "baratine.run.skip")
  private boolean runSkip = false;

  @Parameter(property = "baratine.run.verbose")
  private boolean verbose = false;

  @Parameter(property = "baratine.run.external")
  private boolean external = false;

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    if (runSkip)
      return;

    if (external) {
      executeExternal();
    }
    else {
      try {
        executeInternal();
      } catch (Exception e) {
        throw new MojoExecutionException(e.getMessage(), e);
      }
    }
  }

  private void executeInternal()
    throws MojoFailureException, MojoExecutionException, IOException,
    ScriptException, InterruptedException
  {
    ScriptEngine script = getScriptEngine();

    if (script != null)
      executeInternal(script);
    else {
      getLog().warn(
        "can't obtain Baratine ScriptEngine falling back to executing Baratine in a separate process");
      executeExternal();
    }
  }

  private ScriptEngine getScriptEngine()
  {
    String baratine = getBaratine();
    String baratineApi = getBaratineApi();

    List<URL> urls = new ArrayList<>();

    ScriptEngine script = null;
    try {
      addUrl(urls, baratine);
      addUrl(urls, baratineApi);

      URLClassLoader cl
        = new URLClassLoader(urls.toArray(new URL[urls.size()]));

      script = new ScriptEngineManager(cl).getEngineByName("baratine");
    } catch (Exception e) {
      getLog().debug(e.getMessage(), e);
    }

    return script;
  }

  private void addUrl(List<URL> urls, String file) throws MalformedURLException
  {
    urls.add(new File(file).toURL());
  }

  private void executeInternal(ScriptEngine script)
    throws IOException, ScriptException, MojoExecutionException,
    InterruptedException
  {
    cleanWorkDir();

    Object obj = script.eval(getStartCmd());
    System.out.println(obj);
    Set artifacts = project.getDependencyArtifacts();

    for (Object a : artifacts) {
      Artifact artifact = (Artifact) a;
      if (!"bar".equals(artifact.getType()))
        continue;

      String file = getDeployableBar(artifact);

      obj = script.eval(getDeployCmd(file));
      System.out.print(obj);

      Thread.sleep(this.deployInterval * 1000);
    }

    obj = script.eval(getDeployCmd(getBarLocation()));
    System.out.println(obj);

    Thread.sleep(this.deployInterval * 1000);

    if (this.script != null) {
      byte[] buf = this.script.getBytes(StandardCharsets.UTF_8);

      int i = 0;

      for (; i < buf.length && buf[i] == ' '; i++) ;

      int start = i;

      getLog().info("running Baratine Script");

      for (; i < buf.length; i++) {
        if (buf[i] == '\n' || i == buf.length - 1) {
          int len = i - start;
          if (i == buf.length - 1)
            len += 1;

          String scriptCmd = new String(buf, start, len);
          System.out.println("baratine>" + scriptCmd);

          obj = script.eval((scriptCmd));
          System.out.println(obj);

          for (; i < buf.length && (buf[i] == ' ' || buf[i] == '\n'); i++) ;

          start = i;
        }
      }
    }

    System.out.println("baratine>status");
    obj = script.eval("status");
    System.out.println(obj);
  }

  public void executeExternal()
    throws MojoExecutionException, MojoFailureException
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

    ExecutorService x = Executors.newFixedThreadPool(3);
    Process process = null;

    try {
      cleanWorkDir();

      ProcessBuilder processBuilder = new ProcessBuilder(command);
      process = processBuilder.start();

      InputStream in = process.getInputStream();
      InputStream err = process.getErrorStream();
      OutputStream out = process.getOutputStream();

      x.submit(new StreamPiper(in, System.out));
      x.submit(new StreamPiper(err, System.err));
      x.submit(new StreamPiper(System.in, out));

      out.write(getStartCmd().getBytes());
      out.flush();
      Thread.sleep(2 * 1000);

      Set artifacts = project.getDependencyArtifacts();

      for (Object a : artifacts) {
        Artifact artifact = (Artifact) a;
        if (!"bar".equals(artifact.getType()))
          continue;

        String file = getDeployableBar(artifact);

        out.write(getDeployCmd(file).getBytes());
        out.flush();

        Thread.sleep(this.deployInterval * 1000);
      }

      out.write(getDeployCmd(getBarLocation()).getBytes());
      out.flush();

      Thread.sleep(deployInterval * 1000);

      if (script != null) {
        byte[] buf = script.getBytes(StandardCharsets.UTF_8);

        int i = 0;

        for (; i < buf.length && buf[i] == ' '; i++) ;

        int start = i;

        getLog().info("running Baratine Script");

        for (; i < buf.length; i++) {
          if (buf[i] == '\n' || i == buf.length - 1) {
            int len = i - start;
            if (i == buf.length - 1)
              len += 1;

            String scriptCmd = new String(buf, start, len);
            System.out.println("baratine>" + scriptCmd);

            out.write((scriptCmd + '\n').getBytes());
            out.flush();
            Thread.sleep(400);

            for (; i < buf.length && (buf[i] == ' ' || buf[i] == '\n'); i++) ;

            start = i;
          }
        }
      }

      getLog().info("Baratine terminated: " + process.waitFor());
    } catch (Exception e) {
      String message = String.format("exception running baratine %1$s",
                                     e.getMessage());
      throw new MojoExecutionException(message, e);
    } finally {
      try {
        x.shutdown();
        x.awaitTermination(1, TimeUnit.SECONDS);
      } catch (Throwable t) {
      } finally {
        x.shutdownNow();
      }

      try {
        if (process != null)
          process.waitFor(2, TimeUnit.SECONDS);
      } catch (Throwable t) {
      } finally {
        if (process.isAlive())
          process.destroyForcibly();
      }
    }
  }

  private String getStartCmd()
  {
    String cmd = "start -bg";

    if (this.conf != null)
      cmd += " --conf " + this.conf.getAbsolutePath();

    cmd += " --root-dir " + this.workDir;

    cmd += " -p " + this.port;

    if (this.verbose)
      cmd += " -vv";

    cmd += "\n";

    return cmd;
  }

  private String getDeployCmd(String file)
  {
    return String.format("deploy %1$s\n", file);
  }

  static class StreamPiper implements Runnable
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
}

