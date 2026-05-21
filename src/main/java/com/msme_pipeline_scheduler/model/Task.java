package com.msme_pipeline_scheduler.model;

import java.util.HashSet;
import java.util.Set;

/**
 * Represents a scheduling task.
 */
public class Task {
    public final String id;
    public final int index; // 0-based index
    public final double[] resources; // length 4: CPU, RAM, GPU, NET
    public final int windowLo;
    public final int windowHi;
    public final double weight;
    public final Set<Integer> conflicts = new HashSet<>();

    public Task(String id, int index, double[] resources, int windowLo, int windowHi, double weight) {
        this.id = id;
        this.index = index;
        this.resources = resources;
        this.windowLo = windowLo;
        this.windowHi = windowHi;
        this.weight = weight;
    }

    @Override
    public String toString() {
        return "Task{" + id + " idx=" + index + " w=" + weight + " window=[" + windowLo + "," + windowHi + "+]" + '}';
    }
}

