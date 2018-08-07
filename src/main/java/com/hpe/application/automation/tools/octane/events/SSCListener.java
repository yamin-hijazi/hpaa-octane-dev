package com.hpe.application.automation.tools.octane.events;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hpe.application.automation.tools.octane.configuration.ConfigurationService;
import com.hpe.application.automation.tools.ssc.Issues;
import com.hpe.application.automation.tools.ssc.ProjectVersions;
import com.hpe.application.automation.tools.ssc.SscConnector;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Run;
import hudson.tasks.Publisher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hijaziy on 7/23/2018.
 */
public class SSCListener {

    static class ProjectVersion {
        public String project;
        public String version;

        public ProjectVersion(String projectName, String projectVersion) {
            this.project = projectName;
            this.version = projectVersion;
        }
    }

    public void handleSSC(Run r) {

        if (!isSSCProject(r)) {
            return;
        }

        ProjectVersion project = getProjectVersion(r);
        String sscServer = ConfigurationService.getSSCServer();
        SSCFortifyConfigurations sscFortifyConfigurations = new SSCFortifyConfigurations();
        //"Basic QWRtaW46ZGV2c2Vjb3Bz"
        sscFortifyConfigurations.baseToken = "Basic QWRtaW46ZGV2c2Vjb3BzMQ==";
        sscFortifyConfigurations.projectName = project.project;
        sscFortifyConfigurations.projectVersion = project.version;
        sscFortifyConfigurations.serverURL = sscServer;

        SscConnector sscConnector = new SscConnector(sscFortifyConfigurations);

        ProjectVersions.ProjectVersion projectVersion =
                sscConnector.getProjectVersion();
//        if(projectVersion.currentState.issueCountDelta == 0){
//            return; // Do nothing.
//        }

        Issues issues = sscConnector.readIssuesOfLastestScan(projectVersion);
        createOctaneIssues(issues);
        //fortify
        //1. get the summery data
        //2. use summery data (if relevant ) and try get last scan from SSC
        //3. after research the polling mechanise - see if we should use it here or get it free from fortify plugin
        //4. any way we need to fetch last scan, or better fetch scan by this job and build id
        //octanesdk
        //1. inject to octane sdk : job identifiers (ci-instance , job id, build id) + fetch result
        //2. sdk will convert results to issues entity collection (json) and call the inject resource in octane
        //3. the sdk part might have to be asynchronous

    }

    private void createOctaneIssues(Issues issues) {
        if(issues == null){
            return;
        }
        DTOFactory dtoFactory = DTOFactory.getInstance();
        for (Issues.Issue issue : issues.data) {
            OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
            setOctaneAnalysis(dtoFactory, issue, octaneIssue);
            setOctaneSeverity(dtoFactory, issue, octaneIssue);
            setOctaneStatus(dtoFactory, issue, octaneIssue);
            Map extendedData = getExtendedData(issue);
            octaneIssue.set_extended_data(extendedData);
            octaneIssue.set_primary_location_full(issue.primaryLocation);
            octaneIssue.set_line(issue.lineNumber);
        }
    }

    private void setOctaneAnalysis(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
//        "issueStatus" : "Unreviewed", - analysis
//        "audited" : false,- analysis
//        "reviewed" : null, - analysis ?

//        "list_node.issue_analysis_node.not_an_issue"
//        "list_node.issue_analysis_node.maybe_an_issue"
//        "list_node.issue_analysis_node.bug_submitted"
//        "list_node.issue_analysis_node.reviewed"
        String analysisId = null;
        if (issue.issueStatus != null && issue.issueStatus.equalsIgnoreCase("reviewed")) {
            analysisId = "list_node.issue_analysis_node.reviewed";
        } else if (issue.reviewed != null && issue.reviewed) {
            analysisId = "list_node.issue_analysis_node.reviewed";
        } else if (issue.audited != null && issue.audited) {
            analysisId = "list_node.issue_analysis_node.reviewed";
        }
        if (analysisId != null) {
            octaneIssue.set_analysis(createListNodeEntity(dtoFactory, analysisId));
        }

    }

    private void setOctaneStatus(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.scanStatus != null) {
            String listNodeId = "list_node.issue_state_node." + issue.scanStatus.toLowerCase();
            if (isLegalOctaneState(issue.scanStatus)) {
                octaneIssue.set_state(createListNodeEntity(dtoFactory, listNodeId));
            }
        }
        if(issue.removed != null && issue.removed){
            octaneIssue.set_state(createListNodeEntity(dtoFactory, "list_node.issue_state_node.closed"));
        }
    }

    private boolean isLegalOctaneState(String scanStatus) {
        List<String> legalNames = Arrays.asList("list_node.issue_state_node.new",
                "list_node.issue_state_node.existing",
                "list_node.issue_state_node.closed",
                "list_node.issue_state_node.reopen");
        return (legalNames.contains(scanStatus));
    }

    private Map getExtendedData(Issues.Issue issue) {
        Map retVal = new HashMap();
        retVal.put("issueName", issue.issueName);
        retVal.put("likelihood", issue.likelihood);
        retVal.put("kingdom", issue.kingdom);
        retVal.put("impact", issue.impact);
        retVal.put("confidence", issue.confidance);
        retVal.put("removedDate", issue.removedDate);
        return retVal;
    }

    private void setOctaneSeverity(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.severity != null) {
            String listNodeId = "list_node.severity." + issue.severity.toLowerCase();
            octaneIssue.set_severity(createListNodeEntity(dtoFactory, listNodeId));
        }
    }
    private static Entity createListNodeEntity(DTOFactory dtoFactory, String id) {
        return dtoFactory.newDTO(Entity.class).setType("list_node").setId(id);
    }



    private boolean isSSCProject(Run r) {
        if (!(r instanceof AbstractBuild)) {
            return false;
        }
        AbstractProject project = ((AbstractBuild) r).getProject();
        for (Object publisherO : project.getPublishersList()) {
            if (publisherO instanceof Publisher) {
                Publisher publisher = (Publisher) publisherO;
                publisher.getClass().getName().equals(
                        "com.fortify.plugin.jenkins.FPRPublisher");
                return true;
            }
        }
        return false;
    }
    private ProjectVersion getProjectVersion(Run r) {
        AbstractProject project = ((AbstractBuild) r).getProject();
        for (Object publisherO : project.getPublishersList()) {
            if (publisherO instanceof Publisher) {
                Publisher publisher = (Publisher) publisherO;
                publisher.getClass().getName().equals(
                        "com.fortify.plugin.jenkins.FPRPublisher");
                return getProjectNameByReflection(publisherO);
            }
        }
        return null;
    }
    private ProjectVersion getProjectNameByReflection(Object someObject) {
        String projectName = ConfigurationService.getFieldValue(someObject, "projectName").toString();
        String projectVersion = ConfigurationService.getFieldValue(someObject, "projectVersion").toString();
        if (projectName != null && projectVersion != null) {
            return new ProjectVersion(projectName, projectVersion);
        }
        return null;
    }
}
