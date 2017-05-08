package org.jboss.tools.vwatch.service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.model.Version;

/**
 * Services for loading eclipse installations data from given folder
 * 
 * @author jpeterka
 *
 */
public class FolderLookupService {

	/**
	 * Load installations based in the repo path
	 * 
	 * @param installationsDir
	 * @return
	 */

	Logger log = Logger.getLogger(FolderLookupService.class);
	LogService ls = new LogService();

	public List<Installation> getInstallations(String installationsDir) {

		List<Installation> installations = new ArrayList<Installation>();
		// check repo dir
		if (!dirExists(installationsDir)) {
			try {
				// create the dir
				File theDir = new File(installationsDir);
				theDir.mkdir();
				log.warn("Installation folder " + installationsDir + " created, but is empty.");
			} catch (Exception e) {
				e.printStackTrace();
				LogService.logAndExit(installationsDir + " doesn't exist or is not directory");
			}
		}

		// load folders list
		installations = getValidInstallFolders(installationsDir);
		if (installations.size() == 0) {
			log.warn("Installation folder " + installationsDir + " is empty.");
		}
		
		// load installations content
		fillInstallationsContent(installations);
		if (installations.size() == 0)
			log.error("No installations data loaded from " + installationsDir);

		return installations;
	}

	/**
	 * Check if directory exists
	 * 
	 * @param installationsDir
	 * @return
	 */
	private boolean dirExists(String installationsDir) {
		File f = new File(installationsDir);
		return (f.exists() && f.isDirectory());
	}

	/**
	 * Returns folder list matching valid pattern
	 * 
	 * @param installationsDir
	 * @return
	 */
	private List<Installation> getValidInstallFolders(String installationsDir) {

		List<Installation> installations = new ArrayList<Installation>();

		VersionService vs = new VersionService();

		log.info("Include versions: " + Settings.getIncludeVersions());
		log.info("Exclude versions: " + Settings.getExcludeVersions());

		File root = new File(installationsDir);
		File[] listFiles = root.listFiles();
		// sort 9 < 10, not 9 > 1
		Arrays.sort(listFiles, new InstallationComparator());
		for (File f : listFiles) {
			if (f.isDirectory() && vs.isValid(f.getName())) {
				Installation i = new Installation();
				i.setRootFolderAbsolutePath(f.getAbsolutePath());
				i.setRootFolderName(f.getName());
				setInstallationVersion(i);
				installations.add(i);
				log.info("Using install dir: " + f.toString());
			}
		}
		return installations;
	}

	private void setInstallationVersion(Installation i) {
		VersionService vs = new VersionService();
		Version v = vs.parseVersionFromInstallationFolder(i);
		i.setVersion(v);
	}

	/**
	 * One by one load installation Content
	 * 
	 * @param folders
	 * @return
	 */
	private void fillInstallationsContent(List<Installation> installations) {

		for (Installation installation : installations) {
			if (!isInstallationValid(installation)) {
				installation.setValid(false);
			} else {
				InstallationService is = new InstallationService();
				is.fillPluginsAndFeatures(installation);
			}
		}

	}

	/**
	 * Checks if installation is valid, i.e. it contains eclipse/studio, plugins
	 * and feature dirs
	 * 
	 * @return
	 */
	private boolean isInstallationValid(Installation installation) {
		File f = new File(installation.getRootFolderAbsolutePath());
		String[] list = f.list();

		List<String> arrayList = new ArrayList<String>();
		Collections.addAll(arrayList, list);

		String eclipseFolder = "";
		if (arrayList.contains("eclipse"))
			eclipseFolder = "eclipse";
		if (arrayList.contains("studio"))
			eclipseFolder = "studio";

		if (eclipseFolder.equals("")) {
			log.error("Folder " + installation.getRootFolderAbsolutePath()
					+ " isn't valid eclipse/devstudio installation (doesn't contain eclipse or studio folder");
			return false;
		}
		installation.setEclipseFolder(eclipseFolder);

		File installationRootFolder = new File(
				installation.getRootFolderAbsolutePath() + File.separator + eclipseFolder);
		if (!installationRootFolder.isDirectory()) {
			log.error("Installation folder " + installationRootFolder.getAbsolutePath() + " is not directory");
			return false;
		}

		String[] installationRootList = installationRootFolder.list();
		arrayList.clear();
		Collections.addAll(arrayList, installationRootList);
		if (!arrayList.contains("plugins") && !arrayList.contains("features")) {
			log.error("Installation folder " + installationRootFolder.getAbsolutePath()
					+ " doesn't contain plugins or features dirs");
			return false;
		}

		// everything seems ok
		return true;
	}

	// ensure that 10.0.0 is considered NEWER than 9.0.0, instead of doing a
	// basic char-by-char compare, which results in 9 > 1
	class InstallationComparator implements Comparator<File> {
		@Override
		public int compare(File f1, File f2) {
			VersionService vs = new VersionService();
			return vs.parseVersionFromInstallationFolder(f1).toNumber()
					- vs.parseVersionFromInstallationFolder(f2).toNumber();
		}
	}
}
