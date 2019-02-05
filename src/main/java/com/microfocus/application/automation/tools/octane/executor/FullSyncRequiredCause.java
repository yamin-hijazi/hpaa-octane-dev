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

package com.microfocus.application.automation.tools.octane.executor;

import hudson.model.Cause;

/**
 * FullSyncRequiredCause
 * Informational class for full sync case.
 * Used for SVN SCM delete action : in this case we don't receive inforamtion about deleted files,
 * therefore full sync required to update ALM Octane entities correctly 
 */
public class FullSyncRequiredCause extends Cause {

    private String buildId;

    public FullSyncRequiredCause(String buildId) {
        this.buildId = buildId;
    }

    public static FullSyncRequiredCause create(String buildId) {
        return new FullSyncRequiredCause(buildId);
    }

    @Override
    public String getShortDescription() {
        return String.format("Triggered by build #%s with full sync parameter.", buildId);
    }
}
