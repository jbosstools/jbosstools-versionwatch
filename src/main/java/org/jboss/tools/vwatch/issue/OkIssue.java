package org.jboss.tools.vwatch.issue;

import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.validator.OkValidator;

public class OkIssue extends Issue {
	

	public OkIssue() {
		
	} 
	
	public OkIssue(Bundle referenceBundle) {
		this.sticky = false;
		this.description = "Issue from previous build was fixed";
		this.severity = Severity.OK;
		this.referenceBundle = referenceBundle;
		this.validation = new OkValidator();
	}
}
