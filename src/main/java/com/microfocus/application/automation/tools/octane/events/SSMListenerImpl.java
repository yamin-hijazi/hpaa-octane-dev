/*
 * © Copyright 2013 EntIT Software LLC
 *  Certain versions of software and/or documents (“Material”) accessible here may contain branding from
 *  Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 *  the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 *  and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 *  marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * © Copyright 2012-2018 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors (“Micro Focus”) are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 *
 */

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.microfocus.application.automation.tools.octane.configuration.ConfigurationService;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.Extension;
import hudson.model.Run;
import hudson.model.listeners.RunListener;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

/**
 * Created by dshmaya on 25/07/2018
 * Jenkins events listener to send Security Scan results to octane
 */

@Extension
public class SSMListenerImpl extends RunListener<Run> {
	private static Logger logger = LogManager.getLogger(SSMListenerImpl.class);

	@Override
	public void onFinalized(Run run) {
		if (ConfigurationService.getModel().isSuspend()) {
			return;
		}
		saveReport(run);

		if (ConfigurationService.getServerConfiguration() != null && ConfigurationService.getServerConfiguration().isValid()) {
			String jobCiId = BuildHandlerUtils.getJobCiId(run);
			String buildCiId = BuildHandlerUtils.getBuildCiId(run);
			logger.info("enqueued build '" + jobCiId + " #" + buildCiId + "' for Security Scan submission");
			OctaneSDK.getInstance().getVulnerabilitiesService().enqueuePushVulnerabilitiesScanResult(jobCiId, buildCiId);
		} else {
			logger.warn("Octane configuration is not valid");
		}
	}

	private void saveReport(Run run) {
		String vulnerabilitiesScanFilePath = run.getLogFile().getParent() + File.separator + "securityScan.json";
		try (PrintWriter out = new PrintWriter(vulnerabilitiesScanFilePath)) {
			out.write("{\n" +
					"  \"data\": [\n" +
					"    {\n" +
					"      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.low\"      },\n" +
					"      \"package\": \"hp.com\",\n" +
					"      \"line\": 10,\n" +
					"      \"remote_id\": \"1034\",\n" +
					"      \"primary_location_full\": \"commitsRuleEngine.java\",\n" +
					"      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
					"      \"owner_email\":\"daniel.shmaya@hpe.com\",\n" +
					"      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.closed\"      },\n" +
					"      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
					"      \"tool_name\": \"external tool\",\n" +
					"      \"external_link\":\"some url here\",\n" +
					"       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.maybe_an_issue\"      },\n" +
					"       \"extended_data\" : {\"key\":\"value\",\"key\":\"value\"},\n" +
					"      \"category\": \"category\"\n" +
					"    }, {\n" +
					"      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.high\"      },\n" +
					"      \"package\": \"hp.com.com\",\n" +
					"      \"line\": 11,\n" +
					"      \"remote_id\": \"1032\",\n" +
					"      \"primary_location_full\": \"entities-factory.html\",\n" +
					"      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
					"      \"owner_email\":\"sa@nga\",\n" +
					"      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.new\"      },\n" +
					"      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
					"      \"tool_name\": \"external too 2\",\n" +
					"      \"external_link\":\"some url here 2\",\n" +
					"       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.reviewed\"      },\n" +
					"       \"extended_data\" : {\"key1\":\"value1\",\"key2\":\"value2\"},\n" +
					"      \"category\": \"category 2\"\n" +
					"    }\n" +
					"  ]\n" +
					"}");

			out.flush();
			out.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}


}
