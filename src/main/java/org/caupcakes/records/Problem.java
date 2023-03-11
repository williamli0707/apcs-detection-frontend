package org.caupcakes.records;

import java.util.LinkedList;

public record Problem(int grade, LinkedList<Attempt> attempts, String defaultCodeTemplate) {
}
