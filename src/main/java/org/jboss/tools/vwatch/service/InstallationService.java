package org.jboss.tools.vwatch.service;

import java.io.File;
import java.io.FilenameFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.issue.MultipleVersionIssue;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.BundleInstance;
import org.jboss.tools.vwatch.model.BundleType;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Version;
import org.jboss.tools.vwatch.validator.PairValidator;

/**
 * Service providing single installation methods like bundle loading 
 * @author Jiri Peterka
 *
 */
public class InstallationService {
	
	Logger log = Logger.getLogger(InstallationService.class);

	String includeIUs = Settings.getIncludeIUs();
	String excludeIUs = Settings.getExcludeIUs();
	
	/**
	 * Fill plugins and features informations
	 * @param installation
	 */
	public void fillPluginsAndFeatures(Installation installation) {
		readBundles(installation, true, includeIUs, excludeIUs);
		readBundles(installation, false, includeIUs, excludeIUs);
	}
	
	private void readBundles(Installation installation, boolean feature, String includeIUs, String excludeIUs) {
		FilenameFilter ff = new FilenameFilter() {
			
			public boolean accept(File dir, String name) {
				
				log.setLevel(Settings.getLogLevel());
				// DO NOT use includeVersions/excludeVersions since those are for installation versions, not individual IUs
				if (name.matches(".+\\d+\\.\\d+.\\d+.+")) {
					log.info(name + " accepted");
					return true;
				}
				else {
					log.info(name + " ignored");
					return false;
				}
			}
		};
		
		String bundleRoot = "";
		if (feature) bundleRoot = installation.getAbsoluteFeaturesDir() ;
		else bundleRoot = installation.getAbsolutePluginsDir();
				
		String[] list = new File(bundleRoot).list(ff);
		
		for (String record  : list ) {
			if (
					(PairValidator.isNullFilter(includeIUs) || record.matches(includeIUs)) && 
					(PairValidator.isNullFilter(excludeIUs) || !record.matches(excludeIUs))
				) {
				Bundle bundle = parseAndGetBundle(installation, feature, record);
				if (bundle != null) installation.getIUs(feature).add(bundle);
			}
		}		
	}

	/**
	 * Parses bundle record and adds Bundle into Bundle Collection
	 * @param <T>
	 * @param record
	 * @param bundles
	 */
	private <T> Bundle parseAndGetBundle(Installation installation, boolean feature, String record) {
		//Bundle b = new Bundle();
		
		// parse version from record
		Pattern pattern = Pattern.compile("\\d+\\.\\d+\\.\\d+\\.*.*");
		Matcher m = pattern.matcher(record);
		m.find();
		String versionString = m.group();
			
		// parse major, minor and build version
		String[] split = versionString.split("\\.");			
		Version v = new Version();
		v.setMajor(Integer.parseInt(split[0]));
		v.setMinor(Integer.parseInt(split[1]));
		v.setBuild(Integer.parseInt(split[2]));
		v.setQualifier(split[3].toString());

		// create bundle instance
		BundleInstance bi = new BundleInstance();
		bi.setVersion(v);
		
		String absolute = installation.getAbsoluteBundleDir(feature) + File.separator + record;
		
		// get bundle type
		int bundleType = BundleType.NONE;
		File f = new File(absolute);
		if (!f.exists())  {
			log.error("ERROR: File " + absolute + " doesn't exist!");
			throw new RuntimeException("File " + absolute + " doesn't exist");
		}

		if (f.getAbsolutePath().toLowerCase().endsWith(".zip")) {
			f.delete();
			return null;
		}
		
		// exclude from report if we match the excludeIUs filter
		
		if (f.isDirectory()) {
			bundleType = BundleType.DIR;
		}
		else if (f.getName().toLowerCase().endsWith(".jar"))
		{
			bundleType = BundleType.JAR;
		}

		// Set full name (remove jar extension if any);
		if (bundleType == BundleType.JAR) {
			String fullName = record;
			String withoutJar = fullName.substring(0, fullName.length() - 4);
			bi.setFullName(withoutJar);
		}		
		else {
			bi.setFullName(record);
		}
		
		bi.setAbsolutePath(installation.getAbsoluteBundleDir(feature) + File.separator + bi.getFullName());
		
		// parse bundle name
		Pattern p2 = Pattern.compile("^.*?(?=_\\d+\\.\\d+\\.\\d+)");
		Matcher m2 = p2.matcher(record);
		m2.find();
		String found = m2.group();
		
		BundleService bs = new BundleService();
		// always returns the FIRST instance matching the 'found' regex
		// use getBundlesFromList to return ALL matching instances
		Bundle b = bs.getBundleFromList(installation.getIUs(feature), found);
		if (b == null ) {
			b = new Bundle();
			b.setInstallation(installation);
			b.setName(found);
		} 
		else {
//			 TODO: figure out why bunchOfBundles.size() is always 1 
//			// check for duplicate bundles
//			Set<Bundle> bunchOfBundles = bs.getBundlesFromList(installation.getBundles(feature),found);
//			if (bunchOfBundles.size()>1)
//			{
//				for (Bundle duplicate_bundle : bunchOfBundles) {
//					log.error("Found dupe: " + duplicate_bundle.getName() + " :: " + duplicate_bundle.getVersion());
//				}
//			}
			// if it contains same instance
			BundleInstance instance = b.getInstance(bi);
			if (instance != null) {
//				log.error("Found bundle instance:  " + instance.getAbsolutePath());
//				log.error("Found version: " + instance.getVersion());
				// just add BundleType flag
				instance.setBundleType(instance.getBundleType() | bundleType);
				// return null, instance already exists
				return null;
			} else {
//				log.error("Found bundle:  " + b.getFullName());
//				log.error("Found version: " + b.getVersion());
				Issue e = new MultipleVersionIssue(b);
				b.getIssues().add(e);				
			}
		}
		
		bi.setBundle(b);		
				
		// parse postfix TBD
		bi.setPostfix("");

		log.setLevel(Settings.getLogLevel());
		//log.debug("For " + b.toString() + " : " + b.getName() + ", version = " + v.toString() + " ( " + v.toNumber() + " )");
		
		bi.setBundleType(bi.getBundleType() | bundleType);
		b.getInstances().add(bi);
//		Collections.sort(b.getInstances());
		return b;							
	}
}
