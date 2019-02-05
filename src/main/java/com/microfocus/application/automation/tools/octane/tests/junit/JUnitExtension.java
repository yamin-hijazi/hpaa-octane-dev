/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.octane.tests.junit;

import com.google.inject.Inject;
import com.microfocus.application.automation.tools.octane.actions.cucumber.CucumberTestResultsAction;
import com.microfocus.application.automation.tools.octane.executor.CheckOutSubDirEnvContributor;
import com.microfocus.application.automation.tools.octane.tests.HPRunnerType;
import com.microfocus.application.automation.tools.octane.tests.OctaneTestsExtension;
import com.microfocus.application.automation.tools.octane.tests.TestResultContainer;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFields;
import com.microfocus.application.automation.tools.octane.tests.detection.ResultFieldsDetectionService;
import com.microfocus.application.automation.tools.octane.tests.impl.ObjectStreamIterator;
import hudson.Extension;
import hudson.FilePath;
import hudson.maven.MavenBuild;
import hudson.maven.MavenModule;
import hudson.maven.MavenModuleSetBuild;
import hudson.model.Run;
import hudson.remoting.VirtualChannel;
import hudson.tasks.test.AbstractTestResultAction;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.remoting.Role;
import org.jenkinsci.remoting.RoleChecker;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Converter of Jenkins test report to ALM Octane test report format(junitResult.xml->mqmTests.xml)
 */
@Extension
public class JUnitExtension extends OctaneTestsExtension {
	private static Logger logger = LogManager.getLogger(JUnitExtension.class);

	private static final String STORMRUNNER_LOAD = "StormRunner Load";
	private static final String STORMRUNNER_FUNCTIONAL = "StormRunner Functional";
	private static final String LOAD_RUNNER = "LoadRunner";
	private static final String PERFORMANCE_CENTER_RUNNER = "Performance Center";
	private static final String PERFORMANCE_TEST_TYPE = "Performance";

	private static final String JUNIT_RESULT_XML = "junitResult.xml"; // NON-NLS

	private static final String PERFORMANCE_REPORT = "PerformanceReport";
	private static final String TRANSACTION_SUMMARY = "TransactionSummary";

	@Inject
	private ResultFieldsDetectionService resultFieldsDetectionService;

	public boolean supports(Run<?, ?> build) {
		if (build.getAction(CucumberTestResultsAction.class) != null) {
			logger.debug("CucumberTestResultsAction found. Will not process JUnit results.");
			return false;
		} else if (build.getAction(AbstractTestResultAction.class) != null) {
			logger.debug("AbstractTestResultAction found, JUnit results expected");
			return true;
		} else {
			logger.debug("AbstractTestResultAction not found, no JUnit results expected");
			return false;
		}
	}

	@Override
	public TestResultContainer getTestResults(Run<?, ?> run, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
		logger.debug("Collecting JUnit results");

		boolean isLoadRunnerProject = isLoadRunnerProject(run);
		FilePath resultFile = new FilePath(run.getRootDir()).child(JUNIT_RESULT_XML);
		if (resultFile.exists()) {
			logger.debug("JUnit result report found");
			ResultFields detectedFields = getResultFields(run, hpRunnerType, isLoadRunnerProject);
			FilePath workspace = BuildHandlerUtils.getWorkspace(run);
			if (workspace == null) {
				logger.error("Received null workspace : " + run);
				return null;
			}

			FilePath filePath = workspace.act(new GetJUnitTestResults(run, Collections.singletonList(resultFile), false, hpRunnerType, jenkinsRootUrl));
			return new TestResultContainer(new ObjectStreamIterator<>(filePath), detectedFields);
		} else {
			//avoid java.lang.NoClassDefFoundError when maven plugin is not present
			if ("hudson.maven.MavenModuleSetBuild".equals(run.getClass().getName())) {
				logger.debug("MavenModuleSetBuild detected, looking for results in maven modules");

				List<FilePath> resultFiles = new LinkedList<>();
				Map<MavenModule, MavenBuild> moduleLastBuilds = ((MavenModuleSetBuild) run).getModuleLastBuilds();
				for (MavenBuild mavenBuild : moduleLastBuilds.values()) {
					AbstractTestResultAction action = mavenBuild.getAction(AbstractTestResultAction.class);
					if (action != null) {
						FilePath moduleResultFile = new FilePath(mavenBuild.getRootDir()).child(JUNIT_RESULT_XML);
						if (moduleResultFile.exists()) {
							logger.debug("Found results in " + mavenBuild.getFullDisplayName());
							resultFiles.add(moduleResultFile);
						}
					}
				}
				if (!resultFiles.isEmpty()) {
					ResultFields detectedFields = getResultFields(run, hpRunnerType, isLoadRunnerProject);
					FilePath filePath = BuildHandlerUtils.getWorkspace(run).act(new GetJUnitTestResults(run, resultFiles, false, hpRunnerType, jenkinsRootUrl));
					return new TestResultContainer(new ObjectStreamIterator<>(filePath), detectedFields);
				}
			}
			logger.debug("No JUnit result report found");
			return null;
		}
	}

	private ResultFields getResultFields(Run<?, ?> build, HPRunnerType hpRunnerType, boolean isLoadRunnerProject) throws InterruptedException {
		ResultFields detectedFields;
		if (hpRunnerType.equals(HPRunnerType.StormRunnerLoad)) {
			detectedFields = new ResultFields(null, STORMRUNNER_LOAD, null);
		} else if (hpRunnerType.equals(HPRunnerType.StormRunnerFunctional)) {
			detectedFields = new ResultFields(null, STORMRUNNER_FUNCTIONAL, null);
		} else if (isLoadRunnerProject) {
			detectedFields = new ResultFields(null, LOAD_RUNNER, null);
		} else if (hpRunnerType.equals(HPRunnerType.PerformanceCenter)) {
			detectedFields = new ResultFields(null, PERFORMANCE_CENTER_RUNNER, null, PERFORMANCE_TEST_TYPE);
		} else {
			detectedFields = resultFieldsDetectionService.getDetectedFields(build);
		}

		return detectedFields;
	}

	private boolean isLoadRunnerProject(Run run) throws IOException, InterruptedException {
		FilePath performanceReportFolder = new FilePath(run.getRootDir()).child(PERFORMANCE_REPORT);
		FilePath transactionSummaryFolder = new FilePath(run.getRootDir()).child(TRANSACTION_SUMMARY);
		return performanceReportFolder.exists() &&
				performanceReportFolder.isDirectory() &&
				transactionSummaryFolder.exists() &&
				transactionSummaryFolder.isDirectory();
	}

	private static class GetJUnitTestResults implements FilePath.FileCallable<FilePath> {

		private final List<FilePath> reports;
		private final String jobName;
		private final String buildId;
		private final String jenkinsRootUrl;
		private final HPRunnerType hpRunnerType;
		private FilePath filePath;
		private List<ModuleDetection> moduleDetection;
		private long buildStarted;
		private FilePath workspace;
		private boolean stripPackageAndClass;
		private String sharedCheckOutDirectory;

		//this class is run on master and JUnitXmlIterator is runnning on slave.
		//this object pass some master2slave data
		private Object additionalContext;
		private String buildRootDir;

		public GetJUnitTestResults(Run<?, ?> build, List<FilePath> reports, boolean stripPackageAndClass, HPRunnerType hpRunnerType, String jenkinsRootUrl) throws IOException, InterruptedException {
			this.reports = reports;
			this.filePath = new FilePath(build.getRootDir()).createTempFile(getClass().getSimpleName(), null);
			this.buildStarted = build.getStartTimeInMillis();
			this.workspace = BuildHandlerUtils.getWorkspace(build);
			this.stripPackageAndClass = stripPackageAndClass;
			this.hpRunnerType = hpRunnerType;
			this.jenkinsRootUrl = jenkinsRootUrl;
			this.buildRootDir = build.getRootDir().getCanonicalPath();
			this.sharedCheckOutDirectory = CheckOutSubDirEnvContributor.getSharedCheckOutDirectory(build.getParent());

			this.jobName = build.getParent().getName();
			this.buildId = build.getId();
			moduleDetection = Arrays.asList(
					new MavenBuilderModuleDetection(build),
					new MavenSetModuleDetection(build),
					new ModuleDetection.Default());


			if (HPRunnerType.UFT.equals(hpRunnerType)) {

				//extract folder names for created tests
				String reportFolder = buildRootDir + "/archive/UFTReport";
				List<String> testFolderNames = new ArrayList<>();
				File reportFolderFile = new File(reportFolder);
				if (reportFolderFile.exists()) {
					File[] children = reportFolderFile.listFiles();
					if (children != null) {
						for (File child : children) {
							testFolderNames.add(child.getName());
						}
					}
				}
				additionalContext = testFolderNames;
			}
			if (HPRunnerType.StormRunnerLoad.equals(hpRunnerType)) {
				try {
					File file = new File(build.getRootDir(), "log");
					Path path = Paths.get(file.getPath());
					additionalContext = Files.readAllLines(path, StandardCharsets.UTF_8);
				} catch (Exception e) {
					logger.error("Failed to add log file for StormRunnerLoad :" + e.getMessage());
				}
			} else if (HPRunnerType.StormRunnerFunctional.equals(hpRunnerType)) {
				try {
					File file = new File(build.getRootDir(), "srf-test-result-urls");
					Path path = Paths.get(file.getPath());
					List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
					Map<String, String> map = new HashMap<>();
					for (String line : lines) {
						String[] parts = line.split(";");
						if (parts.length == 2) {
							map.put(parts[0], parts[1]);
						}
					}
					additionalContext = map;
				} catch (Exception e) {
					logger.error("Failed to read/parse srf-test-result-urls file for StormRunnerFunctional :" + e.getMessage());
				}
			}
		}

		@Override
		public FilePath invoke(File f, VirtualChannel channel) throws IOException, InterruptedException {
			OutputStream os = filePath.write();
			BufferedOutputStream bos = new BufferedOutputStream(os);
			ObjectOutputStream oos = new ObjectOutputStream(bos);

			try {
				for (FilePath report : reports) {
					JUnitXmlIterator iterator = new JUnitXmlIterator(report.read(), moduleDetection, workspace, sharedCheckOutDirectory, jobName, buildId, buildStarted, stripPackageAndClass, hpRunnerType, jenkinsRootUrl, additionalContext);
					while (iterator.hasNext()) {
						oos.writeObject(iterator.next());
					}
				}
			} catch (XMLStreamException e) {
				throw new IOException(e);
			}
			os.flush();

			oos.close();
			return filePath;
		}

		@Override
		public void checkRoles(RoleChecker roleChecker) throws SecurityException {
			roleChecker.check(this, Role.UNKNOWN);
		}
	}

	/*
	 * To be used in tests only.
	 */
	public void _setResultFieldsDetectionService(ResultFieldsDetectionService detectionService) {
		this.resultFieldsDetectionService = detectionService;
	}
}
