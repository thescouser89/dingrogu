package org.jboss.pnc.dingrogu.api;


public class Endpoints {

    private static final String START = "/start";
    private static final String CANCEL = "/cancel";
    private static final String CALLBACK = "/callback";

    private static final String ADAPTER_ENDPOINT = "/adapter";
    private static final String WORKFLOW_ENDPOINT = "/workflow";

    private static final String ADAPTER_DELIVERABLES_ANALYZER_ANALYZE =  ADAPTER_ENDPOINT + "/deliverables-analyzer/analyze";
    public static final String ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_START =  ADAPTER_DELIVERABLES_ANALYZER_ANALYZE + START;
    public static final String ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_CANCEL =  ADAPTER_DELIVERABLES_ANALYZER_ANALYZE + CANCEL;
    public static final String ADAPTER_DELIVERABLES_ANALYZER_ANALYZE_CALLBACK =  ADAPTER_DELIVERABLES_ANALYZER_ANALYZE + CALLBACK;

    public static final String WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE = WORKFLOW_ENDPOINT + "/deliverable-analyzer";
    public static final String WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE_START = WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE + START;
    public static final String WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE_CANCEL = WORKFLOW_DELIVERABLES_ANALYZER_ANALYZE + CANCEL;
}
