package com.msme_pipeline_scheduler.model;

import java.util.HashMap;
import java.util.Map;

public class AssignmentResult {
    public Map<String, Integer> assignment = new HashMap<>();
    public double penalty = Double.POSITIVE_INFINITY;
    public long runtimeMs = 0L;
    public boolean feasible = false;
    public String violationReason = "";
}

