package org.jboss.tools.vwatch.issue;

import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.validator.VersionDecreasedValidator;

/**
 * This issue describes situation when there component version is lower than
 * component version for previous version product
 * 
 * @author jpeterka, nboldt
 * 
 */
public class VersionDecreasedIssue extends Issue {
	
	public VersionDecreasedIssue() {
		
	}

	// old method which doesn't report the full problem
	public VersionDecreasedIssue(Bundle referenceBundle) {
		this.sticky = true;
		this.description = "Version must be higher than " + referenceBundle.getFullVersions();
		this.severity = Severity.ERROR;
		this.referenceBundle = referenceBundle;
		this.validation = new VersionDecreasedValidator();
	}		

	// new method which reports both reference version and this version, so the problem is more obvious
	public VersionDecreasedIssue(Bundle referenceBundle, Bundle thisBundle) {
		this.sticky = true;
		this.description = "Version " + thisBundle.getFullVersions() + " must be higher than " + referenceBundle.getFullVersions();
		this.severity = Severity.ERROR;
		this.referenceBundle = referenceBundle;
		this.validation = new VersionDecreasedValidator();
	}
}
