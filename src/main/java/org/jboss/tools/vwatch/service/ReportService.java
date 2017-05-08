package org.jboss.tools.vwatch.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.report.BundleVersionReport;
import org.jboss.tools.vwatch.report.ProductReport;
import org.jboss.tools.vwatch.report.Report;

/**
 * Service providing final report generating from given installations
 * 
 * @author jpeterka
 * 
 */
public class ReportService {

	private static ReportService instance = null;
	private String[] htmlArtifacts = { "bumped.png", "same.png", "decreased.png", "vwstyle.css" };

	public static ReportService getInstance() {
		if (instance == null) {
			instance = new ReportService();
		}
		return instance;
	}

	List<Report> reports;

	private ReportService() {
		reports = new ArrayList<Report>();
	}

	Logger log = Logger.getLogger(ReportService.class);

	/**
	 * Generates report
	 * 
	 * @param installations
	 *            given list of installations
	 * @param includeIUs
	 *            list of IUs to include the in report
	 * @param excludeIUs
	 *            list of IUs to exclude from the report
	 */
	public void generateReport(List<Installation> installations) {

		// add reports
		reports.add(new BundleVersionReport(installations));
		reports.add(new ProductReport(findInstallation(installations, Settings.getProduct())));

		for (Report r : reports) {
			r.generateReport();
		}
	}

	private Installation findInstallation(List<Installation> installations, String product) {
		for (Installation i : installations) {
			if (i.getRootFolderName().equals(product)) {
				return i;
			}
		}
		return installations.size() > 0 ? installations.get(installations.size() - 1) : null;
	}

	public String getHTMLArtifacts(int num) {
		return htmlArtifacts[num];
	}
}
