package org.jboss.tools.vwatch.issue;

import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.validator.FolderAndJarValidator;

/**
 * This issue describes situation when there component version is lower than
 * component version for previous version product
 * 
 * @author jpeterka
 * 
 */
public class FolderAndJarIssue extends Issue {
	
	public FolderAndJarIssue() {
		
	}
	
	public FolderAndJarIssue(Bundle referenceBundle) {
		this.sticky = false;
		this.description = "There is folder and JAR for same plugin";
		this.severity = Severity.ERROR;
		this.referenceBundle = referenceBundle;
		this.validation = new FolderAndJarValidator();
	}		
}
