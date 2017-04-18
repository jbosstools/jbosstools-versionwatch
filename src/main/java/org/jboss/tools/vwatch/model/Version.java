package org.jboss.tools.vwatch.model;

import org.apache.log4j.Logger;

/**
 * Basic version class 
 * @author jpeterka, nboldt
 *
 */
public class Version {

	int major;
	int minor;
	int build;
	String qualifier;
	
	public int getMajor() {
		return major;
	}
	public void setMajor(int major) {
		this.major = major;
	}
	public int getMinor() {
		return minor;
	}
	public void setMinor(int minor) {
		this.minor = minor;
	}
	public int getBuild() {
		return build;
	}
	public void setBuild(int build) {
		this.build = build;
	}
	public String getQualifier() {
		return qualifier;
	}
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}
	public String toString() {
		return major + "." + minor + "." + build; 
	}
	public String toQualifiedString() {
		return major + "." + minor + "." + build + "." + qualifier;
	}
	public int toNumber() {
		
		return major*100000000 + minor*100000 + build;
	}

	public Version(String versionString) throws NumberFormatException
	{
		Logger log = Logger.getLogger(Version.class);
		try {
			String[] data = versionString.split("\\.");
			if (data.length >=1 && data[0] != null) setMajor(Integer.parseInt(data[0]));
			if (data.length >=2 && data[1] != null) setMinor(Integer.parseInt(data[1]));
			if (data.length >=3 && data[2] != null) setBuild(Integer.parseInt(data[2]));
			if (data.length >=4 && data[3] != null) setQualifier(data[3]);
		} catch (NumberFormatException e) {
			log.error("Cannot convert version to numbers - " + e.getMessage());
		}
	}

	public Version() {
	}
}
