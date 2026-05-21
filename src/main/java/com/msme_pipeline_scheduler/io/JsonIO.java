package com.msme_pipeline_scheduler.io;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.msme_pipeline_scheduler.model.Instance;
import com.msme_pipeline_scheduler.model.Task;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

/**
 * Simple JSON reader that maps the provided instance JSON to the in-memory Instance using Gson.
 */
public class JsonIO {
    private static final Gson G = new Gson();

    public static Instance readInstance(File f) throws IOException {
        try (FileReader fr = new FileReader(f)) {
            Type t = new TypeToken<Map<String,Object>>(){}.getType();
            Map<String,Object> root = G.fromJson(fr, t);
            int K = ((Number) root.get("K")).intValue();
            Instance inst = new Instance(K);

            List<String> taskIds = (List<String>) root.get("tasks");
            List<List<Number>> resources = (List<List<Number>>) root.get("resources");
            List<List<Number>> capacities = (List<List<Number>>) root.get("capacities");
            List<List<Number>> windowsRaw = (List<List<Number>>) root.get("windows");
            List<Number> weights = (List<Number>) root.get("weights");
            List<List<Number>> conflictsRaw = (List<List<Number>>) root.get("conflicts");

            if (capacities != null) {
                for (List<Number> c : capacities) {
                    double[] cap = new double[c.size()];
                    for (int i = 0; i < c.size(); i++) cap[i] = c.get(i).doubleValue();
                    inst.capacities.add(cap);
                }
            } else {
                for (int i = 0; i < K; i++) inst.capacities.add(new double[]{32,128,8,6.0});
            }

            int n = resources.size();
            for (int i = 0; i < n; i++) {
                String id = (taskIds != null && i < taskIds.size()) ? taskIds.get(i) : ("T" + i);
                List<Number> r = resources.get(i);
                double[] rr = new double[r.size()];
                for (int j = 0; j < r.size(); j++) rr[j] = r.get(j).doubleValue();
                List<Number> w = windowsRaw.get(i);
                int lo = w.get(0).intValue();
                int hi = w.get(1).intValue();
                double weight = weights.get(i).doubleValue();
                Task task = new Task(id, i, rr, lo, hi, weight);
                inst.tasks.add(task);
            }

            if (conflictsRaw != null) {
                for (List<Number> pair : conflictsRaw) {
                    int a = pair.get(0).intValue();
                    int b = pair.get(1).intValue();
                    if (a >=0 && a < inst.tasks.size() && b >=0 && b < inst.tasks.size()) {
                        inst.tasks.get(a).conflicts.add(b);
                        inst.tasks.get(b).conflicts.add(a);
                    }
                }
            }
            return inst;
        }
    }

    public static void writeResult(com.msme_pipeline_scheduler.model.AssignmentResult res, File out) throws IOException {
        try (FileWriter fw = new FileWriter(out)) {
            G.toJson(res, fw);
        }
    }
}

