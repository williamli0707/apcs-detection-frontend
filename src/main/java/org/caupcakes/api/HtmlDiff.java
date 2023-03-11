package org.caupcakes.api;

import okhttp3.*;
import org.caupcakes.records.Diff;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Hashtable;

public class HtmlDiff {
    private static final OkHttpClient client = new OkHttpClient.Builder().build();
    private static final Hashtable<Diff, String> diffcache = new Hashtable<>();

    public static String requestDiff(Diff diff) {
        String html = diffcache.get(diff);

        if (html != null) {
            return html;
        }

        MediaType mediaType = MediaType.parse("application/json");
        RequestBody body = RequestBody.create(new JSONObject().put("left", diff.a1().code()).put("right", diff.a2().code()).put("diff_level", "word").toString(), mediaType);
        Request request = new Request.Builder()
                .url("https://api.diffchecker.com/public/text?output_type=html&email=test@example.com")
                .post(body)
                .addHeader("Content-Type", "application/json")
                .build();


        try (Response response = client.newCall(request).execute()) {
            html = response.body().string();

            diffcache.put(diff, html);

            return html;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
