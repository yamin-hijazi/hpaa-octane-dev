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

package com.microfocus.application.automation.tools.sse.sdk.request;

import com.microfocus.application.automation.tools.sse.common.RestXmlUtils;
import com.microfocus.application.automation.tools.sse.sdk.Client;

import java.util.Map;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class GetLabRunEntityTestSetRunsRequest extends GetRequest {
    
    public GetLabRunEntityTestSetRunsRequest(Client client, String runId) {
        
        super(client, runId);
    }
    
    @Override
    protected String getSuffix() {
        return "procedure-testset-instance-runs";
    }
    
    @Override
    protected String getQueryString() {
        
        return String.format("query={procedure-run[%s]}&page-size=2000", _runId);
    }

    @Override
    protected Map<String, String> getHeaders() {
        // It's pretty weird that in 1260 p1 the xml header should be provided.
        // Otherwise the server would generate wrong query sql.
        return RestXmlUtils.getAppXmlHeaders();
    }
    
}
