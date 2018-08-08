package com.hpe.application.automation.tools.octane.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hp.octane.integrations.dto.SecurityScans.OctaneIssue;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IssuesFileSerializer {

    public static void serialize(List<OctaneIssue> octaneIssues) {
        try{
            Map dataFormat = new HashMap<>();
            dataFormat.put("data",octaneIssues);
            PrintWriter fw = new PrintWriter("the-file-name.txt", "UTF-8");
            new ObjectMapper().writeValue(fw,dataFormat);
            fw.flush();
            fw.close();
        }catch(Exception e){
            System.out.println(e);
        }

    }
}
