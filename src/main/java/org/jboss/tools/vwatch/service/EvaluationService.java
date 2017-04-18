package org.jboss.tools.vwatch.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.jboss.tools.vwatch.Settings;
import org.jboss.tools.vwatch.model.Bundle;
import org.jboss.tools.vwatch.model.Installation;
import org.jboss.tools.vwatch.model.Issue;
import org.jboss.tools.vwatch.validator.BundleValidator;
import org.jboss.tools.vwatch.validator.FolderAndJarValidator;
import org.jboss.tools.vwatch.validator.MD5Validator;
import org.jboss.tools.vwatch.validator.OkValidator;
import org.jboss.tools.vwatch.validator.PairValidator;
import org.jboss.tools.vwatch.validator.Validator;
import org.jboss.tools.vwatch.validator.VersionBumped;
import org.jboss.tools.vwatch.validator.VersionDecreasedValidator;

/**
 * Evaluation Service
 * 
 * @author jpeterka
 * 
 */
public class EvaluationService {

	Logger log = Logger.getLogger(EvaluationService.class);
	List<PairValidator> pairValidators = new ArrayList<PairValidator>();
	List<BundleValidator> bundleValidators = new ArrayList<BundleValidator>();

	/**
	 * Sorts installations according to versions
	 * 
	 * @param originalList
	 *            original unsorted list
	 * @return sorted installations from min version to max version
	 */
	public List<Installation> sortInstallations(List<Installation> originalList) {

		List<Installation> sortedList = new ArrayList<Installation>();

		Installation min = originalList.get(0);
		log.setLevel(Settings.getLogLevel());
		log.info("Original list: " + originalList.toString());
		int steps = originalList.size();
		for (int i = 0; i < steps; i++) {
			min = originalList.get(0);
			for (int j = 0; j < originalList.size() - 1; j++) {
				if (min.getRootFolderName().compareTo(
						originalList.get(j + 1).getRootFolderName()) > 0) {
					min = originalList.get(j + 1);
				}
			}
			sortedList.add(min);
			originalList.remove(min);
			log.info("Sorted: " + min.getRootFolderName());
		}

		log.info("Original list: " + originalList.toString());
		log.info("Sorted list: " + sortedList.toString());

		return sortedList;
	}

	public void prepareValidators() {

		// prepare validator list		
		pairValidators.add(new VersionDecreasedValidator());
		pairValidators.add(new OkValidator());
		pairValidators.add(new VersionBumped());
		
		bundleValidators.add(new FolderAndJarValidator());
		
		if (Settings.isMd5checkEnabled()) {
			pairValidators.add(new MD5Validator());
		}
	}
	
	/**
	 * Finds conflicts in given installations
	 * 
	 * @param installations
	 *            installation list for evaluation
	 */
	public void findConflicts(List<Installation> installations) {

		log.setLevel(Settings.getLogLevel());

		// Single bundle evaluation
		for (int i = 0; i < installations.size() ; i++) {
			findBundleIssues(installations.get(i), true);	
			findBundleIssues(installations.get(i), false);	
		}
		
		// Pair issue evaluation
		for (int i = 0; i < installations.size() - 1; i++) {
			log.debug("Finding conflicts in installation: "
					+ installations.get(i + 1).getRootFolderAbsolutePath());

			// features
			findPairIssues(installations.get(i),installations.get(i + 1), true);		
			// plugins
			findPairIssues(installations.get(i),installations.get(i + 1), false);
		}
	}

	private void findBundleIssues(Installation installation, boolean feature) {
		// find new issues
		for (Bundle b : installation.getIUs(feature)) {
			runBundleValidators(b);
		}
	}

	/**
	 * Finds conflicts between two installations TODO: Separate validation rules
	 * 
	 * @param i1
	 *            first installation
	 * @param i2
	 *            second installation
	 * @param feature
	 *            boolean to set whether to use feature or plugin pack
	 */
	private void findPairIssues(final Installation i1,
			final Installation i2, boolean feature) {

		checkFixesForPreviousIssues(i1, i2, feature);
		findNewPairIssues(i1, i2, feature);		
	}

	private void checkFixesForPreviousIssues(Installation i1, Installation i2,
			boolean feature) {

		BundleService bs = new BundleService();
		for (Bundle b2 : i2.getIUs(feature)) {
			Bundle b1 = bs.getBundleFromList(i1.getIUs(feature),
					b2.getName());
			if (b1 != null) {

				// Check fixes
				if (!b1.hasMultipleInstances() && !b2.hasMultipleInstances()) {
					checkFixes(b1, b2);
				} else {
					log.info("Multiple instances, not supported yet");
				}
			} else {
				log.info("Not found " + b2.getName() + " in "
						+ i1.getRootFolderName());
			}
		}

	}
	
	private void checkFixes(Bundle b1, Bundle b2) {		
		for (Issue i : b1.getIssues()) {
			if (i.isSticky()) {
				Validator v = i.getValidation();
				if (v instanceof PairValidator) {
					PairValidator pv = (PairValidator)v;
					Bundle refBundle = i.getReferenceBundle();
					// issue not fixed
					if (!pv.isValid(refBundle, b2)) {
						pv.addIssue(refBundle, b2);
					
					} else {
						//b2.getIssues().add(new OkIssue(b2));
					}
				}
			}
		}
	}

	private void findNewPairIssues(Installation i1, Installation i2,
			boolean feature) {
		BundleService bs = new BundleService();
		log.setLevel(Settings.getLogLevel());

		for (Bundle b2 : i2.getIUs(feature)) {
			// only validate if the include/exclude filters match, which saves a ton of time
			if (
					(PairValidator.isNullFilter(Settings.getIncludeIUs())
					|| b2.getName().matches(Settings.getIncludeIUs()))
					&&
					(PairValidator.isNullFilter(Settings.getExcludeIUs())
							|| !b2.getName().matches(Settings.getExcludeIUs()))					
					) {
				Bundle b1 = bs.getBundleFromList(i1.getIUs(feature),
						b2.getName());
				if (b1 != null) {
					if (!b1.hasMultipleInstances()
							&& !b2.hasMultipleInstances()) {
						runPairValidators(b1, b2);
					} else {
						log.info("Multiple instances, not supported yet");
					}
				} else {
					log.info("Could not find " + b2.getName() + " in "
							+ i1.getRootFolderName());
				}
			}
		}

	}
	
	private void runPairValidators(Bundle b1, Bundle b2) {
		for (PairValidator v : pairValidators) {
			v.validate(b1, b2);
		}
	}
	
	private void runBundleValidators(Bundle b) {
		for (BundleValidator v : bundleValidators) {
			v.validate(b);
		}
	}
	
}
