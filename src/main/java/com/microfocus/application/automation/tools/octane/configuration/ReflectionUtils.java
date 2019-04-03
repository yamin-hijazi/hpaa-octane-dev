package com.microfocus.application.automation.tools.octane.configuration;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Field;

public class ReflectionUtils {
    private static final Logger logger = LogManager.getLogger(ReflectionUtils.class);
    public static <T>  T getFieldValue(Object someObject, String fieldName) {
        for (Field field : someObject.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            if (field.getName().equals(fieldName)) {
                Object value = null;
                try {
                    value = field.get(someObject);
                } catch (IllegalAccessException e) {
                    logger.error("Failed to getFieldValue", e);
                }
                if (value != null) {
                    return (T)value;
                }
            }
        }
        return null;
    }
}
