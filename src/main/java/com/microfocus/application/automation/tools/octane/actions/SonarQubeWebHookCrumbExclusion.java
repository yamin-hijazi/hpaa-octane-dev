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

package com.microfocus.application.automation.tools.octane.actions;

import hudson.Extension;
import hudson.security.csrf.CrumbExclusion;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * this class allows webhook resource to be excluded from CSRF validations
 * in case jenkins configured to have this kind of validation
 */

@Extension
public class SonarQubeWebHookCrumbExclusion extends CrumbExclusion {

	@Override
	public boolean process(HttpServletRequest req, HttpServletResponse resp, FilterChain chain) throws IOException, ServletException {
		String pathInfo = req.getPathInfo();
		if (pathInfo == null || pathInfo.isEmpty()) {
			return false;
		}
		if (!pathInfo.equals(getExclusionPath())) {
			return false;
		}
		chain.doFilter(req, resp);
		return true;
	}

	private String getExclusionPath() {
		return "/" + Webhooks.WEBHOOK_PATH + Webhooks.NOTIFY_METHOD;
	}
}