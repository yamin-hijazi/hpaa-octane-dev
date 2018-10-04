package com.microfocus.application.automation.tools.octane.configuration;

import hudson.model.Descriptor;
import jenkins.model.Jenkins;

import java.lang.reflect.Field;

public class SSCServerConfigUtil {

    public static String getSSCServer() {
        Descriptor sscDescriptor = getSSCDescriptor();
        return getSSCServerFromDescriptor(sscDescriptor);
    }

    private static String getSSCServerFromDescriptor(Descriptor sscDescriptor) {
        Object urlObj = getFieldValue(sscDescriptor, "url");
        if(urlObj != null) {
            return urlObj.toString();
        }
        return null;
    }

    private static Object getFieldValue(Object someObject, String fieldName) {
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

    private static Descriptor getSSCDescriptor(){
        return Jenkins.getInstance().getDescriptorByName("com.fortify.plugin.jenkins.FPRPublisher");
    }

}
