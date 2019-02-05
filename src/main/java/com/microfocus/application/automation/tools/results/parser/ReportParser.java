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

package com.microfocus.application.automation.tools.results.parser;

import java.io.InputStream;
import java.util.List;

import com.microfocus.application.automation.tools.results.service.almentities.AlmTestSet;

public interface ReportParser {
	String TESTING_FRAMEWORK_JUNIT = "JUnit";
	String EXTERNAL_TEST_TYPE = "EXTERNAL-TEST";
	String REPORT_FORMAT_JENKINS_JUNIT_PLUGIN = "Jenkins JUnit Plugin";
	String REPORT_FORMAT_ANT = "Ant";
	String REPORT_FORMAT_MAVEN_SUREFIRE_PLUGIN = "Maven Surefire Plugin";
	String EXTERNAL_TEST_SET_TYPE_ID = "hp.qc.test-set.external";
	String EXTERNAL_TEST_INSTANCE_TYPE_ID = "hp.qc.test-instance.external-test";
	String EXTERNAL_RUN_TYPE_ID = "hp.qc.run.external-test";
	
	List<AlmTestSet> parseTestSets(InputStream reportInputStream, String testingFramework, String testingTool) throws ReportParseException;
}
