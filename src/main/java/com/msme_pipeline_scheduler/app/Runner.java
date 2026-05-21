package com.msme_pipeline_scheduler.app;

import com.msme_pipeline_scheduler.io.JsonIO;
import com.msme_pipeline_scheduler.model.AssignmentResult;
import com.msme_pipeline_scheduler.model.Instance;
import com.msme_pipeline_scheduler.solver.ResourceAwareGreedyScheduler;

import java.io.File;

/**
 * Simple CLI runner: --input <file> --output <file> [--timeBudgetMs <ms>]
 */
public class Runner {
    public static void main(String[] args) throws Exception {
        String inPath = null; String outPath = null; long budget = 5000;
        for (int i=0;i<args.length;i++) {
            switch (args[i]) {
                case "--input": inPath = args[++i]; break;
                case "--output": outPath = args[++i]; break;
                case "--timeBudgetMs": budget = Long.parseLong(args[++i]); break;
                default: System.err.println("Unknown arg: " + args[i]); break;
            }
        }
        if (inPath == null || outPath == null) {
            System.err.println("Usage: --input <file> --output <file> [--timeBudgetMs <ms>]");
            System.exit(1);
        }
        File in = new File(inPath);
        Instance inst = JsonIO.readInstance(in);
        var solver = new ResourceAwareGreedyScheduler();
        AssignmentResult res = solver.solve(inst, budget);
        JsonIO.writeResult(res, new File(outPath));
        System.out.println("Done. Feasible=" + res.feasible + " penalty=" + res.penalty + " runtime_ms=" + res.runtimeMs);
    }
}

