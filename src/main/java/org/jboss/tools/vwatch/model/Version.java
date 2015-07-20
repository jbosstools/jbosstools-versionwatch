package org.jboss.tools.vwatch.model;


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
		
		return major*1000000 + minor*1000 + build;
	}
}
