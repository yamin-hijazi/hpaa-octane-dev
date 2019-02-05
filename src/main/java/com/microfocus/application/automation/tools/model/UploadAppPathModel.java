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

package com.microfocus.application.automation.tools.model;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import org.kohsuke.stapler.DataBoundConstructor;

/**
 * Created with IntelliJ IDEA.
 * User: jingwei
 * Date: 5/20/16
 * Time: 2:52 PM
 * To change this template use File | Settings | File Templates.
 */
public class UploadAppPathModel extends AbstractDescribableImpl<UploadAppPathModel> {
    private String mcAppPath;

    @DataBoundConstructor
    public UploadAppPathModel(String mcAppPath) {
        this.mcAppPath = mcAppPath;
    }

    public String getMcAppPath() {
        return mcAppPath;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<UploadAppPathModel> {
        public String getDisplayName() {
            return "";
        }
    }
}
