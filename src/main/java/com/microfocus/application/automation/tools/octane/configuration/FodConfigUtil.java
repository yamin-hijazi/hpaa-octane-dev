package com.microfocus.application.automation.tools.octane.configuration;

import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;

import static com.microfocus.application.automation.tools.octane.configuration.ReflectionUtils.getFieldValue;

public class FodConfigUtil {

    public static class ServerConnectConfig {
        public String baseUrl;
        public String apiUrl;
        public String clientId;
        public String clientSecret;

    }
    public static ServerConnectConfig getFODServerConfig() {
        Descriptor fodDescriptor = getFODDescriptor();
        ServerConnectConfig serverConnectConfig = null;
        if(fodDescriptor != null){
            serverConnectConfig = new ServerConnectConfig();
            serverConnectConfig.apiUrl = getFieldValue(fodDescriptor, "apiUrl");
            serverConnectConfig.baseUrl = getFieldValue(fodDescriptor, "baseUrl");
            serverConnectConfig.clientId = getFieldValue(fodDescriptor, "clientId");
            serverConnectConfig.clientSecret = getFieldValue(fodDescriptor, "clientSecret");
        }
        return serverConnectConfig;
    }
    private static Descriptor getFODDescriptor() {
        return Jenkins.getInstance().getDescriptorByName("org.jenkinsci.plugins.fodupload.FodGlobalDescriptor");

    }

    public static Long getFODReleaseFromBuild(AbstractBuild build) {
        return build != null ? getRelease(build.getProject()) : null;
    }
    private static Long getRelease(AbstractProject project) {
        for (Object publisher : project.getPublishersList()) {
            if (publisher instanceof Publisher && "org.jenkinsci.plugins.fodupload.StaticAssessmentBuildStep".equals(publisher.getClass().getName())) {
                return getReleaseByReflection(publisher);
            }
        }
        return null;
    }

    private static Long getReleaseByReflection(Object fodPublisher) {

        Object modelObj = getFieldValue(fodPublisher, "model");
        if(modelObj == null){
            return null;
        }
        String bsiToken = getFieldValue(modelObj, "bsiTokenOriginal");
        return parseBSITokenAndGetReleaseId(bsiToken);
    }

    private static Long parseBSITokenAndGetReleaseId(String bsiToken) {
        return null;
    }
}
