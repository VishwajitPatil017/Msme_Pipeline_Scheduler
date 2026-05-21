package com.msme_pipeline_scheduler.solver;

import com.msme_pipeline_scheduler.model.AssignmentResult;
import com.msme_pipeline_scheduler.model.Instance;
import com.msme_pipeline_scheduler.model.Task;
import com.msme_pipeline_scheduler.util.PenaltyCalculator;
import com.msme_pipeline_scheduler.util.Validator;

import java.util.*;

/**
 * A simple priority-weighted resource-aware greedy scheduler with a small local repacking attempt.
 */
public class ResourceAwareGreedyScheduler implements Scheduler {

    private final double alphaBalance = 0.1; // balance penalty weight (small)

    @Override
    public AssignmentResult solve(Instance instance, long timeBudgetMs) {
        long start = System.nanoTime();
        AssignmentResult res = new AssignmentResult();
        int n = instance.tasks.size();
        int K = instance.K;

        // initialize slot usages
        double[][] slotUsage = new double[K][4];
        List<Set<Integer>> slotTasks = new ArrayList<>();
        for (int s = 0; s < K; s++) slotTasks.add(new HashSet<>());

        // ordering: descending weight * resource norm, tie-breaker smaller window width, higher degree
        List<Task> order = new ArrayList<>(instance.tasks);
        order.sort((a,b) -> {
            double na = a.weight * resourceNorm(a.resources);
            double nb = b.weight * resourceNorm(b.resources);
            if (na != nb) return Double.compare(nb, na);
            int wa = a.windowHi - a.windowLo;
            int wb = b.windowHi - b.windowLo;
            if (wa != wb) return Integer.compare(wa, wb);
            return Integer.compare(b.conflicts.size(), a.conflicts.size());
        });

        int[] assign = new int[n];
        Arrays.fill(assign, -1);
        List<Task> unplaced = new ArrayList<>();

        for (Task t : order) {
            List<Integer> candidates = new ArrayList<>();
            for (int s = t.windowLo; s <= t.windowHi && s < K; s++) candidates.add(s);
            double bestScore = Double.POSITIVE_INFINITY;
            int bestSlot = -1;
            for (int s : candidates) {
                // conflict check
                boolean conflict = false;
                for (int otherIdx : slotTasks.get(s)) if (t.conflicts.contains(otherIdx)) { conflict = true; break; }
                if (conflict) continue;
                // capacity check
                boolean ok = true;
                for (int d = 0; d < 4; d++) {
                    if (slotUsage[s][d] + t.resources[d] > instance.capacities.get(s)[d] + 1e-9) { ok = false; break; }
                }
                if (!ok) continue;

                // marginal penalty estimate: base w*slot + small balance penalty on CPU
                double avgCpu = averageCpu(slotUsage) ;
                double newCpu = slotUsage[s][0] + t.resources[0];
                double balanceDelta = (newCpu - avgCpu)*(newCpu - avgCpu);
                double score = t.weight * (s+1) + alphaBalance * balanceDelta; // slot indices are 1-based in penalty
                if (score < bestScore) { bestScore = score; bestSlot = s; }
            }
            if (bestSlot >= 0) {
                assign[t.index] = bestSlot;
                slotTasks.get(bestSlot).add(t.index);
                for (int d=0; d<4; d++) slotUsage[bestSlot][d] += t.resources[d];
            } else {
                unplaced.add(t);
            }
        }

        // try simple repacking for unplaced tasks: try to evict a single low-weight task
        for (Task t : new ArrayList<>(unplaced)) {
            boolean placed = false;
            for (int s = t.windowLo; s <= t.windowHi && s < K && !placed; s++) {
                // attempt to free capacity by evicting one task from slot s
                for (int evictIdx : new HashSet<>(slotTasks.get(s))) {
                    Task ev = instance.tasks.get(evictIdx);
                    // check if evicted task can go to another slot
                    for (int alt = ev.windowLo; alt <= ev.windowHi && !placed; alt++) {
                        if (alt == s) continue;
                        if (!slotTasks.get(alt).stream().anyMatch(idx -> ev.conflicts.contains(idx))) {
                            boolean ok = true;
                            for (int d=0; d<4; d++) {
                                if (slotUsage[alt][d] + ev.resources[d] > instance.capacities.get(alt)[d] + 1e-9) { ok = false; break; }
                            }
                            if (ok) {
                                // move ev to alt, free space in s and place t
                                slotTasks.get(s).remove(ev.index);
                                for (int d=0; d<4; d++) { slotUsage[s][d] -= ev.resources[d]; }
                                slotTasks.get(alt).add(ev.index);
                                for (int d=0; d<4; d++) { slotUsage[alt][d] += ev.resources[d]; }
                                // try place t in s
                                boolean fits = true;
                                for (int d=0; d<4; d++) if (slotUsage[s][d] + t.resources[d] > instance.capacities.get(s)[d] + 1e-9) { fits = false; break; }
                                if (fits) {
                                    slotTasks.get(s).add(t.index);
                                    for (int d=0; d<4; d++) slotUsage[s][d] += t.resources[d];
                                    assign[t.index] = s;
                                    placed = true;
                                    unplaced.remove(t);
                                } else {
                                    // rollback move
                                    slotTasks.get(alt).remove(ev.index);
                                    for (int d=0; d<4; d++) { slotUsage[alt][d] -= ev.resources[d]; }
                                    slotTasks.get(s).add(ev.index);
                                    for (int d=0; d<4; d++) { slotUsage[s][d] += ev.resources[d]; }
                                }
                            }
                        }
                    }
                    if (placed) break;
                }
            }
        }

        // prepare result
        Map<String,Integer> asg = new HashMap<>();
        for (Task t : instance.tasks) {
            if (assign[t.index] >= 0) asg.put(t.id, assign[t.index]+1); // return 1-based slots
        }
        res.assignment = asg;
        long end = System.nanoTime();
        res.runtimeMs = (end - start) / 1_000_000;

        // validate
        Validator.ValidationReport report = Validator.validateAssignment(instance, res.assignment);
        res.feasible = report.feasible;
        res.violationReason = report.reason;

        // compute penalty if feasible
        if (res.feasible) {
            res.penalty = PenaltyCalculator.computePenalty(instance, res.assignment);
        } else {
            res.penalty = Double.POSITIVE_INFINITY;
        }

        return res;
    }

    private static double resourceNorm(double[] r) {
        double s=0; for (double v:r) s+=v*v; return Math.sqrt(s);
    }

    private static double averageCpu(double[][] slotUsage) {
        double s=0; for (double[] u:slotUsage) s+=u[0]; return s/slotUsage.length;
    }
}

