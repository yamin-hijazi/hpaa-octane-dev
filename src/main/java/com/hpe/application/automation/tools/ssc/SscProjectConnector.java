package com.hpe.application.automation.tools.ssc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hpe.application.automation.tools.octane.events.SSCFortifyConfigurations;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static com.hpe.application.automation.tools.ssc.SSCClientManager.*;

/**
 * Created by hijaziy on 7/12/2018.
 */
public class SscProjectConnector {

    private SSCFortifyConfigurations sscFortifyConfigurations;
    private CloseableHttpClient httpClient;

    SscProjectConnector(SSCFortifyConfigurations sscFortifyConfigurations,
                                CloseableHttpClient httpClient){
        this.sscFortifyConfigurations = sscFortifyConfigurations;
        this.httpClient = httpClient;
    }
    private String sendGetEntity(String urlSuffix) {
        String url = sscFortifyConfigurations.serverURL + "/api/v1/" + urlSuffix;
        return sendGetRequest(url);
    }

    private String sendGetRequest(String url) {

        HttpGet request = new HttpGet(url);
        request.addHeader("Authorization", "FortifyToken " +
                SSCClientManager.instance().getToken(sscFortifyConfigurations,false));
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscFortifyConfigurations.serverURL));

        CloseableHttpResponse response = null;
        try {
            response = httpClient.execute(request);
            //401. Access..
            if(!succeeded(response.getStatusLine().getStatusCode())){
                request.removeHeaders("Authorization");
                request.addHeader("Authorization", "FortifyToken " +
                        SSCClientManager.instance().getToken(sscFortifyConfigurations,false));
                response = httpClient.execute(request);
            }
            String toString = isToString(response.getEntity().getContent());
            return toString;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {

        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                HttpClientUtils.closeQuietly(response);
            }
        }
        return null;
    }

    public ProjectVersions.ProjectVersion getProjectVersion() {
        Integer projectId = getProjectId();
        if (projectId == null) {
            return null;
        }
        String suffix = null;
        try {
            suffix = "projects/" + projectId + "/versions?q=name:" + URLEncoder.encode(this.sscFortifyConfigurations.projectVersion, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            //todo: add logger message here
            return null;
        }
        String rawResponse = sendGetEntity(suffix);
        ProjectVersions projectVersions = responseToObject(rawResponse, ProjectVersions.class);
        if (projectVersions.data.length == 0) {
            return null;
        }
        return projectVersions.data[0];
    }

    public Integer getProjectId() {
        String rawResponse = null;
        try {
            rawResponse = sendGetEntity("projects?q=name:" + URLEncoder.encode(this.sscFortifyConfigurations.projectName, "UTF-8"));
        } catch (UnsupportedEncodingException e) {
            //todo: add logger message here
            return null;
        }
        Projects projects = responseToObject(rawResponse, Projects.class);
        if (projects.data.length == 0) {
            return null;
        }
        return projects.data[0].id;
    }
    public <T> T responseToObject(String response, Class<T> type) {
        if (response == null) {
            return null;
        }
        try {
            return new ObjectMapper().readValue(response,
                    TypeFactory.defaultInstance().constructType(type));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    public Issues readNewIssuesOfLastestScan(int projectVersionId) {
        String urlSuffix = String.format("projectVersions/%d/issues?issue_age:new", projectVersionId);
        String rawResponse = sendGetEntity(urlSuffix);
        return responseToObject(rawResponse, Issues.class);
    }

    public Artifacts getArtifactsOfProjectVersion(Integer id, int limit) {

        String urlSuffix = String.format("projectVersions/%d/artifacts?limit=%d", id, limit);
        String rawResponse = sendGetEntity(urlSuffix);
        return responseToObject(rawResponse, Artifacts.class);
    }
}
