package org.jboss.tools.vwatch.issue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.model.Severity;
import org.jboss.tools.vwatch.model.Version;
import org.jboss.tools.vwatch.service.VersionService;
import org.jboss.tools.vwatch.validator.MultipleVersionValidator;

/**
 * See for ignored:
 * https://github.com/jbosstools/jbosstools-target-platforms/blob/4.31.x/jbosstools/multiple/jbosstools-multiple.target#L11 * 
 * @author jpeterka
 *
 */
public class MultipleVersionIssue extends Issue {

	Logger log = Logger.getLogger(MultipleVersionIssue.class);

	private Properties multipleVersionIgnoreProperties = new Properties();
	ClassLoader classloader = Thread.currentThread().getContextClassLoader();
	
	InputStream is = classloader.getResourceAsStream("multiple-version-ignore.properties");{
		try {
			multipleVersionIgnoreProperties.load(is);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public MultipleVersionIssue(Bundle referenceBundle) {
		// TODO figure out why getVersions() only returns ONE version
		if (multipleVersionIgnoreProperties.containsKey(referenceBundle.getName())) {
			// Check that ref bundle version is in the allowed range
			// eg., for org.objectweb.asm [4.0.0,5.0.1] != 5.2.0
			String[] lims = multipleVersionIgnoreProperties.getProperty(referenceBundle.getName()).split(",");
			Version lowerLimit = new Version(lims[0].replaceAll("[\\{\\(\\)\\}\\[\\]]+", ""));
			Version upperLimit = new Version(lims[1].replaceAll("[\\{\\(\\)\\}\\[\\]]+", ""));
			Version thisBundle = new Version(referenceBundle.getVersions());
			//log.info("Got range: " + lowerLimit + " to " + upperLimit + "; check this bundle: " + thisBundle);
			VersionService vs = new VersionService();
			//log.info("Check lower limit: " + vs.isVersionGreaterOrEqual(lowerLimit, thisBundle));
			//log.info("Check upper limit: " + vs.isVersionGreaterOrEqual(thisBundle, upperLimit));
			if (vs.isVersionGreaterOrEqual(lowerLimit, thisBundle) && vs.isVersionGreaterOrEqual(thisBundle, upperLimit))
			{
				this.description = "Multiple versions ignored from expected range " + multipleVersionIgnoreProperties.getProperty(referenceBundle.getName());
				this.severity = Severity.IGNORE;
				log.info(referenceBundle.getName() + ": " + this.description + "; " + referenceBundle.getVersions());
			} else {
				this.description = "Multiple versions expected, but outside expected range " + multipleVersionIgnoreProperties.getProperty(referenceBundle.getName());
				this.severity = Severity.ERROR;
				log.info(referenceBundle.getName() + ": " + this.description + "; " + referenceBundle.getVersions());
			}
		} else {
			this.description = "Multiple versions of same bundle found!";
			this.severity = Severity.ERROR;			
			log.info(referenceBundle.getName() + ": " + this.description);
//			log.info("Versions found: "+referenceBundle.getVersions());
		}
		this.sticky = false;
		this.referenceBundle = referenceBundle;
		this.validation = new MultipleVersionValidator();
		
	}
}
