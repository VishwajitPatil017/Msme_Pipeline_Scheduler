package com.msme_pipeline_scheduler.model;

import java.util.ArrayList;
import java.util.List;

/**
 * In-memory representation of an input instance.
 */
public class Instance {
    public final List<Task> tasks = new ArrayList<>();
    public final List<double[]> capacities = new ArrayList<>();
    public final int K; // number of slots

    public Instance(int K) {
        this.K = K;
    }
}

