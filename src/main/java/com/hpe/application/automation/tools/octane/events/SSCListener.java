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

import java.util.*;

/**
 * Created by hijaziy on 7/23/2018.
 */
public class SSCListener {

    public static String SEVERITY_LG_NAME_LOW = "list_node.severity.low";
    public static String SEVERITY_LG_NAME_MEDIUM = "list_node.severity.medium";
    public static String SEVERITY_LG_NAME_HIGH = "list_node.severity.high";
    public static String SEVERITY_LG_NAME_CRITICAL = "list_node.severity.urgent";
    public static String EXTERNAL_TOOL_NAME =  "external tool";

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
        List<OctaneIssue> octaneIssues = createOctaneIssues(issues);
        IssuesFileSerializer.serialize(octaneIssues);

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

    private List<OctaneIssue> createOctaneIssues(Issues issues) {
        if(issues == null){
            return new ArrayList<>();
        }
        DTOFactory dtoFactory = DTOFactory.getInstance();
        List<OctaneIssue> octaneIssues = new ArrayList<>();
        for (Issues.Issue issue : issues.data) {
            OctaneIssue octaneIssue = dtoFactory.newDTO(OctaneIssue.class);
            setOctaneAnalysis(dtoFactory, issue, octaneIssue);
            setOctaneSeverity(dtoFactory, issue, octaneIssue);
            setOctaneStatus(dtoFactory, issue, octaneIssue);
            Map extendedData = getExtendedData(issue);
            octaneIssue.setExtended_data(extendedData);
            octaneIssue.setPrimary_location_full(issue.primaryLocation);
            octaneIssue.setLine(issue.lineNumber);
            octaneIssue.setRemote_id(issue.issueInstanceId);
            octaneIssue.setIntroduced_date(issue.foundDate);
            octaneIssue.setExternal_link(issue.hRef);
            octaneIssue.setTool_name(EXTERNAL_TOOL_NAME);
            octaneIssues.add(octaneIssue);
        }

        return octaneIssues;
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
            octaneIssue.setAnalysis(createListNodeEntity(dtoFactory, analysisId));
        }

    }

    private void setOctaneStatus(DTOFactory dtoFactory, Issues.Issue issue, OctaneIssue octaneIssue) {
        if (issue.scanStatus != null) {
            String listNodeId = "list_node.issue_state_node." + issue.scanStatus.toLowerCase();
            if (isLegalOctaneState(listNodeId)) {
                octaneIssue.setState(createListNodeEntity(dtoFactory, listNodeId));
            }
        }
        if(issue.removed != null && issue.removed){
            octaneIssue.setState(createListNodeEntity(dtoFactory, "list_node.issue_state_node.closed"));
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
            String octaneSeverity = getOctaneSeverityFromSSCValue(issue.severity);
            octaneIssue.setSeverity(createListNodeEntity(dtoFactory, octaneSeverity));
        }
    }
    private String getOctaneSeverityFromSSCValue(String severity) {
        if (severity == null) {
            return null;
        }
        String logicalNameForSeverity = null;
        if (severity.startsWith("4")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_CRITICAL;
        }
        if (severity.startsWith("3")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_HIGH;
        }
        if (severity.startsWith("2")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_MEDIUM;
        }
        if (severity.startsWith("1")) {
            logicalNameForSeverity = SEVERITY_LG_NAME_LOW;
        }

        return logicalNameForSeverity;

    }
    private static Entity createListNodeEntity(DTOFactory dtoFactory, String id) {
        if(id == null){
            return null;
        }
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
