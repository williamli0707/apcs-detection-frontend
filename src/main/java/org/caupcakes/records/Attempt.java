package org.caupcakes.records;

import org.jetbrains.annotations.NotNull;

public record Attempt(long timestamp, String code, int index) implements Comparable<Attempt> {
    @Override
    public int compareTo(@NotNull Attempt o) {
        return Long.compare(timestamp, o.timestamp);
    }
}
