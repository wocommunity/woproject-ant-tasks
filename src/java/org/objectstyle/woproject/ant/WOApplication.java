/* ====================================================================
 *
 * The ObjectStyle Group Software License, Version 1.0
 *
 * Copyright (c) 2002 -2004 The ObjectStyle Group
 * and individual authors of the software.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        ObjectStyle Group (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "ObjectStyle Group" and "Cayenne"
 *    must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact andrus@objectstyle.org.
 *
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    nor may "ObjectStyle" appear in their names without prior written
 *    permission of the ObjectStyle Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the ObjectStyle Group.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 *
 */
package org.objectstyle.woproject.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.taskdefs.Chmod;
import org.apache.tools.ant.taskdefs.Copy;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.PatternSet;
import org.objectstyle.woenvironment.env.WOBuildPropertiesNotFoundException;
import org.objectstyle.woenvironment.env.WOEnvironment;

/**
 * Ant task to build WebObjects application. For detailed instructions go to the
 * <a href="../../../../../ant/woapplication.html">manual page </a>.
 * 
 * 
 * @ant.task category="packaging"
 * 
 * @author Emily Bache
 * @author Andrei Adamchik
 */
public class WOApplication extends WOTask {

	private final String[] stdFrameworkNames = new String[] { "JavaWebObjects", "JavaWOExtensions", "JavaEOAccess", "JavaEOControl", "JavaFoundation", "JavaJDBCAdaptor", "JavaXML" };

	protected List frameworkSets = new ArrayList();

	protected List otherClasspathSets = new ArrayList();

	protected boolean stdFrameworks = true;

	protected boolean embedStdFrameworks = false;

	private WOEnvironment woEnvironment;

	// was "gu+x"
	protected String chmod = "750";

	protected String jvmOptions = "";

	// web.xml stuff
	protected boolean webXML = false;

	protected String webXML_WOROOT = null;

	protected String webXML_LOCALROOT = null;

	protected String webXML_WOAINSTALLROOT = null;

	protected String webXML_WOAppMode = null;

	protected String webXML_WOtaglib = null;

	protected String webXML_CustomContent = null;
	
	protected String startupScriptName = null;
	
	protected String frameworksBaseURL = null;

	public void release() {
		super.release();
		frameworkSets = null;
		otherClasspathSets = null;
		woEnvironment = null;
	}

	public String getPrincipalClass() {
		String aPrincipalClass = super.getPrincipalClass();
		if (aPrincipalClass == null || aPrincipalClass.length() == 0) {
			aPrincipalClass = "Application";
		}
		return aPrincipalClass;
	}

	/**
	 * Runs WOApplication task. Main worker method that would validate all task
	 * settings and create a WOApplication.
	 */
	public void execute() throws BuildException {
		super.execute();
		validateAttributes();

		log("Installing " + name + " in " + destDir);
		createDirectories();
		if (hasClasses()) {
			jarClasses();
		}
		if (hasSources()) {
			jarSources();
		}
		if (hasLib()) {
			copyLibs();
		}
		if (hasResources()) {
			copyResources();
		}
		if (hasWs()) {
			copyWsresources();
		}
		if (hasEmbeddedFrameworks()) {
			copyEmbeddedFrameworks();
		}
		if (hasEmbeddedOtherClasspaths()) {
			copyEmbeddedOtherClasspaths();
		}

		// create all needed scripts
		AppFormat appFormat = new AppFormat(this);
		if (appFormat.processTemplates()) {
			// chmod UNIX scripts
			chmodScripts();
		}
		appFormat.release();
		frameworkSets = new ArrayList();
		otherClasspathSets = new ArrayList();
		woEnvironment = null;
		this.release();
	}

	/**
	 * Sets executable flag for all scripts. This is required on UNIX/Mac
	 * platforms. On Windows this action is simply ignored.
	 */
	protected void chmodScripts() throws BuildException {
		if (System.getProperty("os.name").toLowerCase().indexOf("win") < 0) {
			File dir = null;
			FileSet fs = null;
			Chmod aChmod = null;
			try {
				dir = taskDir();
				super.log("chmod scripts in " + dir, Project.MSG_VERBOSE);

				fs = new FileSet();
				fs.setDir(dir);
				fs.createInclude().setName("**/" + name);
				if(startupScriptName != null) {
					fs.createInclude().setName("**/" + startupScriptName);
				}
				fs.createInclude().setName("**/*.sh");

				aChmod = this.getSubtaskFactory().getChmod();
				aChmod.setPerm(this.getChmod());
				aChmod.addFileset(fs);
				aChmod.execute();
			} finally {
				dir = null;
				fs = null;
				aChmod = null;
			}
		} else {
			super.log("'" + System.getProperty("os.name") + "' is some kind of windows, skipping chmod.");
		}
	}

	/**
	 * Method copyEmbeddedFrameworks.
	 * 
	 * @throws BuildException
	 */
	protected void copyEmbeddedFrameworks() throws BuildException {
		Copy cp = new Copy();
		cp.setOwningTarget(getOwningTarget());
		cp.setProject(getProject());
		cp.setTaskName(getTaskName());
		cp.setLocation(getLocation());

		cp.setTodir(embeddedFrameworksDir());

		// The purpose of this is to create filesets that actually
		// allow the framework directory to be copied into the
		// WOApplication directory. If we didn't do this, we'd
		// have to append '/' or '/**' to the end of the includes
		// in the <frameworks> tag.
		List theFrameworkSets = getFrameworkSets();
		int size = theFrameworkSets.size();
		boolean hasSet = false;
		for (int i = 0; i < size; i++) {
			FrameworkSet fs = (FrameworkSet) theFrameworkSets.get(i);

			if (fs.getEmbed() == false) {
				continue;
			}

			File root = fs.getDir(getProject());
			DirectoryScanner ds = fs.getDirectoryScanner(getProject());
			String[] dirs = ds.getIncludedDirectories();

			for (int j = 0; j < dirs.length; j++) {
				String includeName = dirs[j];

				if (includeName.endsWith(".framework") == false) {
					throw new BuildException("'name' attribute must end with '.framework'");
				}

				FileSet newFs = new FileSet();
				PatternSet.NameEntry include;

				newFs.setDir(root);
				include = newFs.createInclude();
				include.setName(includeName + "/Resources/");
				include = newFs.createInclude();
				include.setName(includeName + "/WebServerResources/");

				cp.addFileset(newFs);
				hasSet = true;
			}
		}
		if (hasSet)
			cp.execute();
	}

	/**
	 * Method copyEmbeddedFrameworks.
	 * 
	 * @throws BuildException
	 */
	protected void copyEmbeddedOtherClasspaths() throws BuildException {
		Copy cp = new Copy();
		cp.setOwningTarget(getOwningTarget());
		cp.setProject(getProject());
		cp.setTaskName(getTaskName());
		cp.setLocation(getLocation());

		cp.setTodir(contentsDir());

		List theOtherClasspathSets = getOtherClasspath();
		int size = theOtherClasspathSets.size();
		boolean hasSet = false;
		for (int i = 0; i < size; i++) {
			OtherClasspathSet cs = (OtherClasspathSet) theOtherClasspathSets.get(i);

			if (cs.getEmbed() == false) {
				continue;
			}

			File root = cs.getDir(getProject());
			DirectoryScanner ds = cs.getDirectoryScanner(getProject());
			String[] dirs = ds.getIncludedDirectories();

			for (int j = 0; j < dirs.length; j++) {
				String includeName = dirs[j];

				FileSet newCs = new FileSet();
				PatternSet.NameEntry include;

				newCs.setDir(root);
				include = newCs.createInclude();

				// Since we're embedding, we expect to copy the entire subtree
				// for this included entry. Force it if the task user didn't.
				if (includeName.endsWith("/") == false) {
					includeName = includeName + "/";
				}
				include.setName(includeName);

				cp.addFileset(newCs);
				hasSet = true;
			}
		}
		if (hasSet)
			cp.execute();
	}

	/**
	 * Returns a list of standard frameworks as a FrameworkSet.
	 */
	public FrameworkSet standardSet() {
		FrameworkSet set = new FrameworkSet();
		set.setProject(this.getProject());
		set.setDir(new File(this.getWOEnvironment().getWOVariables().systemRoot() + "/Library/Frameworks"));

		for (int i = 0; i < stdFrameworkNames.length; i++) {
			String path =

			stdFrameworkNames[i] + ".framework";
			PatternSet.NameEntry include = set.createInclude();
			include.setName(path);
		}

		// Force embedding of the standard frameworks.
		set.setEmbed(embedStdFrameworks);
		return set;
	}

	/**
	 * Sets a flag indicating that standard frameworks, namely JavaWebObjects,
	 * JavaWOExtensions, JavaEOAccess, JavaEOControl, JavaFoundation,
	 * JavaJDBCAdaptor should be automatically referenced in deployed
	 * application.
	 */
	public void setStdFrameworks(boolean flag) {
		stdFrameworks = flag;
	}

	public void setEmbedStdFrameworks(boolean flag) {
		embedStdFrameworks = flag;
		// If we request embedding for the standard
		// frameworks, we certainly want to reference
		// them.
		if (flag) {
			stdFrameworks = true;
		}
	}

	/**
	 * Returns location where WOApplication is being built up. For WebObjects
	 * applications this is a <code>.woa</code> directory.
	 */
	protected File taskDir() {
		return getProject().resolveFile(destDir + File.separator + name + ".woa");
	}

	protected File contentsDir() {
		return new File(taskDir(), "Contents");
	}

	protected File embeddedFrameworksDir() {
		return new File(contentsDir(), "Frameworks");
	}

	protected File resourcesDir() {
		return new File(contentsDir(), "Resources");
	}

	protected File wsresourcesDir() {
		return new File(contentsDir(), "WebServerResources");
	}

	protected File wsresourcesDestDir() {
		File woLocation = new File(webServerDir(), "WebObjects");
		File appLocation = new File(woLocation, name + ".woa");
		File contentLocation = new File(appLocation, "Contents");
		return new File(contentLocation, "WebServerResources");
	}

	/**
	 * @return
	 */
	public String getWebXML_LOCALROOT() {
		return webXML_LOCALROOT;
	}

	/**
	 * @return
	 */
	public String getWebXML_WOAINSTALLROOT() {
		return webXML_WOAINSTALLROOT;
	}

	/**
	 * @return
	 */
	public String getWebXML_WOAppMode() {
		return webXML_WOAppMode;
	}

	/**
	 * @return
	 */
	public String getWebXML_WOROOT() {
		log(" WOApplication.getWebXML_WOROOT() webXML_WOROOT: " + webXML_WOROOT, Project.MSG_VERBOSE);
		return webXML_WOROOT;
	}

	/**
	 * @return
	 */
	public String getWebXML_WOtaglib() {
		return webXML_WOtaglib;
	}

	/**
	 * @return
	 */
	public boolean getWebXML() {
		return webXML;
	}

	/**
	 * @param string
	 */
	public void setWebXML_LOCALROOT(String string) {
		webXML_LOCALROOT = string;
	}

	/**
	 * @param string
	 */
	public void setWebXML_WOAINSTALLROOT(String string) {
		webXML_WOAINSTALLROOT = string;
	}

	/**
	 * @param string
	 */
	public void setWebXML_WOAppMode(String string) {
		webXML_WOAppMode = string;
	}

	/**
	 * @param string
	 */
	public void setWebXML_WOROOT(String string) {
		webXML_WOROOT = string;
	}

	/**
	 * @param string
	 */
	public void setWebXML_WOtaglib(String string) {
		webXML_WOtaglib = string;
	}

	/**
	 * @param string
	 */
	public void setWebXML(boolean value) {
		webXML = value;
	}

	protected boolean hasLib() {
		return lib.size() > 0;
	}

	protected boolean hasEmbeddedFrameworks() {
		List theFrameworkSets = getFrameworkSets();
		int size = theFrameworkSets.size();

		for (int i = 0; i < size; i++) {
			FrameworkSet fs = (FrameworkSet) theFrameworkSets.get(i);

			if (fs.getEmbed()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Create a nested FrameworkSet.
	 */
	public FrameworkSet createFrameworks() {
		FrameworkSet frameSet = new FrameworkSet();
		frameworkSets.add(frameSet);
		return frameSet;
	}

	public List getFrameworkSets() {
		if (stdFrameworks) {
			ArrayList fullList = new ArrayList(frameworkSets.size() + 1);
			fullList.add(standardSet());
			fullList.addAll(frameworkSets);
			return fullList;
		}
		return frameworkSets;
	}

	/**
	 * Return true if any of the otherclasspath elements have the 'embed'
	 * attribute set to true.
	 * 
	 * @return true if an otherclasspath element is embedded
	 */
	protected boolean hasEmbeddedOtherClasspaths() {
		List theClasspathSets = getOtherClasspath();
		int size = theClasspathSets.size();

		for (int i = 0; i < size; i++) {
			OtherClasspathSet cs = (OtherClasspathSet) theClasspathSets.get(i);

			if (cs.getEmbed()) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Create a nested OtherClasspath.
	 */
	public OtherClasspathSet createOtherclasspath() {
		OtherClasspathSet otherClasspathSet = new OtherClasspathSet();
		otherClasspathSets.add(otherClasspathSet);
		return otherClasspathSet;
	}

	/**
	 * @return List
	 */
	public List getOtherClasspath() {
		return otherClasspathSets;
	}

	/**
	 * @return WOEnvironment
	 */
	public WOEnvironment getWOEnvironment() {
		if (woEnvironment != null)
			return woEnvironment;
		woEnvironment = new WOEnvironment(this.getProject().getProperties());
		if (!woEnvironment.variablesConfigured())
			this.getProject().fireBuildFinished(new WOBuildPropertiesNotFoundException());
		return woEnvironment;
	}

	/**
	 * @return
	 */
	public String getChmod() {
		return chmod;
	}

	/**
	 * @param string
	 */
	public void setChmod(String string) {
		chmod = string;
	}

	/**
	 * Method setJvmOptions.
	 * 
	 * @param jvmOptions
	 */
	public void setJvmOptions(String jvmOptions) {
		if (jvmOptions == null) {
			this.jvmOptions = "";
		}
		this.jvmOptions = jvmOptions;
	}

	/**
	 * Method getJvmOptions.
	 * 
	 * @return String
	 */
	public String getJvmOptions() {
		return jvmOptions;
	}

	/**
	 * Ensure we have a consistent and legal set of attributes, and set any
	 * internal flags necessary based on different combinations of attributes.
	 * 
	 * @throws BuildException
	 *             if task attributes are inconsistent or missing.
	 */
	protected void validateAttributes() throws BuildException {
		log(" this.getName().validateAttributes(): " + this.getName() + " webXML: " + webXML + " webXML_WOROOT: " + webXML_WOROOT, Project.MSG_VERBOSE);
		if (webXML) {
			if (webXML_WOROOT == null) {
				webXML_WOROOT = this.getWOEnvironment().getWOVariables().systemRoot();
				if (webXML_WOROOT == null)
					throw new BuildException("'webXML_WOROOT' attribute is missing.");
			}
			if (webXML_LOCALROOT == null) {
				webXML_LOCALROOT = this.getWOEnvironment().getWOVariables().localRoot();
				if (webXML_LOCALROOT == null)
					throw new BuildException("'webXML_LOCALROOT' attribute is missing.");
			}
			if (webXML_WOAINSTALLROOT == null) {
				webXML_WOAINSTALLROOT = this.getWOEnvironment().getWOVariables().localLibraryDir() + "/WebObjects/Applications";
				if (webXML_WOAINSTALLROOT == null)
					throw new BuildException("'webXML_WOAINSTALLROOT' attribute is missing.");
			}
			if (webXML_WOAppMode == null) {
				webXML_WOAppMode = "Development";
				if (webXML_WOAppMode == null)
					throw new BuildException("'webXML_WOAppMode' attribute is missing.");
			}
			if (webXML_WOtaglib == null) {
				webXML_WOtaglib = "/WEB-INF/tlds/WOtaglib_1_0.tld";
				if (webXML_WOtaglib == null)
					throw new BuildException("'webXML_WOtaglib' attribute is missing.");
			}
		}
	}

	public String getWebXML_CustomContent() {
		return webXML_CustomContent;
	}

	public void setWebXML_CustomContent(String webXML_CustomContent) {
		this.webXML_CustomContent = webXML_CustomContent;
	}

	public String getStartupScriptName() {
		return startupScriptName;
	}

	public void setStartupScriptName(String startupScriptName) {
		this.startupScriptName = startupScriptName;
	}

	public String getFrameworksBaseURL() {
		return frameworksBaseURL;
	}

	public void setFrameworksBaseURL(String frameworksBaseURL) {
		this.frameworksBaseURL = frameworksBaseURL;
	}
}