package com.hpe.application.automation.tools.ssc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.hpe.application.automation.tools.octane.events.SSCFortifyConfigurations;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.HttpClientUtils;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

public class SSCClientManager {


    private static SSCClientManager _instance;

    private static Object lockObj = new Object();
    private AuthToken.AuthTokenData authToken;
    private CloseableHttpClient httpClient;

    public static SSCClientManager instance(){
        if(_instance == null){
            synchronized (lockObj){
                _instance = new SSCClientManager();
                _instance.initHttpClient();
            }
        }
        return _instance;
    }
    String getToken(SSCFortifyConfigurations sscCfgs){
        boolean generateToken = false;
        if(authToken == null) {
            generateToken = true;
        }else {
            //Calendar.getInstance
            Date tenMinsFromNow = new Date(System.currentTimeMillis() + 10 * 60 * 1000);
            Date expired = SSCDateUtils.getDateFromDateString(authToken.terminalDate, SSCDateUtils.sscFormat);
            if (expired.before(tenMinsFromNow)) {
                generateToken = true;
            }
        }
        if(generateToken){
            authToken = null;
            synchronized(lockObj) {
                if(authToken == null) {
                    sendReqAuth(sscCfgs);
                }
            }
        }
        return this.authToken.token;
    }
    private void sendReqAuth(SSCFortifyConfigurations sscCfgs) {
        //"/{SSC Server Context}/api/v1"
        //String url = "http://" + serverURL + "/ssc/api/v1/projects?q=id:2743&fulltextsearch=true";
        String url = sscCfgs.serverURL + "/api/v1/tokens";
        HttpPost request = new HttpPost(url);
        request.addHeader("Authorization", sscCfgs.baseToken);
        request.addHeader("Accept", "application/json");
        request.addHeader("Host", getNetHost(sscCfgs.serverURL));
        request.addHeader("Content-Type", "application/json;charset=UTF-8");

        String body = "{\"type\": \"UnifiedLoginToken\"}";
        CloseableHttpResponse response = null;
        try {
            HttpEntity entity = new ByteArrayEntity(body.getBytes("UTF-8"));
            request.setEntity(entity);
            response = httpClient.execute(request);
            if (succeeded(response.getStatusLine().getStatusCode())) {

                String toString = isToString(response.getEntity().getContent());
                com.hpe.application.automation.tools.ssc.AuthToken authToken = new ObjectMapper().readValue(toString,
                        TypeFactory.defaultInstance().constructType(com.hpe.application.automation.tools.ssc.AuthToken.class));
                this.authToken = authToken.data;
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
        } finally {
            if (response != null) {
                EntityUtils.consumeQuietly(response.getEntity());
                HttpClientUtils.closeQuietly(response);
            }
        }
    }
    private void initHttpClient() {

        String proxyHost = System.getProperty("http.proxyHost");//"proxy.il.hpecorp.net";
        String proxyPort = System.getProperty("http.proxyPort");
        Integer proxyPortNumber = proxyPort != null ? Integer.valueOf(proxyPort) : null;//8080;


        if (proxyHost != null && !proxyHost.isEmpty() && proxyPortNumber != null) {
            HttpClientBuilder clientBuilder = HttpClients.custom();
            clientBuilder.setProxy(new HttpHost(proxyHost, proxyPortNumber));
            httpClient = clientBuilder.build();
        } else {
            httpClient = HttpClients.createDefault();
        }
    }
    public static String getNetHost(String serverURL) {
        //http://myd-vma00564.swinfra.net:8180/ssc
        String prefix = "http://";
        int indexOfStart = serverURL.toLowerCase().indexOf(prefix) + prefix.length();
        int indexOfEnd = serverURL.lastIndexOf("/");
        if (indexOfEnd < 0) {
            return serverURL.substring(indexOfStart);
        }
        return serverURL.substring(indexOfStart, indexOfEnd);
    }

    public static boolean succeeded(int statusCode) {
        return statusCode == 200 || statusCode == 201;
    }
    static String isToString(InputStream is) throws IOException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = is.read(buffer)) != -1) {
            result.write(buffer, 0, length);
        }
        return result.toString();
    }

    public SscProjectConnector getProjectConnector(SSCFortifyConfigurations sscFortifyConfigurations) {
        return new SscProjectConnector(sscFortifyConfigurations,this.httpClient);
    }

}
