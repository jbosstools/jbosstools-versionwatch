package org.jboss.tools.vwatch;

import org.apache.log4j.Level;
/**
 * Version Watch Settings class
 * Currently supported 
 * @author Jiri Peterka, Nick Boldt
 *
 */
public class Settings {
	
	// Properties that can be used with Version Watch (-Dproperty)
	public static final String installationsDirProperty = "vwatch.installationsDir";
	public static final String productVMProperty = "vwatch.product";
	public static final String md5checkVMProperty = "vwatch.md5check"; 
	public static final String includeVersionsProperty = "vwatch.includeVersions";
	public static final String excludeVersionsProperty = "vwatch.excludeVersions";
	public static final String includeIUsProperty = "vwatch.includeIUs";
	public static final String excludeIUsProperty = "vwatch.excludeIUs";
	public static final String filenameSuffixProperty = "vwatch.filenameSuffix";
	public static final String loglevelProperty = "vwatch.loglevel";
	
	// Default values
	private static String installationsDir = "/tmp/vw";
	private static String product = "";
	private static boolean md5checkEnabled = false;	
	private static String includeVersions = "\\d+\\.\\d+\\.\\d+";
	private static String excludeVersions = "";
	private static String includeIUs = ".*";
	private static String excludeIUs = "";
	private static String filenameSuffix = ".html";
	private static Level loglevel = Level.ERROR;	// -Dvwatch.loglevel=3
		
	public static String getInstallationsDir() {
		return installationsDir;
	}
	public static void setInstallationsDir(String installationsDir) {
		Settings.installationsDir = installationsDir;
	}

	public static void setProduct(String product) {
		Settings.product = product;
	}
	public static String getProduct() {
		return product;
	}

	public static boolean isMd5checkEnabled() {
		return md5checkEnabled;
	}
	public static void setMd5checkEnabled(boolean md5checkEnabled) {
		Settings.md5checkEnabled = md5checkEnabled;
	}

	public static String getIncludeVersions() {
		return includeVersions;
	}
	public static void setIncludeVersions(String includeVersions) {
		Settings.includeVersions = includeVersions;
	}
	public static String getExcludeVersions() {
		return excludeVersions;
	}
	public static void setExcludeVersions(String excludeVersions) {
		Settings.excludeVersions = excludeVersions;
	}

	public static String getIncludeIUs() {
		return includeIUs;
	}
	public static void setIncludeIUs(String includeIUs) {
		Settings.includeIUs = includeIUs;
	}
	public static String getExcludeIUs() {
		return excludeIUs;
	}
	public static void setExcludeIUs(String excludeIUs) {
		Settings.excludeIUs = excludeIUs;
	}

	public static String getFilenameSuffix() {
		return filenameSuffix;
	}
	public static void setFilenameSuffix(String filenameSuffix) {
		Settings.filenameSuffix = filenameSuffix;
	}
	
	public static Level getLogLevel() {
		return Settings.loglevel;
	}
	public static void setLogLevel(Level level) {
		Settings.loglevel = level;
	}
	
}