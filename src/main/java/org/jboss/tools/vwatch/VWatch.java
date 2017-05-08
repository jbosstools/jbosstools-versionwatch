package org.jboss.tools.vwatch;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.apache.log4j.PatternLayout;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.service.EvaluationService;
import org.jboss.tools.vwatch.service.FolderLookupService;
import org.jboss.tools.vwatch.service.ReportService;
import org.jboss.tools.vwatch.service.StopWatch;

/**
 * VWatch Class root class for Version Watch usage
 * 
 * @author jpeterka
 * 
 */
public class VWatch {

	public static final String VERSION = "0.4.200";
	private int loglevelDefault = 4; // 7=DEBUG, 6=INFO, 4=WARN (default), 3=ERROR
	private boolean md5checkEnabled = false;
	Logger log = Logger.getLogger(VWatch.class);
	List<Installation> installations = new ArrayList<Installation>();

	/**
	 * Main method
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		run();
	}

	/**
	 * Main function execution complete cycle of operations from configuration,
	 * loading data to creating reports
	 */
	public static void run() {
		StopWatch.start();
		VWatch vw = new VWatch();
		vw.configureLog4j();
		vw.configureVWatch();
		vw.loadInstallations();
		vw.evaluateInstallations();
		vw.createReport();
	}

	/**
	 * Read configuration from outside: Supported parameters: -
	 * vwatch.installationsDir : root with eclipse installations -
	 * vwatch.includeIUs / excludeIUs : restrict list of bundles to only those
	 * matching include/exclude filters ; see also README.md for more
	 */
	void configureVWatch() {
		String logLevelStr = System.getProperty(Settings.loglevelProperty);
		int loglevel;
		if (logLevelStr != null && !logLevelStr.equals("")) {
			loglevel = Integer.parseInt(logLevelStr);
		} else {
			loglevel = loglevelDefault;
		}

		switch (loglevel) {
		case 7:
			Settings.setLogLevel(Level.DEBUG);
			break;
		case 6:
			Settings.setLogLevel(Level.INFO);
			break;
		case 4:
			Settings.setLogLevel(Level.WARN);
			break;
		case 3:
			Settings.setLogLevel(Level.ERROR);
			break;
		case 0:
			Settings.setLogLevel(Level.FATAL);
			break;
		default:
			Settings.setLogLevel(Level.ALL);
			break;
		}
		log.setLevel(Settings.getLogLevel());

		String installationsDir = System.getProperty(Settings.installationsDirProperty);
		if (installationsDir != null && !installationsDir.equals("")) {
			Settings.setInstallationsDir(installationsDir);
			log.info("Installations dir set to: " + installationsDir);
		} else {
			log.warn("installationsDir is not set!");
			log.warn("Must define on commandline with -DinstallationsDir=/path/to/installations/");
		}

		String md5check = System.getProperty(Settings.md5checkVMProperty);
		if (md5check != null) {
			Settings.setMd5checkEnabled(true);
		}
		String product = System.getProperty(Settings.productVMProperty);
		if (product != null) {
			Settings.setProduct(product);
		}

		String includeVersions = System.getProperty(Settings.includeVersionsProperty);
		if (includeVersions != null) {
			Settings.setIncludeVersions(includeVersions);
		}
		String excludeVersions = System.getProperty(Settings.excludeVersionsProperty);
		if (excludeVersions != null) {
			Settings.setExcludeVersions(excludeVersions);
		}

		String includeIUs = System.getProperty(Settings.includeIUsProperty);
		if (includeIUs != null) {
			Settings.setIncludeIUs(includeIUs);
		}
		String excludeIUs = System.getProperty(Settings.excludeIUsProperty);
		if (excludeIUs != null) {
			Settings.setExcludeIUs(excludeIUs);
		}

		String filenameSuffix = System.getProperty(Settings.filenameSuffixProperty);
		if (filenameSuffix != null) {
			Settings.setFilenameSuffix(filenameSuffix);
		}

	}

	/**
	 * Basic programmatic log4j configuration
	 */
	void configureLog4j() {
		Logger root = Logger.getRootLogger();
		root.addAppender(new ConsoleAppender(new PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)));
	}

	/**
	 * Load installations into collections
	 */
	void loadInstallations() {
		log.info("Loading installation started");
		FolderLookupService fls = new FolderLookupService();
		installations = fls.getInstallations(Settings.getInstallationsDir());
		log.info("Installations loaded");

	}

	/**
	 * Sorts and evaluates installations and finds defined bundle conflicts
	 */
	void evaluateInstallations() {
		EvaluationService es = new EvaluationService();
		// this is no longer needed because we can just sort the installations
		// with Arrays.sort, which is much faster
		// installations = es.sortInstallations(installations);
		es.prepareValidators();
		es.findConflicts(installations);
	}

	/**
	 * Create final report
	 */
	void createReport() {
		ReportService rs = ReportService.getInstance();
		rs.generateReport(installations);
	}

	public boolean isMd5checkEnabled() {
		return md5checkEnabled;
	}

}
