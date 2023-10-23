package org.caupcakes.api;

import com.github.sisyphsu.dateparser.DateParserUtils;
import com.helger.commons.string.util.LevenshteinDistance;
import com.opencsv.CSVWriter;
import okhttp3.*;
import org.caupcakes.records.Attempt;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RunestoneAPI2 {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();

    private final static Hashtable<String, String> studentnamescache = new Hashtable<>();
    private final static Hashtable<String, String> defaultcodetemplatecache = new Hashtable<>();
    private String cookie;
    private static final String loginURL = "https://runestone.academy/user/login?_next=/";
    private static final String gradeURL = "https://runestone.academy/ns/assessment/gethist";

    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final Pattern pform = Pattern.compile("<input name=\"_formkey\" type=\"hidden\" value=\"(.*?)\" />"),
                                 psessionid = Pattern.compile("session_id_runestone=(.*?);");

    public RunestoneAPI2() {
        resetCookie();
        System.out.println("Done initializing cookie: " + cookie);
        cookie = "_gcl_au=1.1.1332442316.1694036375; __utmc=28105279; RS_info=\"{\"readings\": [\"Lists/ListComprehensions.html\"\054 \"Lists/NestedLists.html\"\054 \"Lists/StringsandLists.html\"\054 \"Lists/listTypeConversionFunction.html\"\054 \"Lists/TuplesandMutability.html\"\054 \"Lists/TupleAssignment.html\"\054 \"Lists/TuplesasReturnValues.html\"\054 \"Lists/Exercises.html\"]\054 \"tz_offset\": 7}\"; session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd; __utma=28105279.603586017.1694036376.1696452015.1698096050.12; __utmz=28105279.1698096050.12.10.utmcsr=landing.runestone.academy|utmccn=(referral)|utmcmd=referral|utmcct=/; session_id_runestone=51880725:2b28a8f1-cad1-45ef-958f-7589dd75af69; access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDcxNjgwNTN9.m9PZxL6-nYqdxQxXXxp9F81M5VxPoAFK6q_HFzhT4yk; __utmb=28105279.3.10.1698096050";
        //TODO fix login issue - session id finding doesn't work
        initNameCache();
    }

    private void initNameCache() {
        Hashtable<String, String> newnames = new Hashtable<>();

        MediaType mediaType = MediaType.parse("text/plain");
        RequestBody body = RequestBody.create("", mediaType);
        Request request = new Request.Builder()
                .url("https://runestone.academy/runestone/admin/course_students")
                .method("POST", body)
                .addHeader("Accept", "application/json, text/javascript, */*; q=0.01")
                .addHeader("Accept-Language", "en-US,en;q=0.9")
                .addHeader("Connection", "keep-alive")
                .addHeader("Content-Length", "0")
                .addHeader("Cookie", cookie)
                .addHeader("Origin", "https://runestone.academy")
                .addHeader("Referer", "https://runestone.academy/runestone/admin/grading")
                .addHeader("Sec-Fetch-Dest", "empty")
                .addHeader("Sec-Fetch-Mode", "cors")
                .addHeader("Sec-Fetch-Site", "same-origin")
                .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/107.0.0.0 Safari/537.36")
                .addHeader("X-Requested-With", "XMLHttpRequest")
                .addHeader("sec-ch-ua", "\"Google Chrome\";v=\"107\", \"Chromium\";v=\"107\", \"Not=A?Brand\";v=\"24\"")
                .addHeader("sec-ch-ua-mobile", "?0")
                .addHeader("sec-ch-ua-platform", "\"Windows\"")
                .build();

        String resp = (String) request(request);
        System.out.println(resp);

        JSONObject respjson = new JSONObject(resp);

        for (Map.Entry<String, Object> entry : respjson.toMap().entrySet()) {
            if (studentnamescache.putIfAbsent(entry.getKey(), (String) entry.getValue()) == null) {
                newnames.put(entry.getKey(), (String) entry.getValue());
            }
        }
    }

    public Hashtable<String, String> getNames() {
        return studentnamescache;
    }

    public void resetCookie() {
        try {
            Response resp = client.newCall(new Request.Builder()
                    .url(loginURL)
                    .post(RequestBody.create(new byte[]{})).build()
            ).execute();
            String respBody = resp.body().string();
            Matcher matcher = pform.matcher(respBody);
            if(!matcher.find()) throw new RuntimeException("no formkey found");
            String formkey = matcher.group(1);

            String payload = "{\n" +
                    "        \"username\": \"wli223\",\n" +
                    "        \"password\": \"verysecurepassword\",\n" +
                    "        \"_next\": \"/\",\n" +
                    "        \"_formkey\": " + formkey + ",\n" +
                    "        \"_formname\": \"login\"\n" +
                    "    }";

            Response resp2 = client.newCall(new Request.Builder()
                    .url(loginURL)
                    .post(RequestBody.create(payload, JSON))
                    .build()
            ).execute();
            System.out.println(resp2.body().string());
            matcher = psessionid.matcher(Objects.requireNonNull(resp2.headers().get("Set-Cookie")));
            if(!matcher.find()) throw new RuntimeException("no session id found");
            String sessionID = matcher.group(1);
            System.out.println("session id: " + sessionID);
//            cookie = "_gcl_au=1.1.1332442316.1694036375; " +
//                    "__utmc=28105279; " +
//                    "RS_info=\"{" +
//                    "\\\"readings\\\": [" +
//                    "\\\"Lists/ListComprehensions.html\\\"\\054 " +
//                    "\\\"Lists/NestedLists.html\\\"\\054 " +
//                    "\\\"Lists/StringsandLists.html\\\"\\054 " +
//                    "\\\"Lists/listTypeConversionFunction.html\\\"\\054 " +
//                    "\\\"Lists/TuplesandMutability.html\\\"\\054 " +
//                    "\\\"Lists/TupleAssignment.html\\\"\\054 \\\"Lists/TuplesasReturnValues.html\\\"\\054 " +
//                    "\\\"Lists/Exercises.html\\\"" +
//                    "]\\054 \\\"tz_offset\\\": 7" +
//                    "}\"; " +
//                    "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd; " +
//                    "__utma=28105279.603586017.1694036376.1695415067.1695677234.8; " +
//                    "__utmz=28105279.1695677234.8.6.utmcsr=landing.runestone.academy|utmccn=(referral)|utmcmd=referral|utmcct=/; " +
//                    "__utmt=1; " +
//                    "session_id_runestone=" + sessionID + "; " +
//                    "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDcxNjgwNTN9.m9PZxL6-nYqdxQxXXxp9F81M5VxPoAFK6q_HFzhT4yk; " +
//                    "__utmb=28105279.3.10.1695677234";
            cookie = "_gcl_au=1.1.1332442316.1694036375; " +
            "__utmc=28105279; " +
            "RS_info=\"{\\\"readings\\\": [\\\"Lists/ListComprehensions.html\\\"\\054 \\\"Lists/NestedLists.html\\\"\\054 \\\"Lists/StringsandLists.html\\\"\\054 \\\"Lists/listTypeConversionFunction.html\\\"\\054 \\\"Lists/TuplesandMutability.html\\\"\\054 \\\"Lists/TupleAssignment.html\\\"\\054 \\\"Lists/TuplesasReturnValues.html\\\"\\054 \\\"Lists/Exercises.html\\\"]\\054 \\\"tz_offset\\\": 7}\"; " +
            "session_id_admin=205.173.47.254-12e99be8-596a-48ec-b212-f66d61c5ebdd; " +
            "__utma=28105279.603586017.1694036376.1695415067.1695677234.8; " +
            "__utmz=28105279.1695677234.8.6.utmcsr=landing.runestone.academy|utmccn=(referral)|utmcmd=referral|utmcct=/; " +
            "__utmt=1; session_id_runestone=" + "51880935:b82c80f1-7409-4c75-8521-ee3ae9dba149" + "; " +
            "access_token=eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ3bGkyMjMiLCJleHAiOjE3MDQ3NDkyMzh9.Thc1eaRZscxOu1MwIJfZps1Tp3tKUNU2fUOisP3Ia_g; " +
            "__utmb=28105279.3.10.1695677234";
            resp.close();
            resp2.close();
        }
        catch (IOException e) {
            throw new RuntimeException(e);
        }
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
//        System.out.println(content);
        try {
            PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("test.txt")));
            pw.write(content);
            pw.close();
        } catch(Exception ignored) {}
        return new JSONObject(content).getInt("grade");
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

//        HashMap<Attempt, Integer> returnMap = new HashMap<>();
//
//        int index = 2; // lines up with runestone viewer
//        for (Attempt a : sortlist) {
////            returnMap.put(new Attempt(a.timestamp(), a.code(), index++), requestGrade(sid, pid));
//            returnMap.put(new Attempt(a.timestamp(), a.code(), index++), 0);
//        }
//        return returnMap;

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

    public void writeStudentData(String pid, String sid, String path) {
        try {
            CSVWriter writer = new CSVWriter(new FileWriter(path));
            LinkedList<Attempt> l = requestHistory(sid, pid);
            String[] r1 = new String[l.size()], r2 = new String[l.size()], r3 = new String[l.size()];
            String prev = "";
            int ind = 0;
            for (Attempt i : l) {
                r1[ind] = i.code();
                r2[ind] = String.valueOf(i.timestamp());
                r3[ind] = String.valueOf(LevenshteinDistance.getDistance(prev, i.code()));
                ind++;
                prev = i.code();
            }
            writer.writeNext(r1);
            writer.writeNext(r2);
            writer.writeNext(r3);
            writer.close();
        }
        catch (Exception ignored) {}
    }

    //TODO
    public void getStudentData(String pid, String sid) {
        LinkedList<Attempt> l = requestHistory(sid, pid);
        String[] r2 = new String[l.size()], r3 = new String[l.size()];
        String prev = "";
        int ind = 0;
        for (Attempt i : l) {
            r2[ind] = String.valueOf(i.timestamp());
            r3[ind] = String.valueOf(LevenshteinDistance.getDistance(prev, i.code()));
            ind++;
            prev = i.code();
        }
    }

    /**
     * Returns the edit distance / second * 10000 metric for all students for a given problem.
     * @param pid the problem id of the problem to be analyzed
     * @return a HashMap with the key values of student ID's and values of metrics, average and maximum in that order.
     */
    public HashMap<String, double[]> getAllScores(String pid) {
        Hashtable<String, String> names = getNames();
        HashMap<String, double[]> scores = new HashMap<>();
        for (String key : names.keySet()) {
            System.out.println(names.get(key) + " (" + key + ")");
            LinkedList<Attempt> history = requestHistory(key, "lhs_test_list2");
//            System.out.println(history);
            double min = 0, max = 0, sum = 0, num = 0;
            Attempt prev = null;
//            System.out.println("num submissions: " + history.size());
            for (Attempt attempt : history) {
                num++;
                if(num == 1) {
                    prev = attempt;
                    continue;
                }
                double diff = 10000d * LevenshteinDistance.getDistance(prev.code(), attempt.code()) / (attempt.timestamp() - prev.timestamp());
//                System.out.println("dist: " + LevenshteinDistance.getDistance(prev.code(), attempt.code()) + " time: " + (attempt.timestamp() - prev.timestamp()) + " diff: " + diff);
                min = Math.min(min, diff);
                max = Math.max(max, diff);
                sum += diff;
                prev = attempt;
            }
            if(num == 1) {
                System.out.println("N/A - one submission");
                break;
            }
            sum /= (num - 1);
            System.out.println("Average edit distance per second: " + sum);
            System.out.println("Maximum edit distance per second: " + max);
            scores.put(key, new double[]{sum, max});
        }
        return scores;
    }
}

