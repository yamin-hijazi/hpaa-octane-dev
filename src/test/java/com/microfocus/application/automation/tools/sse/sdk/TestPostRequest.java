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

package com.microfocus.application.automation.tools.sse.sdk;

import java.util.Map;

import com.microfocus.application.automation.tools.sse.common.StringUtils;
import com.microfocus.application.automation.tools.sse.common.TestCase;
import com.microfocus.application.automation.tools.sse.sdk.request.PostRequest;
import org.junit.Assert;
import org.junit.Test;

import com.microfocus.application.automation.tools.sse.common.RestClient4Test;

@SuppressWarnings("squid:S2699")
public class TestPostRequest extends TestCase {
    
    @Test
    public void testPostRequestException() {
        
        Response response =
                new MockPostRequest(new MockRestClientPostRequestException(
                        URL,
                        DOMAIN,
                        PROJECT,
                        USER), RUN_ID).execute();
        Assert.assertTrue(PostRequestException.class.equals(response.getFailure().getClass()));
    }
    
    private class MockRestClientPostRequestException extends RestClient4Test {
        
        public MockRestClientPostRequestException(
                String url,
                String domain,
                String project,
                String username) {
            
            super(url, domain, project, username);
        }
        
        @Override
        public Response httpPost(
                String url,
                byte[] data,
                Map<String, String> headers,
                ResourceAccessLevel resourceAccessLevel) {
            
            throw new PostRequestException();
        }
    }
    
    private class PostRequestException extends NullPointerException {
        
        private static final long serialVersionUID = 1L;
        
    }
    
    private class MockPostRequest extends PostRequest {
        
        protected MockPostRequest(Client client, String runId) {

            super(client, runId);
        }
        
        @Override
        protected String getSuffix() {
            
            return StringUtils.EMPTY_STRING;
        }
    }
}
