package org.caupcakes.records;

public record ViewDiff(String sid, String pid, String html, int startindex, int endindex, long starttime,
                       long endtime) {
}
