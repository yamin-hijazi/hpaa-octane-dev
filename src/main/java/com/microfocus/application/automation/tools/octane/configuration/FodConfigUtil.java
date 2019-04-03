package com.microfocus.application.automation.tools.octane.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.Descriptor;
import hudson.tasks.Publisher;
import jenkins.model.Jenkins;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.StringUtils;
import java.io.IOException;
import java.util.Map;

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
        try {
            return handleURLFormat(bsiToken);
        }catch (Exception e){
            return handleBase64Format(bsiToken);
        }
    }

    private static Long handleBase64Format(String bsiToken)  {

        String bsi64 = StringUtils.newStringUtf8(Base64.decodeBase64(bsiToken));
        try {
            Map bsiJsonAsMap = new ObjectMapper().readValue(bsi64,
                    TypeFactory.defaultInstance().constructType(Map.class));
            return Long.valueOf(bsiJsonAsMap.get("releaseId").toString());

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

    }

    private static Long handleURLFormat(String bsiToken) {
        //https://api.sandbox.fortify.com/bsi2.aspx?tid=159&tc=Octane&pv=3059&payloadType=ANALYSIS_PAYLOAD&astid=25&ts=JS%2fXML%2fHTML
        if(bsiToken == null){
            return null;
        }
        String releaseString = bsiToken.substring(bsiToken.indexOf("=",bsiToken.indexOf("pv"))+1,
                bsiToken.indexOf("&",
                        bsiToken.indexOf("pv")));
        return Long.valueOf(releaseString);
    }
}
