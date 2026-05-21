package com.msme_pipeline_scheduler.solver;

import com.msme_pipeline_scheduler.model.AssignmentResult;
import com.msme_pipeline_scheduler.model.Instance;

public interface Scheduler {
    AssignmentResult solve(Instance instance, long timeBudgetMs);
}

