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
package com.microfocus.application.automation.tools.run;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.exceptions.SonarIntegrationException;
import com.microfocus.application.automation.tools.octane.model.SonarHelper;
import com.microfocus.application.automation.tools.octane.actions.WebhookAction;
import com.microfocus.application.automation.tools.octane.Messages;
import com.microfocus.application.automation.tools.octane.actions.Webhooks;
import hudson.Extension;
import hudson.ExtensionList;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import jenkins.model.GlobalConfiguration;
import jenkins.model.Jenkins;
import jenkins.tasks.SimpleBuildStep;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public class SonarOctaneListener extends Builder implements SimpleBuildStep {

	// these properties will be used for sonar communication
	private String sonarProjectKey;
	private String sonarToken;
	private String sonarServerUrl;
	boolean pushVulnerabilities;
	boolean pushCoverage;

	private List<SonarHelper.DataType> dataTypeList = new ArrayList();


	// inject variables from job configuration if exist
	@DataBoundConstructor
	public SonarOctaneListener(boolean pushVulnerabilities, boolean pushCoverage) {
		this.sonarProjectKey = sonarProjectKey == null ? "" : sonarProjectKey;
		this.sonarToken = sonarToken == null ? "" : sonarToken;
		this.sonarServerUrl = sonarServerUrl == null ? "" : sonarServerUrl;
		this.pushCoverage = pushCoverage;
		this.pushVulnerabilities = pushVulnerabilities;

		if (pushVulnerabilities){
			dataTypeList.add(SonarHelper.DataType.VULNERABILITIES);
		}
		if(pushCoverage){
			dataTypeList.add(SonarHelper.DataType.COVERAGE);
		}

	}

	/**
	 * get project key
	 *
	 * @return
	 */
	public String getSonarProjectKey() {
		return sonarProjectKey;
	}

	/**
	 * get server token
	 *
	 * @return
	 */
	public String getSonarToken() {
		return sonarToken;
	}

	/**
	 * get server url
	 *
	 * @return
	 */
	public String getSonarServerUrl() {
		return sonarServerUrl;
	}


	public boolean isPushVulnerabilities() {
		return pushVulnerabilities;
	}

	public boolean isPushCoverage() {
		return pushCoverage;
	}

	/**
	 * this method is initializing sonar server details from listener configuration or
	 * sonar plugin data
	 *
	 * @param run               current run
	 * @param allConfigurations jenkins global configuration
	 * @throws InterruptedException
	 */
	private String[] getSonarDetails(@Nonnull Run<?, ?> run, ExtensionList<GlobalConfiguration> allConfigurations, TaskListener listener) throws InterruptedException {
		String[] serverDetails = new String[2];
		// if one of the properties is empty, need to query sonar plugin from jenkins to get the data
		if (sonarProjectKey.isEmpty() || sonarToken.isEmpty() || sonarServerUrl.isEmpty()) {
			try {
				if (allConfigurations != null) {
					SonarHelper adapter = new SonarHelper(run, listener);
					serverDetails[0] = sonarServerUrl.isEmpty() ? adapter.getServerUrl() : sonarServerUrl;
					serverDetails[1] = sonarToken.isEmpty() ? adapter.getServerToken() : sonarToken;

				}
			} catch (Exception e) {
				throw new InterruptedException("exception occurred while init sonar tracker for job " + run.getDisplayName() + " error message: " + e.getMessage());
			}
		}
		return serverDetails;
	}

	private String getBuildNumber(Run<?, ?> run) {
		if (run instanceof AbstractBuild) {
			AbstractBuild abstractBuild = (AbstractBuild) run;
			return String.valueOf(abstractBuild.getNumber());
		}
		return "";
	}

	/**
	 * Run this step.
	 *
	 * @param run       a build this is running as a part of
	 * @param workspace a workspace to use for any file operations
	 * @param launcher  a way to start processes
	 * @param listener  a place to send output
	 * @throws InterruptedException if the step is interrupted
	 */
	@Override
	public void perform(@Nonnull Run<?, ?> run, @Nonnull FilePath workspace, @Nonnull Launcher launcher,
	                    @Nonnull TaskListener listener) throws InterruptedException, IOException {
		PrintStream logger = listener.getLogger();
		ExtensionList<GlobalConfiguration> allConfigurations = GlobalConfiguration.all();
		String jenkinsRoot = Jenkins.getInstance().getRootUrl();
		String callbackWebHooksURL = jenkinsRoot + Webhooks.WEBHOOK_PATH + Webhooks.NOTIFY_METHOD;

		if (run instanceof AbstractBuild) {
			logger.println("callback URL for jenkins resource will be set to: " + callbackWebHooksURL);
			String[] serverDetails = getSonarDetails(run, allConfigurations, listener);
			OctaneSDK.getClients().forEach(octaneClient -> {
				try {
					octaneClient.getSonarService().ensureSonarWebhookExist(callbackWebHooksURL, serverDetails[0], serverDetails[1]);
				} catch (SonarIntegrationException e) {
					logger.println("Web-hook registration in sonarQube for build " + getBuildNumber(run) + " failed: " + e.getMessage());
				}
			});
			run.addAction(new WebhookAction(true, serverDetails[0],dataTypeList));

		}
	}

	@Override
	public SonarDescriptor getDescriptor() {
		return (SonarDescriptor) super.getDescriptor();
	}


	@Extension
	public static class SonarDescriptor extends BuildStepDescriptor<Builder> {

		public SonarDescriptor() {
			load();
		}

		@Override
		public String getDisplayName() {
			return "ALM Octane SonarQube listener";
		}

		/**
		 * test sonar connection
		 *
		 * @param url
		 * @param token
		 * @param projectKey
		 * @return
		 */
		public FormValidation doTestConnection(@QueryParameter("sonarServerUrl") final String url, @QueryParameter("sonarToken") final String token,
		                                       @QueryParameter("sonarProjectKey") final String projectKey) {
			if(OctaneSDK.getClients().isEmpty()){
				return FormValidation.warning(Messages.missingOctaneConfiguration());
			}
			if (url.isEmpty()) {
				return FormValidation.warning(Messages.missingSonarServerUrl());
			} else {
				String connectionStatus = OctaneSDK.getClients().get(0).getSonarService().getSonarStatus(projectKey);
				if (!"CONNECTION_FAILURE".equals(connectionStatus)) {
					return FormValidation.ok("Validation passed. Connected successfully to server " + url);
				} else {
					return FormValidation.warning(Messages.cannotEstablishSonarConnection());
				}
			}
		}

		@Override
		public boolean isApplicable(Class<? extends AbstractProject> aClass) {
			return true;
		}
	}
}
