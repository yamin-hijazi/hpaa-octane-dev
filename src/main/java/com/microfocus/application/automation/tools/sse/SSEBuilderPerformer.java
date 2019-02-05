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

package com.microfocus.application.automation.tools.sse;

import com.microfocus.application.automation.tools.model.SseModel;
import com.microfocus.application.automation.tools.rest.RestClient;
import com.microfocus.application.automation.tools.sse.result.model.junit.Testsuites;
import com.microfocus.application.automation.tools.sse.sdk.Args;
import com.microfocus.application.automation.tools.sse.sdk.Logger;
import com.microfocus.application.automation.tools.sse.sdk.RunManager;
import hudson.util.VariableResolver;

/***
 * 
 * @author Effi Bar-She'an
 * @author Dani Schreiber
 * 
 */
public class SSEBuilderPerformer {
    
    private final RunManager _runManager = new RunManager();
    
    public Testsuites start(
            SseModel model,
            Logger logger,
            VariableResolver<String> buildVariableResolver) throws InterruptedException {
        
        Testsuites ret;

        Args args = new ArgsFactory().createResolved(model, buildVariableResolver);

        RestClient restClient;

        restClient = new RestClient(args.getUrl(),
                args.getDomain(),
                args.getProject(),
                args.getUsername());

        ret = _runManager.execute(restClient, args, logger);
        return ret;
    }
    
    public void stop() {
        _runManager.stop();
    }
}
