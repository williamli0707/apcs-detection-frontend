package org.caupcakes.api;


import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class EmailService {
    private static final OkHttpClient client = new OkHttpClient().newBuilder()
            .build();

    public static void send(String content) {
        MediaType mediaType = MediaType.parse("application/json");

        JSONObject fromobj = new JSONObject();
        fromobj.put("email", System.getenv("email.sender"));
        fromobj.put("name", System.getenv("email.sender.name"));

        JSONObject to = new JSONObject();
        to.put("email", System.getenv("email.receiver"));

        JSONArray toArray = new JSONArray();
        toArray.put(to);

        JSONObject json = new JSONObject();
        json.put("from", fromobj);
        json.put("to", toArray);
        json.put("subject", System.getenv("email.subject"));
        json.put("text", content);
        json.put("category", "Integration Test"); // default


        RequestBody body = RequestBody.create(json.toString(), mediaType);


        Request request = new Request.Builder()
                .url("https://send.api.mailtrap.io/api/send")
                .post(body)
                .addHeader("Authorization", "Bearer " + System.getenv("email.bearer"))
                .addHeader("Content-Type", "application/json")
                .build();

        try {
            client.newCall(request).execute();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
