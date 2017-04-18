package org.jboss.tools.vwatch.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.report.BundleVersionReport;
import org.jboss.tools.vwatch.report.ProductReport;
import org.jboss.tools.vwatch.report.Report;
import org.jboss.vwatch.util.CSSReader;

/**
 * Service providing final report generating from given installations
 * 
 * @author jpeterka
 * 
 */
public class ReportService {

	private static ReportService instance = null;
	private String cssContent;
	private String bumpIcoPath,decIcoPath,sameIcoPath;

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
	 * 			  list of IUs to include the in report
	 * @param excludeIUs
	 *            list of IUs to exclude from the report
	 */
	public void generateReport(List<Installation> installations) {
		try {
			bumpIcoPath = FileService.getInstance().ExportResource("/bumped.png");
            sameIcoPath = FileService.getInstance().ExportResource("/same.png");
            decIcoPath = FileService.getInstance().ExportResource("/decreased.png");

			String cssPath = FileService.getInstance().ExportResource("/vwstyle.css");
			cssContent = CSSReader.readCSSFile(cssPath);
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

	public String getCSSContent() {
		return cssContent;
	}

	public String getBumpIcoPath() {
		String relPath = FileService.getInstance().getRelPath(new File(System.getProperty("user.dir")+"target/").getAbsolutePath(), bumpIcoPath);
		return  relPath;
	}

    public String getDecIcoPath() {
        String relPath = FileService.getInstance().getRelPath(new File(System.getProperty("user.dir")+"target/").getAbsolutePath(), decIcoPath);
        return  relPath;
    }

    public String getSameIcoPath() {
        String relPath = FileService.getInstance().getRelPath(new File(System.getProperty("user.dir")+"target/").getAbsolutePath(), sameIcoPath);
        return  relPath;
    }



}
