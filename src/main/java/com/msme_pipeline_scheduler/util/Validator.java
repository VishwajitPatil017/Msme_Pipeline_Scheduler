package com.msme_pipeline_scheduler.util;

import com.msme_pipeline_scheduler.model.Instance;
import com.msme_pipeline_scheduler.model.Task;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Validator {

    public static class ValidationReport { public boolean feasible; public String reason; }

    public static ValidationReport validateAssignment(Instance inst, Map<String,Integer> assignmentOneBased) {
        ValidationReport r = new ValidationReport();
        int K = inst.K;
        Map<Integer,String> idByIndex = new HashMap<>();
        for (Task t: inst.tasks) idByIndex.put(t.index, t.id);

        // convert to slot->set of indices
        Map<Integer, Set<Integer>> slotToTasks = new HashMap<>();
        for (Task t: inst.tasks) {
            Integer s1 = assignmentOneBased.get(t.id);
            if (s1 == null) continue;
            int s = s1 - 1;
            if (s < 0 || s >= K) { r.feasible=false; r.reason = "Task " + t.id + " assigned to invalid slot " + s1; return r; }
            slotToTasks.computeIfAbsent(s, k->new HashSet<>()).add(t.index);
        }

        // F1: conflicts
        for (var e : slotToTasks.entrySet()) {
            int s = e.getKey();
            var set = e.getValue();
            for (int a : set) {
                Task ta = inst.tasks.get(a);
                for (int b : set) {
                    if (a==b) continue;
                    if (ta.conflicts.contains(b)) {
                        r.feasible = false;
                        r.reason = "Conflict in slot " + (s+1) + " between " + ta.id + " and " + idByIndex.get(b);
                        return r;
                    }
                }
            }
        }

        // F2: capacity
        for (var e : slotToTasks.entrySet()) {
            int s = e.getKey();
            double[] used = new double[4];
            for (int idx : e.getValue()) {
                double[] rr = inst.tasks.get(idx).resources;
                for (int d=0; d<4; d++) used[d] += rr[d];
            }
            double[] cap = inst.capacities.get(s);
            for (int d=0; d<4; d++) {
                if (used[d] > cap[d] + 1e-9) {
                    r.feasible = false;
                    r.reason = String.format("Resource dim %d overload in slot %d: used=%.3f cap=%.3f", d, s+1, used[d], cap[d]);
                    return r;
                }
            }
        }

        // F3: SLA windows
        for (Task t: inst.tasks) {
            Integer s1 = assignmentOneBased.get(t.id);
            if (s1 == null) continue; // unassigned allowed to be reported as infeasible elsewhere
            int s = s1 -1;
            if (s < t.windowLo || s > t.windowHi) {
                r.feasible = false;
                r.reason = "Task " + t.id + " assigned outside SLA window ["+t.windowLo+","+t.windowHi+"] -> " + (s+1);
                return r;
            }
        }

        r.feasible = true; r.reason = ""; return r;
    }
}

