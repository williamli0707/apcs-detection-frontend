package org.caupcakes.api;

import com.github.sisyphsu.dateparser.DateParserUtils;
import okhttp3.*;
import org.caupcakes.records.Attempt;
import org.caupcakes.records.Problem;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.caupcakes.db.Database.*;


public record RuneStoneAPI(String cookie) {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    private final static Hashtable<String, String> studentnamescache = loadNames();
    private final static Hashtable<String, String> defaultcodetemplatecache = new Hashtable<>();

    public RuneStoneAPI(String cookie) {
        this.cookie = cookie;

        initNameCache();
    }

    public static String getName(String sid) {
        return studentnamescache.get(sid);
    }

    public int requestGrade(String sid, String pid) {
        String content = (String) request(new Request.Builder()
                .url("https://runestone.academy/runestone/admin/getGradeComments?acid=" + pid + "&sid=" + sid)
                .get()
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", cookie)
                .addHeader("DNT", "1")
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"105\", \"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"105\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build());

        return new JSONObject(content).getInt("grade");
    }

    private void initNameCache() {
        Hashtable<String, String> newnames = new Hashtable<>();

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create("", mediaType);
        Request request = new Request.Builder()
                .url("https://runestone.academy/runestone/admin/course_students")
                .method("POST", body)
                .addHeader("Accept", "application/json")
                .addHeader("Accept-Encoding", "gzip, deflate, br")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Length", "0")
                .addHeader("Content-Type", "application/json; charset=utf-8")
                .addHeader("Cookie", cookie)
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/116.0.0.0 Safari/537.36")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("Sec-Ch-Ua", "\"Chromium\";v=\"116\", \"Not)A;Brand\";v=\"24\", \"Google Chrome\";v=\"116\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"macOS\"")
                .build();

        String resp = (String) request(request);
        System.out.println(resp);
        JSONObject respjson = new JSONObject(resp);

        for (Map.Entry<String, Object> entry : respjson.toMap().entrySet()) {
            if (studentnamescache.putIfAbsent(entry.getKey(), (String) entry.getValue()) == null) {
                newnames.put(entry.getKey(), (String) entry.getValue());
            }
        }

        saveNames(newnames);
    }

    /**
     * Returns <Sid, <Pid, Problem>>
     *
     * @param studentids The students to get problems for
     * @param problemids The problems to get
     * @return data
     */
    public Hashtable<String, Hashtable<String, Problem>> getAllData(List<String> studentids, List<String> problemids) {
        long time = Instant.now().toEpochMilli();

        Map.Entry<Hashtable<String, Hashtable<String, Problem>>, Hashtable<String, ArrayList<String>>> dbdata = getData(studentids, problemids, this);
        Hashtable<String, Hashtable<String, Problem>> newdata = new Hashtable<>();

        int threads = Math.min(10, dbdata.getValue().size() / 2);

        if (threads > 0) {
            System.out.println("threads " + threads);

            Hashtable<String, String> defaultcodetable = new Hashtable<>();
            ExecutorService executor = Executors.newFixedThreadPool(threads);

            List<String> students = dbdata.getValue().keySet().stream().toList();
            for (int i = 0; i < threads; i++) {
                int finalI = i;
                executor.submit(() -> {
                    try {
                        for (String sid : students.subList(students.size() / threads * finalI, students.size() / threads * (finalI + 1))) {
                            for (String pid : dbdata.getValue().get(sid)) {
                                try {
                                    Hashtable<String, Problem> srow = newdata.getOrDefault(sid, new Hashtable<>());
                                    Hashtable<String, Problem> fromdbrow = dbdata.getKey().getOrDefault(sid, new Hashtable<>());

                                    LinkedList<Attempt> history = requestHistory(sid, pid);
                                    int grade = requestGrade(sid, pid);

                                    String defaultcodetemplate = defaultcodetable.getOrDefault(pid, getDefaultCodeTemplate(pid));

                                    srow.put(pid, new Problem(grade, history, defaultcodetemplate));
                                    fromdbrow.put(pid, new Problem(grade, history, defaultcodetemplate));

                                    newdata.put(sid, srow);
                                    dbdata.getKey().put(sid, fromdbrow);

                                    if (!defaultcodetable.containsKey(pid)) {
                                        defaultcodetable.put(pid, defaultcodetemplate);
                                    }
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
            }

            try {
                executor.shutdown();
                executor.awaitTermination(10, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            save(newdata);
        }

        System.out.println(Instant.now().toEpochMilli() - time);

        return dbdata.getKey();
    }

    public String getDefaultCodeTemplate(String pid) {
        String s = defaultcodetemplatecache.get(pid);

        if (s != null) {
            return s;
        }

        Request request = new Request.Builder()
                .url("https://runestone.academy/runestone/admin/htmlsrc?acid=" + pid)
                .get()
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .addHeader("Cookie", cookie)
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/109.0.0.0 Safari/537.36")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("sec-ch-ua", "\"Not_A Brand\";v=\"99\", \"Google Chrome\";v=\"109\", \"Chromium\";v=\"109\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build();

        String resp = (String) request(request);

        Document doc = Jsoup.parse(new JSONObject(resp).getString("htmlsrc"));

        String body = doc.select("textarea").text();

        body = body.substring(0, body.indexOf("===="));

        defaultcodetemplatecache.put(pid, body);

        return body;
    }

    public LinkedList<Attempt> requestHistory(String sid, String pid) {
        MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
        JSONObject body = new JSONObject().put("acid", pid).put("sid", sid);
        RequestBody reqbody = RequestBody.create(body.toString(), mediaType);

        Request request = new Request.Builder()
                .url("https://runestone.academy/ns/assessment/gethist")
                .post(reqbody)
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", cookie)
                .addHeader("DNT", "1")
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                .addHeader("accept", "application/json")
                .addHeader("content-type", "application/json; charset=utf-8")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"105\", \"Not)A;Brand\";v=\"8\", \"Chromium\";v=\"105\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build();

        String resp = (String) request(request);

        JSONObject respjson = new JSONObject(resp).getJSONObject("detail");
        JSONArray history = respjson.getJSONArray("history");
        JSONArray timestamps = respjson.getJSONArray("timestamps");

        Iterator<Object> historyiter = history.iterator();
        Iterator<Object> timestampsiter = timestamps.iterator();

        LinkedList<Attempt> sortlist = new LinkedList<>();

        while (historyiter.hasNext()) {
            // we opt to use DateParserUtils because I'm too lazy to convert dates.
            sortlist.add(new Attempt(DateParserUtils.parseDate(timestampsiter.next().toString()).getTime(), (String) historyiter.next(), 0));
        }
        sortlist.sort(Attempt::compareTo);

        LinkedList<Attempt> returnlist = new LinkedList<>();

        int index = 2; // lines up with runestone viewer
        for (Attempt a : sortlist) {
            returnlist.add(new Attempt(a.timestamp(), a.code(), index++));
        }

        return returnlist;
    }

    private Object request(Request request) { // no param 3 retries
        return request(request, 3);
    }

    private Object request(Request request, int retries) {
        if (retries == 0) {
            throw new RuntimeException(" requests failed for " + request.url());
        }

        try (Response resp = client.newCall(request).execute()) {
            String out = resp.body().string();

            resp.close();

            return out;
        } catch (Exception e) {
            return request(request, retries - 1);
        }
    }
}

