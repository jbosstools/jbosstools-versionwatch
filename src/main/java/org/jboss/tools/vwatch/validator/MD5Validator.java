package org.jboss.tools.vwatch.validator;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.issue.MD5Issue;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.BundleInstance;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.service.MD5Service;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarEntry;

import static org.jboss.tools.vwatch.model.BundleType.JAR;

public class MD5Validator extends PairValidator {
	
	private Logger log = Logger.getLogger(MD5Validator.class);
	MD5Service md5 = MD5Service.getInstance();
	private List<String> md5issueDetails = new ArrayList<String>();


	@Override
	public boolean isValid(Bundle b1, Bundle b2) {
		log.debug("MD5 check for " + b1.getName());
		for (BundleInstance i1 : b1.getInstances()) {

			log.debug("Searching instance in second installation");
			BundleInstance i2 = b2.getInstance(i1);

			if (i2 != null) {
				if (i1.isJar() && i2.isJar()) {
					if (isSameMd5Jar(i1.getAbsolutePath() + ".jar",i2.getAbsolutePath() + ".jar")) {
						log.debug("MD5 for both jars is identical");
						return true;
					}
				}
				log.debug("Performing deeper archive analysis");
				unpackAndCompare(i1, i2);
			} else {
				log.debug(i1.getFullName() + " not found in a pair installation");
			}

		}
		boolean ret = (md5issueDetails.size() == 0);
		return ret;
	}


	@Override
	public void addIssue(Bundle b1, Bundle b2) {
		List<String> storedDetails = new ArrayList<String>(md5issueDetails);
		Issue i = new MD5Issue(b1, storedDetails);
		b2.getIssues().add(i);
		md5issueDetails.clear();
	}


	/**
	 * Unpack jar file into given destination directory
	 */
	private void unpackJarUnhandled(String destDir, File jarFile) throws IOException {
		java.util.jar.JarFile jar = new java.util.jar.JarFile(jarFile);
		Enumeration<JarEntry> enumEntries = jar.entries();
		while (enumEntries.hasMoreElements()) {
			java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumEntries.nextElement();
			java.io.File f = new java.io.File(destDir + java.io.File.separator + file.getName());
			log.debug(file.getName());
			if (!f.getParentFile().exists()) {
				log.debug("Creating subfolders " + f.getParentFile().getAbsolutePath());
				f.getParentFile().mkdirs();
			}

			if (f.isFile()) {
				log.debug("Creating file " + f.getAbsolutePath());
				java.io.InputStream is = jar.getInputStream(file); // get the input stream
				java.io.FileOutputStream fos = new java.io.FileOutputStream(f);
				while (is.available() > 0) {  // write contents of 'is' to 'fos'
					fos.write(is.read());
				}
				fos.close();
				is.close();
			}
		}
		jar.close();
	}

	/**
	 * Unpacks given Jar bundle instance
	 */
	private void unpackJar(BundleInstance i) {
		if (!i.isJar()) throw new RuntimeException("Bundle instance in not jar file");
		String d = getUnpackFolderPath(i);
		unpackJar(d,new File(i.getAbsolutePath() + ".jar"));
	}

	/**
	 * Unpacks jarfile into destDir
	 */
	private void unpackJar(String destDir, File jarFile) {
		log.debug("Unpacking jar file " + jarFile.getAbsolutePath() + " into " + destDir);
		try {
			unpackJarUnhandled(destDir, jarFile);
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot unpack JAR " + e.getMessage());
		}
		log.debug("File unpacked");
	}

	/**
	 * Compares two dirs and add found disprepances into md5issueDetails
	 */
	private void compareFilesInFolders(String d1, String d2) {
		log.debug("Comparing files in folders " + d1 + " and " + d2);
		File dir1 = new File(d1);
		File dir2 = new File(d2);

		Collection<File> list1 = FileUtils.listFiles(dir1, null, true);
		Collection<File> list2 = FileUtils.listFiles(dir2, null, true);

		HashMap<String, File> h1 = getHashMapFromCol(list1, d1);
		HashMap<String, File> h2 = getHashMapFromCol(list2, d2);
		Set<String> both = new HashSet<String>();

		findBothInHashMap(h1, h2, both);
		findBothInHashMap(h2, h1, both);

		compareMd5forBoth(h1, h2, both);
		log.debug("Comparing finished, " + md5issueDetails.size() + " diff found" );
	}


	/**
	 * Compares files and finds not matching md5 across collections. h1,h2
	 */
	private void compareMd5forBoth(HashMap<String, File> h1, HashMap<String, File> h2, Set<String> both) {
		for (String s: both ) {
			String md5one = MD5Service.getInstance().getMD5(h1.get(s));
			String md5second = MD5Service.getInstance().getMD5(h2.get(s));

			if (!md5one.equals(md5second)) {
				String message = "There is diff in " + s;
				log.info(message);
				md5issueDetails.add(message);
			}
		}

	}

	/**
	 * Get file hashmap from collection
	 */
	private HashMap<String, File> getHashMapFromCol(Collection<File> col, String root) {
		HashMap<String, File> h = new HashMap<String, File>();
		try {

			for (File f : col) {
				String absPath = f.getAbsolutePath().replace("\\","/");
				String root2 = root.replace("\\","/");
				String[] split = absPath.split(root2 + "/");
				String relPath = split[1];
				h.put(relPath, f);
			}
		} catch (Exception e) {
			log.error(e.getMessage());
			throw new RuntimeException("Problem processing collection for MD5Validator: " + e);
		}
		return h;
	}


	/**
	 * Finds same files in two hashmaps and put it into set
	 */
	private void findBothInHashMap(HashMap<String, File> h1, HashMap<String, File> h2, Set<String> both) {
		Iterator it = h1.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry pairs = (Map.Entry)it.next();
			String key = pairs.getKey().toString();
			if (h2.containsKey(key)) {
				both.add(key);
			} else {
				File diffFile = (File)pairs.getValue();
				String message = "Additional file: " + diffFile.getName();
				log.debug(message);
				md5issueDetails.add(message);
			}

			pairs.getKey();

		}
	}


	/**
	 * Takes instances and unpack jar instance and compare md5 for each files
	 * @param i1 instance1
	 * @param i2 instance2
	 */
	private void unpackAndCompare(BundleInstance i1, BundleInstance i2) {
		// unpack what is necessary
		if (i1.isJar()) unpackJar(i1);
		if (i2.isJar()) unpackJar(i2);

		// compare folders
		String d1 = getUnpackFolderPath(i1);
		String d2 = getUnpackFolderPath(i2);
		compareFilesInFolders(d1, d2);


		if (i1.isJar())	deleteFolder(d1);
		if (i2.isJar()) deleteFolder(d2);
	}

	/**
	 * Delete given folder
	 */
	private void deleteFolder(String d) {
		log.debug("Deleting folder " + d);
		try {
			FileUtils.deleteDirectory(new File(d));
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Cannot delete folder " + d + " " + e.getMessage());
		}
	}


	/**
	 * Returns true if instance is jar type
	 */
	private boolean isInstanceJar(BundleInstance i) {
		return (i.getBundleType() | JAR) == JAR;
	}

	/**
	 * Returns folder
	 */
	private String getUnpackFolderPath(BundleInstance i) {
		String ret = "";
		if (isInstanceJar(i)) {
			ret = i.getBundle().getInstallation().getTmpDir() + File.separator + i.getFullName();
		} else {
			ret	 = i.getAbsolutePath();
		}
		return ret;
	}

	/**
	 * Return true if md5 of two files matches, false otherwise
	 */
	public boolean isSameMd5Jar(String p1, String p2) {
		String md5first = md5.getMD5(new File (p1));
		String md5second= md5.getMD5(new File (p2));
		return (md5first.equals(md5second));
	}
}
