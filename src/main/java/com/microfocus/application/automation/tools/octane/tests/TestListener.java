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

package com.microfocus.application.automation.tools.octane.tests;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import com.microfocus.application.automation.tools.octane.tests.detection.UFTExtension;
import com.microfocus.application.automation.tools.octane.tests.xml.TestResultXmlWriter;
import hudson.Extension;
import hudson.FilePath;
import hudson.model.Run;
import hudson.tasks.Builder;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.xml.stream.XMLStreamException;
import java.util.List;

/**
 * Jenkins events life cycle listener for processing test results on build completed
 */
@Extension
@SuppressWarnings({"squid:S2699", "squid:S3658", "squid:S2259", "squid:S1872"})
public class TestListener {
	private static Logger logger = LogManager.getLogger(TestListener.class);

	private static final String STORMRUNNER_LOAD_TEST_RUNNER_CLASS = "StormTestRunner";
	private static final String STORMRUNNER_FUNCTIONAL_TEST_RUNNER_CLASS = "RunFromSrfBuilder";
	private static final String PERFORMANCE_CENTER_TEST_RUNNER_CLASS = "PcBuilder";
	public static final String TEST_RESULT_FILE = "mqmTests.xml";

	public boolean processBuild(Run run) {
		FilePath resultPath = new FilePath(new FilePath(run.getRootDir()), TEST_RESULT_FILE);
		TestResultXmlWriter resultWriter = new TestResultXmlWriter(resultPath, run);
		boolean success = true;
		boolean hasTests = false;
		String jenkinsRootUrl = Jenkins.getInstance().getRootUrl();
		HPRunnerType hpRunnerType = HPRunnerType.NONE;
		List<Builder> builders = JobProcessorFactory.getFlowProcessor(run.getParent()).tryGetBuilders();
		if (builders != null) {
			for (Builder builder : builders) {
				if (STORMRUNNER_LOAD_TEST_RUNNER_CLASS.equals(builder.getClass().getSimpleName())) {
					hpRunnerType = HPRunnerType.StormRunnerLoad;
					break;
				}
				if (STORMRUNNER_FUNCTIONAL_TEST_RUNNER_CLASS.equals(builder.getClass().getSimpleName())) {
					hpRunnerType = HPRunnerType.StormRunnerFunctional;
					break;
				}
				if (UFTExtension.RUN_FROM_FILE_BUILDER.equals(builder.getClass().getSimpleName()) || UFTExtension.RUN_FROM_ALM_BUILDER.equals(builder.getClass().getSimpleName())) {
					hpRunnerType = HPRunnerType.UFT;
					break;
				}
				if (PERFORMANCE_CENTER_TEST_RUNNER_CLASS.equals(builder.getClass().getSimpleName())) {
					hpRunnerType = HPRunnerType.PerformanceCenter;
					break;
				}
			}
		}

		try {
			for (OctaneTestsExtension ext : OctaneTestsExtension.all()) {
				if (ext.supports(run)) {
					TestResultContainer testResultContainer = ext.getTestResults(run, hpRunnerType, jenkinsRootUrl);
					if (testResultContainer != null && testResultContainer.getIterator().hasNext()) {
						resultWriter.writeResults(testResultContainer);
						hasTests = true;
					}
				}
			}
		} catch (Throwable t) {
			success = false;
			logger.error("failed to process test results", t);
		} finally {
			try {
				resultWriter.close();

				// we don't push individual maven module results (although we create the file for future use)
				if (!"hudson.maven.MavenBuild".equals(run.getClass().getName())) {
					if (success && hasTests) {
						String projectFullName = BuildHandlerUtils.getJobCiId(run);
						if (projectFullName != null) {
							OctaneSDK.getClients().forEach(octaneClient ->
									octaneClient.getTestsService().enqueuePushTestsResult(projectFullName, String.valueOf(run.getNumber())));
						}
					}
				}
			} catch (XMLStreamException xmlse) {
				success = false;
				logger.error("failed to finalize test results processing", xmlse);
			}
		}
		return success && hasTests;
	}
}
