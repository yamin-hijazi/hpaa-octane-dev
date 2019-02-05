/*
 * Certain versions of software and/or documents ("Material") accessible here may contain branding from
 * Hewlett-Packard Company (now HP Inc.) and Hewlett Packard Enterprise Company.  As of September 1, 2017,
 * the Material is now offered by Micro Focus, a separately owned and operated company.  Any reference to the HP
 * and Hewlett Packard Enterprise/HPE marks is historical in nature, and the HP and Hewlett Packard Enterprise/HPE
 * marks are the property of their respective owners.
 * __________________________________________________________________
 * MIT License
 *
 * (c) Copyright 2012-2019 Micro Focus or one of its affiliates.
 *
 * The only warranties for products and services of Micro Focus and its affiliates
 * and licensors ("Micro Focus") are set forth in the express warranty statements
 * accompanying such products and services. Nothing herein should be construed as
 * constituting an additional warranty. Micro Focus shall not be liable for technical
 * or editorial errors or omissions contained herein.
 * The information contained herein is subject to change without notice.
 * ___________________________________________________________________
 */

package com.microfocus.application.automation.tools.results.projectparser.performance;

import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;


/**
 * Holds information on the SLA's of one scenario (per job / run / build)
 */
public class JobLrScenarioResult extends LrScenario {
    public static final int DEFAULT_CONNECTION_MAX = -1;
    public static final int DEFAULT_SCENARIO_DURATION = -1;
    public ArrayList<GoalResult> scenarioSlaResults;
    public Map<String, Integer> vUserSum;
    public TreeMap<String, Integer> transactionSum;
    public TreeMap<String, TreeMap<String, Integer>> transactionData;
    private int connectionMax;
    private long scenarioDuration;

    public JobLrScenarioResult(String scenarioName) {
        super.setScenrioName(scenarioName);
        connectionMax = DEFAULT_CONNECTION_MAX;
        scenarioDuration = DEFAULT_SCENARIO_DURATION;
        vUserSum = new TreeMap<String, Integer>();
        transactionSum = new TreeMap<String, Integer>();
        transactionData = new TreeMap<>();
        scenarioSlaResults = new ArrayList<GoalResult>(0);
    }

    public long getScenarioDuration() {
        return scenarioDuration;
    }

    public void setScenarioDuration(long scenarioDuration) {
        this.scenarioDuration = scenarioDuration;
    }

    public int getConnectionMax() {
        return connectionMax;
    }

    public void setConnectionMax(int connectionMax) {
        this.connectionMax = connectionMax;
    }
}
