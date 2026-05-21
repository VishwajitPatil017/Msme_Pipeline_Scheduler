package com.msme_pipeline_scheduler.util;

import com.msme_pipeline_scheduler.model.Instance;
import com.msme_pipeline_scheduler.model.Task;

import java.util.Map;

/**
 * Computes the penalty for an assignment. Implements P_base + a small load-balance term.
 */
public class PenaltyCalculator {

    private static final double ALPHA_BALANCE = 0.05;

    public static double computePenalty(Instance inst, Map<String,Integer> assignmentOneBased) {
        // base penalty: sum_t w(t) * slot_index (1-based)
        double p = 0.0;
        int K = inst.K;
        double[] cpuUtil = new double[K];
        for (Task t: inst.tasks) {
            Integer s1 = assignmentOneBased.get(t.id);
            if (s1 == null) continue;
            int s = s1 - 1;
            p += t.weight * (s+1);
            cpuUtil[s] += t.resources[0];
        }

        // balance penalty: variance of CPU utilization across slots
        double mean=0; for (double v:cpuUtil) mean+=v; mean /= K;
        double var=0; for (double v:cpuUtil) var += (v-mean)*(v-mean); var /= K;
        p += ALPHA_BALANCE * var;
        return p;
    }
}

