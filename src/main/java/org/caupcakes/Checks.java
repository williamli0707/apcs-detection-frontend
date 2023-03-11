package org.caupcakes;

import org.bitbucket.cowwoc.diffmatchpatch.DiffMatchPatch;
import org.caupcakes.api.EmailService;
import org.caupcakes.api.RuneStoneAPI;
import org.caupcakes.db.CodescanTask;
import org.caupcakes.records.Attempt;
import org.caupcakes.records.Diff;
import org.caupcakes.records.Problem;

import java.util.*;
import java.util.stream.Collectors;

import static org.caupcakes.db.Database.deleteCodescanTask;
import static org.caupcakes.db.Database.saveResults;

public class Checks {
    private static final DiffMatchPatch dmp = new DiffMatchPatch();

    public static void majordiff(CodescanTask task, RuneStoneAPI runestone) {
        Hashtable<String, Hashtable<String, Problem>> data = runestone.getAllData(task.getStudentids(), task.getProblemids());
        // sid, pid, Problem

        LinkedList<Diff> notimesuslist = new LinkedList<>();
        LinkedList<Diff> timesuslist = new LinkedList<>();

        for (Map.Entry<String, Hashtable<String, Problem>> student : data.entrySet()) {
            String sid = student.getKey();

            for (Map.Entry<String, Problem> problem : student.getValue().entrySet()) {
                String pid = problem.getKey();
                Problem p = problem.getValue();

                List<Attempt> filtered = p.attempts().stream().filter(s -> s.code().length() > 50).toList();
                if (filtered.size() > 0) {
                    Attempt last = null;

                    for (Attempt a : filtered) {
                        if (last != null) {
                            double diffscore = computeDiffScore(last.code(), a.code());
                            double defaultscore = computeDiffScore(p.defaultCodeTemplate(), a.code());
                            double difflastscore = computeDiffScore(p.defaultCodeTemplate(), last.code());

                            double score = (diffscore * Math.sqrt(defaultscore * difflastscore));

                            notimesuslist.add(new Diff(sid, pid, last, a, score));

                            score /= (a.timestamp() - last.timestamp()) / 1000.0;

                            timesuslist.add(new Diff(sid, pid, last, a, score));
                        }

                        last = a;
                    }
                }
            }
        }

        notimesuslist.sort((o1, o2) -> Double.compare(o2.score(), o1.score()));
        timesuslist.sort((o1, o2) -> Double.compare(o2.score(), o1.score()));

        // truncate to 50

        notimesuslist = new LinkedList<>(notimesuslist.subList(0, 50));
        timesuslist = new LinkedList<>(timesuslist.subList(0, 50));

        // save in db and return url
        String id = saveResults(notimesuslist, timesuslist);

        // send email
        EmailService.send("Results are available at: " + System.getenv("url") + id);

        // delete the original document in queue
        deleteCodescanTask(task);
    }


    public static void submissionCount(CodescanTask task, RuneStoneAPI runestone, double percent) {
        if (percent > 1.0 || percent < 0.0) {
            throw new RuntimeException("Cannot examine top " + percent + " percent. Must be number between 0 and 1");
        }

        Hashtable<String, Hashtable<String, Problem>> data = runestone.getAllData(task.getStudentids(), task.getProblemids());
        Hashtable<String, Integer> maxscore = new Hashtable<>(); // pid, max score

        for (Map.Entry<String, Hashtable<String, Problem>> student : data.entrySet()) {
            for (Map.Entry<String, Problem> problem : student.getValue().entrySet()) {
                maxscore.merge(problem.getKey(), problem.getValue().grade(), Integer::max);
            }
        }

        // modify data since we dont reuse data

        Hashtable<String, ArrayList<Map.Entry<String, Integer>>> attemptstable = new Hashtable<>(); // pid, <sid, attempts>

        for (Map.Entry<String, Hashtable<String, Problem>> student : data.entrySet()) {
            String sid = student.getKey();

            for (Map.Entry<String, Problem> problem : student.getValue().entrySet()) {
                String pid = problem.getKey();

                if (problem.getValue().grade() == maxscore.get(pid)) {
                    if (attemptstable.containsKey(pid)) {
                        attemptstable.get(pid).add(new AbstractMap.SimpleEntry<>(sid, problem.getValue().attempts().size()));
                    } else {
                        ArrayList<Map.Entry<String, Integer>> deflist = new ArrayList<>();
                        deflist.add(new AbstractMap.SimpleEntry<>(sid, problem.getValue().attempts().size()));

                        attemptstable.put(pid, deflist);
                    }
                }
            }
        }

        for (Map.Entry<String, ArrayList<Map.Entry<String, Integer>>> problem : attemptstable.entrySet()) {
            String pid = problem.getKey();

            System.out.println(pid);

            ArrayList<Map.Entry<String, Integer>> attempt = problem.getValue();
            attempt.sort(Map.Entry.comparingByValue());

            int[] attemptsarr = new int[attempt.size()];
            int index = 0;

            for (Map.Entry<String, Integer> entry : attempt) {
                attemptsarr[index++] = entry.getValue();
            }

//             truncate list to bottom 5%
            attempt = attempt.stream().limit((long) Math.ceil(attempt.size() * percent)).collect(Collectors.toCollection(ArrayList::new));

            for (Map.Entry<String, Integer> attempts : attempt) {
                System.out.println(RuneStoneAPI.getName(attempts.getKey()) + ": " + attempts.getValue());
            }
        }
    }

    private static double computeDiffScore(String s1, String s2) {
        LinkedList<DiffMatchPatch.Diff> diff = dmp.diffMain(s1, s2);

        return diff.stream().map(s -> (s.operation == DiffMatchPatch.Operation.INSERT) ? Math.pow(s.text.length(), 0.8) : 0.0).reduce(0.0, Double::sum);
    }
}

