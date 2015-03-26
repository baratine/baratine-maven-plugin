package com.caucho.maven;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

@Mojo(name = "bar", defaultPhase = LifecyclePhase.PACKAGE, requiresProject = true, threadSafe = true,
  requiresDependencyResolution = ResolutionScope.RUNTIME)
public class BarMojo extends AbstractMojo
{
  public void execute() throws MojoExecutionException, MojoFailureException
  {
    System.out.println("Main.execute");
  }
}
