package com.hpe.application.automation.tools.ssc;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SSCDateUtils {
    //"2017-02-12T12:31:44.000+0000"
    public static final String sscFormat =  "yyyy-MM-dd'T'hh:mm:ss";
    public static final String octaneFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    public static String convertDateSSCToOctane(String inputFoundDate) {
        if (inputFoundDate == null) {
            return null;
        }
        Date date = getDateFromDateString(inputFoundDate,sscFormat);
        //"2018-06-03T14:06:58Z"
        SimpleDateFormat targetDateFormat = new SimpleDateFormat(octaneFormat);
        return targetDateFormat.format(date);
    }
    public static Date getDateFromDateString(String inputDate,String format) {
        try {
            DateFormat sourceDateFormat = new SimpleDateFormat(format);
            return sourceDateFormat.parse(inputDate);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}
