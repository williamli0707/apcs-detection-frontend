package org.caupcakes.records;

import org.caupcakes.Colors;
import org.jetbrains.annotations.NotNull;

import java.sql.Date;
import java.text.SimpleDateFormat;

public record Attempt(long timestamp, String code, int index) implements Comparable<Attempt> {
    private static SimpleDateFormat sdf = new SimpleDateFormat("E MMMM d h:m:s a z y");
    @Override
    public int compareTo(@NotNull Attempt o) {
        return Long.compare(timestamp, o.timestamp);
    }

    @Override
    public String toString() {
        return Colors.ANSI_RED + "Attempt " + index + " (" + sdf.format(new Date(timestamp)) + "): \n" + Colors.ANSI_RESET + code + "\n";
    }
}
