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
import org.jboss.tools.vwatch.service.FolderLookupSerivce;
import org.jboss.tools.vwatch.service.ReportService;
import org.jboss.tools.vwatch.service.StopWatch;

/**
 * VWatch Class root class for Version Watch usage
 * 
 * @author jpeterka
 * 
 */
public class VWatch {

	private String repoPath = "/opt/vw";
	private String filter = ".*";
	private int loglevel = 4; // 7 = DEBUG, 6 = INFO, 4 = WARN (default), 3 = ERROR
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
	 * vwatch.filter : restrict list of bundles to only those matching filter
	 */
	private void configureVWatch() {
		String logLevelStr = System.getProperty("vwatch.loglevel");
		if (logLevelStr != null) { 
			loglevel = Integer.parseInt(logLevelStr);
		}
		else {
			loglevel = 7;
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

		String installationsDir = System.getProperty("vwatch.installationsDir");
		if (installationsDir != null) {
			repoPath = installationsDir;
			log.info("Installations dir set to:" + repoPath);
		}

		String filter = System.getProperty(Settings.filterVMProperty);
		Settings.setFilter(filter);
		
		String md5check= System.getProperty(Settings.md5checkVMProperty);
		if (md5check != null) {
			Settings.setMd5checkEnabled(true);
		}
		String product = System.getProperty(Settings.productVMProperty);
		if (product != null) {
			Settings.setProduct(product);
		}

	}

	/**
	 * Basic programmatic log4j configuration
	 */
	private void configureLog4j() {
		Logger root = Logger.getRootLogger();
		root.addAppender(new ConsoleAppender(new PatternLayout(
				PatternLayout.TTCC_CONVERSION_PATTERN)));
	}

	/**
	 * Load installations into collections
	 */
	private void loadInstallations() {
		log.info("Loading installation started");
		FolderLookupSerivce fls = new FolderLookupSerivce();
		installations = fls.getInstallations(repoPath);
		log.info("Installations loaded");

	}

	/**
	 * Sorts and evaluates installations and finds defined bundle conflicts
	 */
	private void evaluateInstallations() {
		EvaluationService es = new EvaluationService();
		// this is no longer needed because we can just sort the installations with Arrays.sort, which is much faster
		//installations = es.sortInstallations(installations);
		es.prepareValidators();
		es.findConflicts(installations, filter);
	}

	/**
	 * Create final report
	 */
	private void createReport() {
		ReportService rs = ReportService.getInstance();
		rs.generateReport(installations);
	}

	public boolean isMd5checkEnabled() {
		return md5checkEnabled;
	}

}
