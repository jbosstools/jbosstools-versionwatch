package org.jboss.tools.vwatch.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jboss.tools.vwatch.service.FileService;
import org.jboss.tools.vwatch.service.MD5Service;

public class BundleInstance {
	String absolutePath;
	String fullName;
	Bundle bundle;
	Version version;
	String fullversion;
	String postfix;
	String md5 = "";
	BundleType bundleType = new BundleType(BundleType.NONE);

	long size;
	List<Issue> issues = new ArrayList<Issue>();

	public String getFullName() {
		return fullName;
	}

	public void setFullName(String fullName) {
		this.fullName = fullName;
	}

	public List<Issue> getIssues() {
		return issues;
	}

	public void setIssues(List<Issue> issues) {
		this.issues = issues;
	}

	public Bundle getBundle() {
		return bundle;
	}

	public void setBundle(Bundle bundle) {
		this.bundle = bundle;
	}

	public String getFullVersion() {
		return fullversion;
	}

	public Version getVersion() {
		return version;
	}

	public void setVersion(Version version) {
		this.version = version;
		this.fullversion = version.toQualifiedString();
	}

	public String getPostfix() {
		return postfix;
	}

	public void setPostfix(String postfix) {
		this.postfix = postfix;
	}

	public int getBundleType() {
		return bundleType.type;
	}

	public void setBundleType(int bundleType) {
		this.bundleType.type = bundleType;
	}

	/**
	 * Returns true if Bundle Instance type is JAR file
	 */
	public boolean isJar() {
		return (getBundleType() | BundleType.JAR) == BundleType.JAR;
	}

	public String toString() {
		return bundle.getName() + "," + version.toString();
	}

	public String getErrorsAndWarnings() {
		String ret = "";
		
		for (Issue i : getIssues()) {
			ret += i.getSeverity().toString() + ":" + i.getDescription() + "&#10;";
		}
		return ret;
	}

	public int getMaxSeverity() {
		int ret = 0;
		for (Issue i : issues) {
			ret = Math.max(ret, i.getSeverity().ordinal());
		}
		return ret;
	}

	public String getMd5() {
		if (md5.equals("")) {
			if (bundleType.isJar()) {
				File f = new File(getAbsolutePath()+ ".jar");
				md5 = MD5Service.getInstance().getMD5(f);	
			}
			else if ((bundleType.isDir()))
			{				
				File f = null;

				// this needs to be changed for per file

				f = new File(getAbsolutePath()+ ".zip");
				FileService.getInstance().zipFolder(new File(getAbsolutePath()), f);				
				md5 = MD5Service.getInstance().getMD5(new File(getAbsolutePath() + ".zip"));
				f.delete();
			}
		}		
		return md5;
	}

	public void setMd5(String md5) {
		this.md5 = md5;
	}

	public String getAbsolutePath() {
		return absolutePath;
	}

	public void setAbsolutePath(String absolutePath) {
		this.absolutePath = absolutePath;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}
}
