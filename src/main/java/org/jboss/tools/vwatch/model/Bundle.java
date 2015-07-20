package org.jboss.tools.vwatch.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Bundle class, used for storing bundle related data
 * 
 * @author jpeterka
 * 
 */
public class Bundle {
	
	String name;	
	List<BundleInstance> instances = new ArrayList<BundleInstance>();
	Installation installation;
	boolean bumped = false;

    public boolean isDecreased() {
        return decreased;
    }

    public void setDecreased() {
        this.decreased = true;
    }

    private boolean decreased = false;

	public Installation getInstallation() {
		return installation;
	}

	public void setInstallation(Installation i) {
		installation = i;
	}

	/**
	 * Returns bundle issues
	 * @return
	 */
	public List<Issue> getIssues() {
		return instances.get(0).getIssues();
	}
	
	/**
	 * Returns html format of bundle versions, with full qualifiers (x.y.z.qqq)
	 * @return
	 */
	public String getFullVersions() {
		String versions = "";
		for (int i = 0; i < getInstances().size(); i++) {		
			versions += getInstances().get(i).getFullVersion();
			if (i < getInstances().size() - 1) {
				versions +="<br/>";
			}
		}
		return versions;
	}
	
	/**
	 * Returns html format of bundle versions, without qualifiers (x.y.z only)
	 * @return
	 */
	public String getVersions() {
		String versions = "";
		for (int i = 0; i < getInstances().size(); i++) {		
			versions += getInstances().get(i).getVersion();
			if (i < getInstances().size() - 1) {
				versions +="<br/>";
			}
		}
		return versions;
	}
	
	/**
	 * Returns bundle name
	 * @return
	 */
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<BundleInstance> getInstances() {
		return instances;
	}

	public void setInstances(List<BundleInstance> instances) {
		this.instances = instances;
	}
	
	public boolean hasMultipleInstances() {
		return instances.size() > 1;
	}

	public Version getVersion() {
		return instances.get(0).getVersion();
	}
	
	
	public String getMd5() {
		return (instances.get(0).getMd5());
	}

	public String getFullName() {
		return (instances.get(0).getFullName());
	}

	public void setMd5(String md5) {
		instances.get(0).setMd5(md5);
	}
	public int getMaxSeverity() {
		return (instances.get(0).getMaxSeverity());
	}
	public String getErrorsAndWarnings() {
		return (instances.get(0).getErrorsAndWarnings());
	}	
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null) return false;
		if (obj instanceof Bundle) {
			Bundle bundle = (Bundle)obj;
			if (bundle.getName().equals(this.getName())) {
				return true;
			}
		}
		return false;
	}


	public BundleInstance getInstance(BundleInstance bi) {
		for (BundleInstance i : instances) {
			if (i.getFullName().equals(bi.getFullName())) {
				return i;
			}
		}
		return null;
	}

	public void setBumped() {
		bumped = true;
	}

	public boolean getBumped() {
		return bumped;
	}


}
