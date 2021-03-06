package org.jboss.tools.vwatch.report;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.VWatch;
import org.jboss.tools.vwatch.issue.MultipleVersionIssue;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.service.BundleService;
import org.jboss.tools.vwatch.service.ReportService;
import org.jboss.tools.vwatch.service.StopWatch;
import org.jboss.tools.vwatch.validator.PairValidator;

/**
 * Service providing final report generating from given installations
 * 
 * @author jpeterka, nboldt
 * 
 */
public class BundleVersionReport extends Report {

	Logger log = Logger.getLogger(BundleVersionReport.class);
	List<Installation> installations;

	/**
	 * Generates report
	 * 
	 * @param installations
	 *            given list of installations
	 * @param includeIUs
	 *            list of IUs to include in report
	 * @param excludeIUs
	 *            list of IUs to exclude from report
	 */

	public BundleVersionReport(List<Installation> installations) {
		this.installations = installations;
	}

	@Override
	protected String getFileName(String includeIUs, String filenameSuffix) {
		return (includeIUs.equals(".*") ? "report_detailed_all" : "report_detailed_filtered") + filenameSuffix;
	}

	@Override
	protected void generateHeader() {
		add("<html><head>"
			+ "<title>Version Watch - Detailed Report</title>"
			+ "<link href=\"" + ReportService.getInstance().getHTMLArtifacts(3) + "\" rel=\"stylesheet\" type=\"text/css\"/>"
			+ "</head>");
		add("<body>");
	}

	@Override
	protected void generateBody() {

		add("<h2>Version Watch - Detailed Report: " + installations.get(installations.size() - 1).getRootFolderName()
				+ "</h2>");
		String includeIUs = Settings.getIncludeIUs();
		String excludeIUs = Settings.getExcludeIUs();
		String filenameSuffix = Settings.getFilenameSuffix();
		File file = new File("target/" + getFileName(includeIUs, filenameSuffix));

		log.setLevel(Settings.getLogLevel());

		try {
			sb.append("<h2>Feature list"
					+ (!PairValidator.isNullFilter(includeIUs) ? "<br/>&nbsp;includeIUs = /" + includeIUs + "/" : "")
					+ (!PairValidator.isNullFilter(excludeIUs)
							? (!PairValidator.isNullFilter(includeIUs) ? " and " : "") + "<br/>&nbsp;excludeIUs = /"
									+ excludeIUs + "/"
							: "")
					+ "</h2>");
			generateTable(sb, installations, true, includeIUs, excludeIUs);
			sb.append("<br/><h2>Plugin list"
					+ (!PairValidator.isNullFilter(includeIUs) ? "<br/>&nbsp;includeIUs = /" + includeIUs + "/" : "")
					+ (!PairValidator.isNullFilter(excludeIUs)
							? (!PairValidator.isNullFilter(includeIUs) ? " and " : "") + "<br/>&nbsp;excludeIUs = /"
									+ excludeIUs + "/"
							: "")
					+ "</h2>");
			generateTable(sb, installations, false, includeIUs, excludeIUs);

			long elapsed = StopWatch.stop();

			sb.append("<p>Generated by VersionWatch " + VWatch.VERSION + " in "
					+ String.format("%d min, %d sec", TimeUnit.MILLISECONDS.toMinutes(elapsed),
							TimeUnit.MILLISECONDS.toSeconds(elapsed)
									- TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(elapsed)))
					+ " at " + (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")).format(new Date()) + ".</p>");
			sb.append("<p><div class=\"footspace\"></div></body></html>");

			printErrorLogFooter();

		} catch (Exception e) {
			log.error("IO error" + e.getMessage());
			e.printStackTrace();
			return;
		}

		// log.warn("Report generated to file:///" + file.getAbsolutePath());
		add("<h2>devstudio version watch - detailed report: " + installations.get(0).getRootFolderName() + "</h2>");
	}

	private void generateTable(StringBuilder bw, List<Installation> installations, boolean feature, String includeIUs,
			String excludeIUs) throws IOException {

		log.setLevel(Settings.getLogLevel());

		BundleService bs = new BundleService();

		SortedSet<Bundle> iuSetAll = new TreeSet<Bundle>(new ErroneousIUComparator());
		List<String> iuNames = new ArrayList<String>();

		for (Installation i : installations) {
			for (Bundle b : i.getIUs(feature)) {
				if ((PairValidator.isNullFilter(includeIUs) || b.getName().matches(includeIUs))
						&& (PairValidator.isNullFilter(excludeIUs) || !b.getName().matches(excludeIUs))) {
					// unique list of bundle names
					if (!iuNames.contains(b.getName())) {
						iuNames.add(b.getName());
					}
					// all bundles
					iuSetAll.add(b);
				}
			}
		}
		//log.debug("iuSetAll size: " + iuSetAll.size());
		//log.debug("iuNames size: " + iuNames.size());
		// now dedupe
		SortedSet<Bundle> iuSet = new TreeSet<Bundle>(new ErroneousIUComparator());
		for (Bundle b : iuSetAll) {
			if (iuNames.contains(b.getName())) { 
				iuSet.add(b);
				iuNames.remove(b.getName());
			}
		}
		//log.debug("iuSet size: " + iuSet.size());
 		
		sb.append("<table width=\"100%\" max-width=\"1024px\">");

		// first row

		String iuType = "Plugin";
		String bundleTitle = "";
		if (feature) {
			iuType = "Feature";
		}
		if (!PairValidator.isNullFilter(includeIUs)) {
			bundleTitle += " includeIUs = /" + includeIUs + "/";
		}
		if (!PairValidator.isNullFilter(excludeIUs)) {
			bundleTitle += (!PairValidator.isNullFilter(includeIUs) ? ", " : "") + " excludeIUs = /" + excludeIUs + "/";
		}
		printErrorLogHeader(iuType);

		StringBuffer headerRow = new StringBuffer(
				"<tr class=\"header\"><td title=\"" + bundleTitle + "\"><b>" + iuType + "</b></td>");
		for (Installation i : installations) {
			headerRow.append("<td><b>" + i.getRootFolderName() + "</b></td>");
		}
		headerRow.append("</tr>\n");

		// next rows
		int rowCount = 0;
		int showHeaderEveryXRows = 40;
		for (Bundle b : iuSet) {
			String s = b.getName();
			if (rowCount % showHeaderEveryXRows == 0) {
				bw.append(headerRow);
			}
			rowCount++;
			bw.append(
					"<tr><td id=\"" + iuType + "_" + s + "\"><a href=\"#" + iuType + "_" + s + "\">" + s + "</a></td>");

			for (Installation i : installations) {
				Bundle iuFromList = bs.getBundleFromList(i.getIUs(feature), s);

				//log.debug("Plugin ID: " + s);

				String tooltip = " title=\"" + i.getRootFolderName() + "&#10;";
				if (iuFromList != null) {
					tooltip += iuFromList.getFullName() + "&#10;";
					String c = "";

					int max = iuFromList.getMaxSeverity();
					if (max == 0)
						c = " class=\"ok\" ";
					else if (max == 1)
						c = " class=\"info\" ";
					else if (max == 3)
						c = " class=\"warning\" ";
					else if (max == 4)
						c = " class=\"error\" ";
					else if (max == 2)
						c = " class=\"ignored\" ";

					if (iuFromList.getIssues().size() > 0) {
						tooltip += iuFromList.getErrorsAndWarnings();
						tooltip += "\" ";
					} else {
						tooltip += "Nothing suspicious";
						tooltip += "\" ";
						c = " class=\"normal\" ";
					}

					printErrorLogInformation(i, iuFromList);

					bw.append("<td " + c + " " + tooltip + ">" + getIcons(iuFromList)
							+ iuFromList.getVersions().toString() + "</td>");

					// JBIDE-21391 print the tooltip in the HTML too so the error is more easy to see + copy to a JIRA
					if (tooltip.contains("ERROR")) {
						bw.append("<td nowrap>"+tooltip.replaceAll("title=","").replaceAll("\"", "").replaceAll("&#10;", "<br/>")+"</td>");
					}
				} else {
					bw.append("<td " + "title=\"IU not available in this version\" class=\"none\">N/A</td>");
				}
			}

			bw.append("</tr>\n");
		}

		bw.append("</table>");

	}

	private String getIcons(Bundle b) {
		String ret = "";
		if (b.getBumped()) {
			String relPath = ReportService.getInstance().getHTMLArtifacts(0);
			ret = "<img src=\"" + relPath + "\"/>";
		} else if (b.isDecreased()) {
			String relPath = ReportService.getInstance().getHTMLArtifacts(2);
			ret = "<img src=\"" + relPath + "\"/>";
		} else {
			boolean check = true;
			for (Issue i : b.getIssues()) {
				if (i instanceof MultipleVersionIssue) {
					check = false;
				}
			}
			if (check) {
				String relPath = ReportService.getInstance().getHTMLArtifacts(1);
				ret = "<img src=\"" + relPath + "\"/>";
			}
		}
		return ret;
	}

	private void printErrorLogHeader(String text) {
		log.warn(
				"----------------------------------------------------------------------------------------------------");
		log.warn("Errors found in " + text + ": ");
	}

	private void printErrorLogInformation(Installation i, Bundle bundle) {
		for (Issue issue : bundle.getIssues()) {
			if (issue.getSeverity() == Severity.ERROR)
				log.error(bundle.getName() + "," + bundle.getVersion() + " from " + i.getRootFolderName() + " "
						+ issue.getDescription());
		}
	}

	private void printErrorLogFooter() {
		log.warn(
				"----------------------------------------------------------------------------------------------------");
		log.warn("");
	}

	public List<Installation> getInstallations() {
		return installations;
	}

	// first, check if the bundle has issues; then sort alphabetically
	class ErroneousIUComparator implements Comparator<Bundle> {

		@Override
		public int compare(Bundle b1, Bundle b2) {
			// a modifier to sort MultipleVersionIssue below VersionDecreasedIssue
			int mod = 0;
			//log.debug("[" + b1.getName() + "] , " + b1.getIssues().size() + (b1.getIssues().size() > 0 ? ": " + b1.getIssues().get(0).getClass().toString() : ""));
			//log.debug("{" + b2.getName() + "} , " + b2.getIssues().size() + (b2.getIssues().size() > 0 ? ": " + b2.getIssues().get(0).getClass().toString() : ""));
			// sort MultipleVersionIssue items below others, such as VersionDecreasedIssue
			if (b1.getIssues().size() > 0 || b2.getIssues().size() > 0) {
				for (Issue i : b1.getIssues()) {
					// float errors VersionDecreasedIssue and MultipleVersionIssue:ERROR to the top of the html output
					if (i.getClass().toString().contains("VersionDecreasedIssue") || (i.getClass().toString().contains("MultipleVersionIssue") && i.getSeverity() == Severity.ERROR))
					{
						//log.warn(b1.getName() + ":: " + i.getSeverity());
						mod -=1;
					}
					// float the MultipleVersionIssue:IGNORE to their regular alpha order
					if (i.getClass().toString().contains("MultipleVersionIssue") && i.getSeverity() == Severity.IGNORE)
					{
						//log.warn(b1.getName() + ":: " + i.getSeverity());
						mod +=1; 
					}
				}
				mod += b2.getIssues().size() - b1.getIssues().size();
				return mod != 0 ? mod : b1.getName().compareTo(b2.getName());
			} else {
				return b1.getName().compareTo(b2.getName());
			}
		}

	}
}
