package org.objectstyle.woproject.ant;

import java.util.LinkedList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.taskdefs.optional.junit.JUnitTask;

public class WOUnit extends JUnitTask {
  private List<FrameworkSet> frameworkSets;

  public WOUnit() throws Exception {
    frameworkSets = new LinkedList<FrameworkSet>();
  }

  public void addFrameworks(FrameworkSet frameworks) throws BuildException {
    frameworkSets.add(frameworks);
  }

  @Override
  public void execute() throws BuildException {
    getCommandline().createClasspath(getProject()).createPath().add(FrameworkSet.jarsPathForFrameworkSets(getProject(), frameworkSets));
    super.execute();
  }
}
