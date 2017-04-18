package org.jboss.tools.vwatch.issue;

import java.util.List;

import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.validator.MD5Validator;

public class MD5Issue extends Issue {
	
	private List<String> details;

	public MD5Issue(Bundle referenceBundle, List<String> details) {
		this.details = details;
		this.sticky = false;
		this.description = "MD5 doesn't match for the same build";
		this.severity = Severity.ERROR;
		this.referenceBundle = referenceBundle;
		this.validation = new MD5Validator();
	}

	public MD5Issue() {
		
	}

	@Override
	public String getDescription() {
		String ret = description;
		int counter = 0;
		for (String d : details) {
			ret += "\n " + d;
			counter++;
			if (counter > 10) {
				ret += "\n " + "...";
				break;
			}
		}
		return ret;
	}
}
