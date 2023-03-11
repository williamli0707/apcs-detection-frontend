package org.caupcakes.db;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.caupcakes.api.RuneStoneAPI;
import org.caupcakes.records.Attempt;
import org.caupcakes.records.Diff;
import org.caupcakes.records.Problem;

import java.time.Instant;
import java.util.*;

import static com.mongodb.client.model.Filters.*;
import static org.caupcakes.api.HtmlDiff.requestDiff;

public class Database {

    private static final MongoClient mongoClient = MongoClients.create(MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(System.getenv("mongodburi")))
            .build());
    private static final MongoCollection<Document> queue = mongoClient.getDatabase("APCS").getCollection("Queue");
    private static final MongoCollection<Document> data = mongoClient.getDatabase("APCS").getCollection("Data");
    private static final MongoCollection<Document> view = mongoClient.getDatabase("APCS").getCollection("View");
    private static final MongoCollection<Document> names = mongoClient.getDatabase("APCS").getCollection("Names");

    static {
        queue.createIndex(eq("time", 1));
        data.createIndex(and(eq("sid", 1), eq("pid", 1)));
        view.createIndex(eq("view", 1));
    }

    public static void deleteCodescanTask(CodescanTask task) {
        queue.deleteOne(eq("time", task.getTime()));
    }

    public static boolean insertIfNotDuplicate(CodescanTask task) {
        ArrayList<String> students = task.getStudentids();
        ArrayList<String> problems = task.getProblemids();

        Collections.sort(students);
        Collections.sort(problems);

        int studentstrlen = students.size();
        int problemsstrlen = problems.size();

        // we assume the queue is not too long
        for (Document doc : queue.find()) {
            ArrayList<String> studentlist = (ArrayList<String>) doc.get("students");
            ArrayList<String> problemslist = (ArrayList<String>) doc.get("problems");

            if (problemslist.size() != problemsstrlen || studentlist.size() != studentstrlen) {
                continue;
            }

            if (arrayListDeepEquals(studentlist, students) && arrayListDeepEquals(problemslist, problems)) {
                // a duplicate task is in the queue
                return false;
            }
        }

        queue.insertOne(task.toDocument());

        return true;
    }

    public static MongoCollection<Document> getQueue() {
        return queue;
    }

    private static <T> boolean arrayListDeepEquals(ArrayList<T> a, ArrayList<T> b) {
        return a.containsAll(b) && b.containsAll(a);
    }

    public static void save(Hashtable<String, Hashtable<String, Problem>> table) {
        for (Map.Entry<String, Hashtable<String, Problem>> student : table.entrySet()) {
            String sid = student.getKey();

            for (Map.Entry<String, Problem> problem : student.getValue().entrySet()) {
                String pid = problem.getKey();

                List<Map.Entry<Long, String>> list = problem.getValue().attempts().stream().map(s -> Map.entry(s.timestamp(), s.code())).toList();

                Document submissions = new Document();

                list.forEach(s -> submissions.append(String.valueOf(s.getKey()), s.getValue()));

                data.insertOne(new Document("sid", sid)
                        .append("pid", pid)
                        .append("grade", problem.getValue().grade())
                        .append("submissions", submissions));
            }
        }
    }

    public static Map.Entry<Hashtable<String, Hashtable<String, Problem>>, Hashtable<String, ArrayList<String>>> getData(List<String> studentids, List<String> problemids, RuneStoneAPI runeStoneAPI) {
        Hashtable<String, Hashtable<String, Problem>> table = new Hashtable<>();
        Hashtable<String, ArrayList<String>> notfound = new Hashtable<>();

        int counter = 0;

        for (Document search : data.find(and(in("sid", studentids), in("pid", problemids)))) {
            counter++;
            String sid = search.getString("sid");
            String pid = search.getString("pid");

            if (!table.containsKey(sid)) {
                table.put(sid, new Hashtable<>());
            }

            LinkedList<Attempt> sortlist = new LinkedList<>();

            for (Map.Entry<String, Object> entry : ((Document) search.get("submissions")).entrySet()) {
                sortlist.add(new Attempt(Long.parseLong(entry.getKey()), entry.getValue().toString(), 0));
            }
            sortlist.sort(Attempt::compareTo);

            int index = 2; // lines up with runestone viewer
            LinkedList<Attempt> attempts = new LinkedList<>();
            for (Attempt a : sortlist) {
                attempts.add(new Attempt(a.timestamp(), a.code(), index++));
            }

            table.get(sid).put(pid, new Problem(search.getInteger("grade"), attempts, runeStoneAPI.getDefaultCodeTemplate(pid)));
        }

        int expected = studentids.size() * problemids.size();

        if (expected != counter) {
            for (String sid : studentids) {
                for (String pid : problemids) {
                    if (!table.containsKey(sid) || !table.get(sid).containsKey(pid)) {
                        notfound.computeIfAbsent(sid, k -> new ArrayList<>()).add(pid);
                    }
                }
            }

        }

        return new AbstractMap.SimpleEntry<>(table, notfound);
    }

    public static String saveResults(LinkedList<Diff> notimedata, LinkedList<Diff> timedata) {
        String id = UUID.randomUUID().toString();

        Document notimedoc = new Document();
        insertData(notimedoc, notimedata);

        Document timedoc = new Document();
        insertData(timedoc, timedata);

        view.insertOne(new Document("id", id).append("notimedata", notimedoc).append("timedata", timedoc).append("time", Instant.now().getEpochSecond()));

        return id;
    }

    private static void insertData(Document doc, LinkedList<Diff> timedata) {
        int index = 0;
        for (Diff d : timedata) {
            doc.append(String.valueOf(index++), new Document("sid", d.sid()).append("pid", d.pid()).append("html", requestDiff(d)).append("startindex", d.a1().index()).append("starttime", d.a1().timestamp()).append("endindex", d.a2().index()).append("endtime", d.a2().timestamp()));
        }
    }

    public static Document find(String id) {
        return view.find(eq("id", id)).first();
    }

    public static Hashtable<String, String> loadNames() {
        Hashtable<String, String> data = new Hashtable<>();

        for (Document doc : names.find()) {
            data.put(doc.getString("sid"), doc.getString("name"));
        }

        return data;
    }

    public static void saveNames(Hashtable<String, String> data) {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            names.insertOne(new Document("sid", entry.getKey()).append("name", entry.getValue()));
        }
    }
}
