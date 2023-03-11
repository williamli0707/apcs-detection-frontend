package org.caupcakes.db;

import org.bson.Document;
import org.caupcakes.Checks;
import org.caupcakes.api.RuneStoneAPI;

import java.util.concurrent.PriorityBlockingQueue;

/**
 * This is a priority blocking queue backed by MongoDB
 */
public class DBBackedQueue {
    private static final PriorityBlockingQueue<CodescanTask> queue = new PriorityBlockingQueue<>();

    public static void start() {
        for (Document doc : Database.getQueue().find()) {
            queue.add(new CodescanTask(doc));
        }

        new Thread(() -> {
            CodescanTask task;

            while (true) {
                try {
                    task = queue.take();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                System.out.println(task);

                RuneStoneAPI runestone = new RuneStoneAPI(task.getCookie());

                // use multithreading to get all results to memory

                if (task.getType().equals("majordiff")) {
                    try {
                        Checks.majordiff(task, runestone);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
//                    case "submissioncount" -> {
//                        try {
//                            Checks.submissionCount(task, runestone, 0.05);
//                        } catch (Exception e) {
//                            e.printStackTrace();
//                        }
//                    }
//                    TODO: if you want to add more types of checks, do so here.
                }
            }
        }).start();
    }


    public static boolean add(CodescanTask codescanTask) {
        if (Database.insertIfNotDuplicate(codescanTask)) {
            return queue.add(codescanTask);
        }

        return false;
    }
}
