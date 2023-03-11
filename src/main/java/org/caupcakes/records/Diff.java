package org.caupcakes.records;

public record Diff(String sid, String pid, Attempt a1, Attempt a2, double score) {
}
