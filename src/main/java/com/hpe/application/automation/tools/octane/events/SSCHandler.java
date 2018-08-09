package com.hpe.application.automation.tools.octane.events;

import com.hp.octane.integrations.dto.DTOFactory;
import com.hp.octane.integrations.dto.entities.Entity;
import com.hpe.application.automation.tools.ssc.Issues;
import com.hpe.application.automation.tools.ssc.ProjectVersions;
import com.hpe.application.automation.tools.ssc.SscConnector;
import com.microfocus.application.automation.tools.octane.tests.build.BuildHandlerUtils;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.model.Run;
import hudson.tasks.Publisher;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by hijaziy on 7/23/2018.
 */
public class SSCHandler {

    private SSCFortifyConfigurations sscFortifyConfigurations = new SSCFortifyConfigurations();
    private SscConnector sscConnector ;
    private ProjectVersions.ProjectVersion projectVersion;
    private Run run;
    public static final String SCAN_RESULT_FILE = "securityScan.json";

    public boolean getScanFinishStatus() {
        //need to check if there is scan that started after run started, if not return false
        //if yes - fetch the status and return true/false accordingly
        return true;
    }

    static class ProjectVersion {
        public String project;
        public String version;

        public ProjectVersion(String projectName, String projectVersion) {
            this.project = projectName;
            this.version = projectVersion;
        }
    }

    public SSCHandler(Run r) {
        run = r;
        ProjectVersion project = getProjectVersion();
        String sscServer = getSSCServer();

        //"Basic QWRtaW46ZGV2c2Vjb3Bz"
        sscFortifyConfigurations.baseToken = "Basic QWRtaW46ZGV2c2Vjb3BzMQ==";
        sscFortifyConfigurations.projectName = project.project;
        sscFortifyConfigurations.projectVersion = project.version;
        sscFortifyConfigurations.serverURL = sscServer;

        sscConnector = new SscConnector(sscFortifyConfigurations);
        if(sscConnector!=null) {
            projectVersion = sscConnector.getProjectVersion();
        }

    }

    public Issues getLatestScan() {

//        saveReport();

        String buildCiId = BuildHandlerUtils.getBuildCiId(run);
        System.out.println("getLatestScan of : "+buildCiId);
        if(buildCiId.equals("54")){
            saveReport();
        }
        return sscConnector.readIssuesOfLastestScan(projectVersion);
//        return null;


    }


    private String getSSCServer() {
        Descriptor sscDescriptor = getSSCDescriptor(run);
        return getServerFromDescriptor(sscDescriptor);
    }

    private String getServerFromDescriptor(Descriptor sscDescriptor) {
        Object urlObj = getFieldValue(sscDescriptor, "url");
        if(urlObj != null) {
            return urlObj.toString();
        }
        return null;
    }
    private Object getFieldValue(Object someObject, String fieldName) {
        for (Field field : someObject.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if(field.getName().equals(fieldName)) {
                Object value = null;
                try {
                    value = field.get(someObject);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                if (value != null) {
                    return value;
                }
            }
        }
        return null;
    }

    private Descriptor getSSCDescriptor(Run r){
        final List<Publisher> publishers = ((AbstractBuild) r).getProject().getPublishersList().toList();
        for (Publisher publisher : publishers) {
            if ("com.fortify.plugin.jenkins.FPRPublisher".equals(publisher.getClass().getName())) {
                return publisher.getDescriptor();
            }
        }
        return null;
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
    private ProjectVersion getProjectVersion() {
        AbstractProject project = ((AbstractBuild) run).getProject();
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
        String projectName = getFieldValue(someObject, "projectName").toString();
        String projectVersion = getFieldValue(someObject, "projectVersion").toString();
        if (projectName != null && projectVersion != null) {
            return new ProjectVersion(projectName, projectVersion);
        }
        return null;
    }

    private void saveReport() {

        String vulnerabilitiesScanFilePath = new File(run.getRootDir(), SCAN_RESULT_FILE).getPath();
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
        System.out.println(timeStamp+" : working on : "+vulnerabilitiesScanFilePath);
        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        String yamin = getYaminVul();
        try (PrintWriter out = new PrintWriter(vulnerabilitiesScanFilePath)) {
//            out.write(yamin);
			out.write(getVulString());

            out.flush();
            out.close();
        } catch (com.hp.mqm.client.exception.FileNotFoundException e) {
            e.printStackTrace();
        } catch (java.io.FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private String getYaminVul() {
        return "{\n" +
                "\t\"data\": [{\n" +
                "\t\t\"primary_location_full\": \"hellow.java\",\n" +
                "\t\t\"line\": 5,\n" +
                "\t\t\"analysis\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_analysis_node.reviewed\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"state\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_state_node.new\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"severity\": {\n" +
                "\t\t\t\"id\": \"list_node.severity.medium\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"remote_id\": \"98F2CC18089300065BC94822FC4AD02B\",\n" +
                "\t\t\"introduced_date\": \"2017-02-12T12:31:44.000+0000\",\n" +
                "\t\t\"external_link\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/8/issues/2743\",\n" +
                "\t\t\"extended_data\": {\n" +
                "\t\t\t\"issueName\": \"J2EE Bad Practices: Leftover Debug Code\",\n" +
                "\t\t\t\"likelihood\": \"0.8\",\n" +
                "\t\t\t\"impact\": \"2.0\",\n" +
                "\t\t\t\"confidence\": \"5.0\",\n" +
                "\t\t\t\"kingdom\": \"Encapsulation\",\n" +
                "\t\t\t\"removedDate\": null\n" +
                "\t\t},\n" +
                "\t\t\"tool_name\": \"external tool\"\n" +
                "\t},\n" +
                "\t{\n" +
                "\t\t\"primary_location_full\": \"hellow.java\",\n" +
                "\t\t\"line\": 6,\n" +
                "\t\t\"analysis\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_analysis_node.reviewed\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"state\": {\n" +
                "\t\t\t\"id\": \"list_node.issue_state_node.new\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"severity\": {\n" +
                "\t\t\t\"id\": \"list_node.severity.medium\",\n" +
                "\t\t\t\"type\": \"list_node\"\n" +
                "\t\t},\n" +
                "\t\t\"remote_id\": \"9793D70459978170AD8DF473412298C2\",\n" +
                "\t\t\"introduced_date\": \"2017-02-12T12:31:44.000+0000\",\n" +
                "\t\t\"external_link\": \"http://myd-vma00564.swinfra.net:8180/ssc/api/v1/projectVersions/8/issues/2742\",\n" +
                "\t\t\"extended_data\": {\n" +
                "\t\t\t\"issueName\": \"Poor Logging Practice: Use of a System Output Stream\",\n" +
                "\t\t\t\"likelihood\": \"1.0\",\n" +
                "\t\t\t\"impact\": \"1.0\",\n" +
                "\t\t\t\"confidence\": \"5.0\",\n" +
                "\t\t\t\"kingdom\": \"Encapsulation\",\n" +
                "\t\t\t\"removedDate\": null\n" +
                "\t\t},\n" +
                "\t\t\"tool_name\": \"external tool\"\n" +
                "\t}]\n" +
                "}";
    }

    private String getVulString() {
        return "{\n" +
                "  \"data\": [\n" +
                "    {\n" +
                "      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.low\"      },\n" +
                "      \"package\": \"hp.com\",\n" +
                "      \"line\": 10,\n" +
                "      \"remote_id\": \"10341\",\n" +
                "      \"primary_location_full\": null,\n" +
                "      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
                "      \"owner_email\":\"daniel.shmaya@hpe.com\",\n" +
                "      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.closed\"      },\n" +
                "      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
                "      \"tool_name\": \"external tool\",\n" +
                "      \"external_link\":\"some url here\",\n" +
                "       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.maybe_an_issue\"      },\n" +
                "       \"extended_data\" : {\"key\":\"value\",\"key\":\"value\"},\n" +
                "      \"category\": \"category\"\n" +
                "    }, {\n" +
                "      \"severity\": {        \"type\": \"list_node\",        \"id\": \"list_node.severity.high\"      },\n" +
                "      \"package\": \"hp.com.com\",\n" +
                "      \"line\": 11,\n" +
                "      \"remote_id\": \"10321\",\n" +
                "      \"primary_location_full\": \"entities-factory.html\",\n" +
                "      \"introduced_date\": \"2018-06-03T14:06:58Z\",\n" +
                "      \"owner_email\":\"sa@nga\",\n" +
                "      \"state\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_state_node.new\"      },\n" +
                "      \"tool_type\": {        \"type\": \"list_node\",        \"id\": \"list_node.securityTool.fod\"      },\n" +
                "      \"tool_name\": \"external too 2\",\n" +
                "      \"external_link\":\"some url here 2\",\n" +
                "       \"analysis\": {        \"type\": \"list_node\",        \"id\": \"list_node.issue_analysis_node.reviewed\"      },\n" +
                "       \"extended_data\" : {\"key1\":\"value1\",\"key2\":\"value2\"},\n" +
                "      \"category\": \"category 2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }
}
