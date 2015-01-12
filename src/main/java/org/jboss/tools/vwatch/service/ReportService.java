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
	 * @param filter
	 *            filter definition
	 */
	public void generateReport(List<Installation> installations) {
		try {
			FileService.getInstance().ExportResource("/bumped.png");
			FileService.getInstance().ExportResource("/vwstyle.css");
		} catch (Exception e) {
			e.printStackTrace();
		}

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
		return installations.get(installations.size() - 1);
	}
}
