package org.caupcakes.api;

import org.caupcakes.db.CodescanTask;
import org.caupcakes.db.DBBackedQueue;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.ArrayList;

@CrossOrigin(origins = "*")
@RestController
public class RestAPI {
    @PostMapping("/submit")
    public String submit(@RequestBody String request) {
        JSONObject obj = new JSONObject(request);
        JSONArray students = obj.getJSONArray("students");
        JSONArray problems = obj.getJSONArray("problems");
        String cookie = obj.getString("cookie");
        String type = obj.getString("type");

        ArrayList<String> studentslist = new ArrayList<>(students.length());
        ArrayList<String> problemslist = new ArrayList<>(problems.length());

        for (Object o : students) {
            studentslist.add(((JSONObject) o).getString("id"));
        }
        for (Object o : problems) {
            problemslist.add((String) o);
        }

        return new JSONObject().put("response", DBBackedQueue.add(new CodescanTask(studentslist, problemslist, cookie, type, Instant.now().getEpochSecond()))).toString();
    }
}
