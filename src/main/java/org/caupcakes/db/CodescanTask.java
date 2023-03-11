package org.caupcakes.db;

import org.bson.Document;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;

public class CodescanTask implements Comparable<CodescanTask> {
    private final ArrayList<String> studentids;
    private final ArrayList<String> problemids;
    private final long time;
    private final String cookie, type;

    public CodescanTask(ArrayList<String> studentids, ArrayList<String> problemids, String cookie, String type, long time) {
        this.studentids = studentids;
        Collections.sort(studentids);

        this.problemids = problemids;
        Collections.sort(problemids);

        this.time = time;
        this.cookie = cookie;
        this.type = type;
    }

    public CodescanTask(Document doc) {
        this.studentids = (ArrayList<String>) doc.get("students");
        Collections.sort(studentids);

        this.problemids = (ArrayList<String>) doc.get("problems");
        Collections.sort(problemids);

        this.time = doc.getLong("time");
        this.cookie = doc.getString("cookie");
        this.type = doc.getString("type");
    }

    public ArrayList<String> getProblemids() {
        return problemids;
    }

    public ArrayList<String> getStudentids() {
        return studentids;
    }

    public String getCookie() {
        return cookie;
    }


    public String getType() {
        return type;
    }

    @Override
    public int compareTo(@NotNull CodescanTask o) {
        return (int) (time - o.time);
    }

    public Document toDocument() {
        return new Document("time", time)
                .append("students", studentids)
                .append("problems", problemids)
                .append("cookie", cookie)
                .append("type", type);
    }

    @Override
    public String toString() {
        return "CodescanTask{" +
                "problemids=" + problemids +
                ", cookie='" + cookie + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    public long getTime() {
        return time;
    }
}
