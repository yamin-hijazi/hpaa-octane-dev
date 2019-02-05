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

package com.microfocus.application.automation.tools.results;

import hudson.model.AbstractBuild;
import hudson.model.DirectoryBrowserSupport;
import hudson.model.ModelObject;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.StaplerResponse;

import javax.servlet.ServletException;
import java.io.IOException;

public class SrfDetailReport implements ModelObject {

    private String name = "";
    private String color = "";
    private String duration = "";
    private String pass = "";
    private String fail = "";
    private AbstractBuild<?,?> build;
    private DirectoryBrowserSupport _directoryBrowserSupport = null;

    public SrfDetailReport(AbstractBuild<?,?> build, String name, DirectoryBrowserSupport directoryBrowserSupport) {
        this.build = build;
        this.name = name;
        _directoryBrowserSupport = directoryBrowserSupport;
    }

    @Override
    public String getDisplayName() {
        return name;
    }

    public AbstractBuild<?,?> getBuild() {
        return build;
    }

    public String getName() {
        return name;
    }

    public void doDynamic(StaplerRequest req, StaplerResponse rsp) throws IOException, ServletException {

        if (_directoryBrowserSupport != null)
            _directoryBrowserSupport.generateResponse(req, rsp, this);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String value) {
        color = value;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String value) {
        duration = value;
    }

    public String getPass() {
        return pass;
    }

    public void setPass(String value) {
        pass = value;
    }

    public String getFail() {
        return fail;
    }

    public void setFail(String value) {
        fail = value;
    }

}
