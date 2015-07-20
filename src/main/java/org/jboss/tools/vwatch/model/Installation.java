package org.jboss.tools.vwatch.model;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

/**
 * Main storage for installation data Installation is synonym for eclipse
 * installation
 * 
 * @author jpeterka
 * 
 */
public class Installation {

	Version version;
	String rootFolderAbsolutePath;
	String rootFolderName;
	String eclipseFolder;
	boolean valid = true;

	Set<Bundle> plugins;
	Set<Bundle> features;

	public String getRootFolderName() {
		return rootFolderName;
	}

	public void setRootFolderName(String rootFolderName) {
		this.rootFolderName = rootFolderName;
	}

	public void setRootFolderAbsolutePath(String rootFolderAbsolutePath) {
		this.rootFolderAbsolutePath = rootFolderAbsolutePath;
	}

	public String getTmpDir() {
		File f = new File(rootFolderAbsolutePath + File.separator + eclipseFolder + File.separator + "tmp");
		if (!f.exists()) f.mkdir();
		return f.getAbsolutePath();
	}

	public Installation() {
		this.plugins = new HashSet<Bundle>();
		this.features = new HashSet<Bundle>();
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
	}

	public String getRootFolderAbsolutePath() {
		return rootFolderAbsolutePath;
	}

	public String getEclipseFolder() {
		return eclipseFolder;
	}

	public void setEclipseFolder(String eclipseFolder) {
		this.eclipseFolder = eclipseFolder;
	}

	public Set<Bundle> getPlugins() {
		return plugins;
	}

	public void setPlugins(Set<Bundle> plugins) {
		this.plugins = plugins;
	}

	public Set<Bundle> getFeatures() {
		return features;
	}

	public void setFeatures(Set<Bundle> features) {
		this.features = features;
	}

	public boolean isValid() {
		return valid;
	}

	public void setValid(boolean valid) {
		this.valid = valid;
	}

	public String getAbsoluteEclipseRoot() {
		return getRootFolderAbsolutePath() + File.separator
				+ getEclipseFolder();
	}

	public String getAbsolutePluginsDir() {
		return getAbsoluteEclipseRoot() + File.separator + "plugins";
	}

	public String getAbsoluteFeaturesDir() {
		return getAbsoluteEclipseRoot() + File.separator + "features";
	}

	public String getAbsoluteBundleDir(boolean feature) {
		if (feature)
			return getAbsoluteFeaturesDir();
		else
			return getAbsolutePluginsDir();

	}

	public Set<Bundle> getIUs(boolean feature) {
		if (feature)
			return getFeatures();
		else
			return getPlugins();
	}

	public String toString() {
		return version.toString();
	}

}
