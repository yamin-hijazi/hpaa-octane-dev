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

package com.microfocus.application.automation.tools.octane.events;

import com.hp.octane.integrations.OctaneSDK;
import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.events.CIEvent;
import com.hp.octane.integrations.dto.events.CIEventType;
import com.microfocus.application.automation.tools.octane.executor.UftTestDiscoveryDispatcher;
import com.microfocus.application.automation.tools.octane.model.processors.projects.JobProcessorFactory;
import com.microfocus.application.automation.tools.settings.OctaneServerSettingsBuilder;
import hudson.Extension;
import hudson.model.Item;
import hudson.model.listeners.ItemListener;
import jenkins.model.Jenkins;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jenkinsci.plugins.workflow.job.WorkflowJob;

/**
 * Run Listener that handles basic CI item events and dispatches notifications to the Octane server
 * User: shitritn
 * Date: 12/06/18
 * Time: 09:33
 */

@Extension
public class GlobalEventsListenerOctaneImpl extends ItemListener {
	private static final Logger logger = LogManager.getLogger(GlobalEventsListenerOctaneImpl.class);
	private static final DTOFactory dtoFactory = DTOFactory.getInstance();

	@Override
	public void onLoaded() {
		OctaneServerSettingsBuilder.getOctaneSettingsManager().initOctaneClients();
	}

	@Override
	public void onDeleted(Item item) {
		try {
			CIEvent event;
			if (item.getParent() != null && item.getParent().getClass().getName().equalsIgnoreCase(JobProcessorFactory.WORKFLOW_MULTI_BRANCH_JOB_NAME)) {
				event = dtoFactory.newDTO(CIEvent.class)
						.setEventType(CIEventType.DELETED)
						.setProject(JobProcessorFactory.getFlowProcessor((WorkflowJob) item).getTranslateJobName());

				OctaneSDK.getClients().forEach(client -> client.getEventsService().publishEvent(event));
			}
		} catch (Throwable throwable) {
			logger.error("failed to build and/or dispatch DELETED event for " + item, throwable);
		}
	}

	@Override
	public void onBeforeShutdown() {
		OctaneSDK.getClients().forEach(OctaneSDK::removeClient);

		UftTestDiscoveryDispatcher dispatcher = Jenkins.getInstance().getExtensionList(UftTestDiscoveryDispatcher.class).get(0);
		dispatcher.close();
	}
}
