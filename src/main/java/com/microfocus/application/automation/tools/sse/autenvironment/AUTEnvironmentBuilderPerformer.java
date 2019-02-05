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

package com.microfocus.application.automation.tools.sse.autenvironment;

import com.microfocus.application.automation.tools.common.SSEException;
import com.microfocus.application.automation.tools.model.AUTEnvironmentResolvedModel;
import com.microfocus.application.automation.tools.model.AutEnvironmentParameterModel;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.authenticator.AuthenticationTool;
import hudson.EnvVars;
import hudson.util.VariableResolver;

import java.util.Collection;
import java.util.List;

/**
 * Created by barush on 29/10/2014.
 */
public class AUTEnvironmentBuilderPerformer {

    private Logger logger;
    private AUTEnvironmentResolvedModel model;
    private RestClient restClient;
    private VariableResolver<String> buildVariableResolver;
    private String autEnvironmentConfigurationIdToReturn;

    public AUTEnvironmentBuilderPerformer(
            AUTEnvironmentResolvedModel model,
            VariableResolver<String> buildVariableResolver,
            Logger logger) {

        this.model = model;
        this.logger = logger;
        this.buildVariableResolver = buildVariableResolver;
    }

    public void start(EnvVars envVars) {
        try {
            if (AuthenticationTool.getInstance().authenticate(getClient(),
                    model.getAlmUserName(),
                    model.getAlmPassword(),
                    model.getAlmServerUrl(),
                    model.getClientType(),
                    logger)) {
                logger.log(String.format(
                        "Alm server url: %s", model.getAlmServerUrl()));
                performAutOperations(envVars);
            }
        } catch (Throwable cause) {
            logger.log(String.format(
                    "Failed to update ALM AUT Environment. Cause: %s",
                    cause.getMessage()));
            throw cause;
        }
    }

    public String getAutEnvironmentConfigurationIdToReturn() {
        return autEnvironmentConfigurationIdToReturn;
    }

    private void performAutOperations(EnvVars envVars) {
        String autEnvironmentId = model.getAutEnvironmentId();

        AUTEnvironmentManager autEnvironmentManager =
                new AUTEnvironmentManager(getClient(), logger);
        String parametersRootFolderId =
                autEnvironmentManager.getParametersRootFolderIdByAutEnvId(autEnvironmentId);
        String autEnvironmentConfigurationId =
                getAutEnvironmentConfigurationId(autEnvironmentManager, autEnvironmentId);

        assignValuesToAutParameters(autEnvironmentConfigurationId, parametersRootFolderId, envVars);
        autEnvironmentConfigurationIdToReturn = autEnvironmentConfigurationId;

    }

    private void assignValuesToAutParameters(
            String autEnvironmentConfigurationId,
            String parametersRootFolderId,
            EnvVars envVars) {

        List<AutEnvironmentParameterModel> autEnvironmentParameters =
                model.getAutEnvironmentParameters();
        if (autEnvironmentParameters == null || autEnvironmentParameters.size() == 0) {
            logger.log("There's no AUT Environment parameters to assign for this build...");
            return;
        }

        String selectedNode = envVars.get("NODE_NAME");
        AUTEnvironmentParametersManager parametersManager =
                new AUTEnvironmentParametersManager(
                        getClient(),
                        autEnvironmentParameters,
                        parametersRootFolderId,
                        autEnvironmentConfigurationId,
                        buildVariableResolver,
                        model.getPathToJsonFile(),
                        logger,
                        selectedNode);

        Collection<AUTEnvironmnentParameter> parametersToUpdate =
                parametersManager.getParametersToUpdate();
        parametersManager.updateParametersValues(parametersToUpdate);
        logger.log("assignValuesToAutParameters");
    }

    private String getAutEnvironmentConfigurationId(
            AUTEnvironmentManager autEnvironmentManager,
            String autEnvironmentId) {

        String autEnvironmentConfigurationId =
                autEnvironmentManager.shouldUseExistingConfiguration(model)
                        ? model.getExistingAutEnvConfId()
                        : autEnvironmentManager.createNewAutEnvironmentConfiguration(
                        autEnvironmentId,
                        model);

        if (StringUtils.isNullOrEmpty(autEnvironmentConfigurationId)) {
            throw new SSEException("There's no AUT Environment Configuration in order to proceed");
        }
        return autEnvironmentConfigurationId;

    }

    private RestClient getClient() {
        if (restClient == null) {
            restClient =
                    new RestClient(
                            model.getAlmServerUrl(),
                            model.getAlmDomain(),
                            model.getAlmProject(),
                            model.getAlmUserName());
        }

        return restClient;
    }
}
