package org.jboss.tools.vwatch.report;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.counter.IssueCounter;
import org.jboss.tools.vwatch.issue.FolderAndJarIssue;
import org.jboss.tools.vwatch.issue.MD5Issue;
import org.jboss.tools.vwatch.issue.MultipleVersionIssue;
import org.jboss.tools.vwatch.issue.VersionDecreasedIssue;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.service.ReportService;

public class ProductReport extends Report {

	Logger log = Logger.getLogger(ProductReport.class);
	Installation installation;
	int overallIssueCount;

	public ProductReport(Installation installation) {
		this.installation = installation;
	}

	@Override
	protected String getFileName() {
		return "report_summary.html";
	}

	protected void generateHeader() {	
		String style = ReportService.getInstance().getCSSContent();
		add("<html><head><title>Version Watch - Summary Report</title><style type=\"text/css\">" + style + "</style></head>");
		add("<body>");
	}

	protected void generateBody() {
		add("<h2>Version Watch - Summary Report: " + installation.getRootFolderName() + "</h2>");
		reportMultipleVersionIssue();
		reportVersionDecreasedIssue();
		reportFolderAndJarIssue();
		reportMD5Issues();
		add("&nbsp;<p>" + (overallIssueCount < 1 ? "<b style='color:green'>No" : "<b style='color:red'>" + overallIssueCount)
			+ " issues found.</b></p>\n");
	}

	protected void generateFooter() {
		super.generateFooter();
	}

	private String reportIssues(Class<? extends Issue> class1) {
		Results r1 = searchIUsForIssues(class1, installation.getIUs(true));
		Results r2 = searchIUsForIssues(class1, installation.getIUs(false));
		int count = r1.getCount() + r2.getCount();
		String text = r1.getHtml() + r2.getHtml();
		IssueCounter.getInstance().setValue(class1,count);
		overallIssueCount += count;
		return count != 0 ? text : null; // "No issues found" + html.newLine() : text;
	}

	private Results searchIUsForIssues(Class<? extends Issue> class1, Set<Bundle> bundles) {
		int counter = 0;
		StringBuffer sb = new StringBuffer();
		List<String> ignored = new ArrayList<String>();

		sb.append("<table cellspacing=2 cellpadding=2>\n");
		for (Bundle b : bundles) {
			List<Issue> issues = b.getIssues();
			for (Issue i : issues) {
				if (i.getClass().equals(class1)) {
					String message = "<tr><td><b style='color:red'>" + i.getReferenceBundle().getName() + "</b></td><td>" +
						i.getReferenceBundle().getFullVersions().replaceAll("<br/>", "</td><td>") + "</td></tr>\n";
					if (i.getSeverity() == Severity.IGNORE) {
						ignored.add(message);
					} else { // if ignored, don't list with the non-ignored list
						sb.append(message);
						counter++;
					}
				}
			}
		}
		if (ignored.size() > 0) {
			sb.append("<tr><td colspan=4 bgcolor='#EEEEEE'><b>Ignored (known issue)</b></td></tr>\n");
			for (String s : ignored) {
				sb.append(s.replaceAll("color:red", "color:blue"));
			}
		}
		sb.append("</table>\n");
		Results r = new Results();
		r.setCount(counter);
		r.setHtml(sb.toString());
		return r;
	}

	private void reportMD5Issues() {
		String out = reportIssues(MD5Issue.class);
		add(out != null ? html.h2() + "Checksum match issues" + html.newLine() + html.h2().end() + out:"");
	}

	private void reportFolderAndJarIssue() {
		String out = reportIssues(FolderAndJarIssue.class);
		add(out != null ? html.h2() + "Folder and jar issues" + html.newLine() + html.h2().end() + out:"");
	}
	
	private void reportMultipleVersionIssue() {
		String out = reportIssues(MultipleVersionIssue.class);
		add(out != null ? html.h2() + "Multiple version issues" + html.newLine() + html.h2().end() + out:"");
	}
	
	private void reportVersionDecreasedIssue() {
		String out = reportIssues(VersionDecreasedIssue.class);
		add(out != null ? html.h2() + "Version decreased issues" + html.newLine() + html.h2().end() + out:"");
	}

	public class Results {
		private int count;
		private String html;

		public int getCount() {
			return count;
		}
		public void setCount(int count) {
			this.count = count;
		}
		public String getHtml() {
			return html;
		}
		public void setHtml(String html) {
			this.html = html;
		}
		
	}

}