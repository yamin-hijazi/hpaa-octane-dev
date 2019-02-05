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

package com.microfocus.application.automation.tools.sse.autenvironment.request.get;

import com.microfocus.application.automation.tools.sse.autenvironment.request.AUTEnvironmentResources;
import com.microfocus.application.automation.tools.sse.sdk.Client;
import com.microfocus.application.automation.tools.sse.sdk.request.GeneralGetRequest;

/**
 * Created by barush on 30/10/2014.
 */
public class GetParametersByAutEnvConfIdRequest extends GeneralGetRequest {
    
    String configurationId;
    
    public GetParametersByAutEnvConfIdRequest(Client client, String configurationId) {
        
        super(client);
        this.configurationId = configurationId;
    }
    
    @Override
    protected String getSuffix() {
        
        return AUTEnvironmentResources.AUT_ENVIRONMENT_PARAMETER_VALUES;
    }
    
    @Override
    protected String getQueryString() {
        
        return String.format("query={app-param-value-set-id[%s]}&page-size=2000", configurationId);
    }
}
