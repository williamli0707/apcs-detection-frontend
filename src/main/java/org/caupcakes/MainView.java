package org.caupcakes;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.IFrame;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.bson.Document;
import org.caupcakes.api.RuneStoneAPI;
import org.caupcakes.db.Database;
import org.caupcakes.records.ViewDiff;

import java.util.Date;
import java.util.Map;


@Route(value = "/view/:id")
public class MainView extends VerticalLayout implements BeforeEnterObserver, HasDynamicTitle {
    private final Checkbox toggletime = new Checkbox();
    private final Button arrowleft = new Button(new Icon(VaadinIcon.ARROW_LEFT));
    private final Button arrowright = new Button(new Icon(VaadinIcon.ARROW_RIGHT));
    private final ViewDiff[] notimedata = new ViewDiff[50];
    private final ViewDiff[] timedata = new ViewDiff[50];
    private final IFrame diffview = new IFrame();
    private final Label title = new Label();
    private boolean time = false;
    private int pointer = 0;

    public MainView() {
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String id = event.getRouteParameters().get("id").get();

        Document doc = Database.find(id);

        if (id.equals("") || doc == null) {
            return;
        }

        loadData(doc);

        HorizontalLayout topbar = new HorizontalLayout();

        topbar.add(title);
        title.getStyle().set("margin-right", "auto");

        toggletime.setLabel("time in formula");
        toggletime.setValue(false);
        toggletime.addClickListener(s -> {
            if (toggletime.getValue()) {
                Notification.show("time in formula");
                time = true;
            } else {
                Notification.show("time not in formula");
                time = false;
            }

            pointer = 0;
            arrowleft.setEnabled(false);
            arrowright.setEnabled(true);
            display();
        });
        toggletime.addClickShortcut(Key.SPACE);

        topbar.add(toggletime);

        arrowleft.setEnabled(false);
        arrowleft.addClickListener(s -> dec());
        arrowleft.addClickShortcut(Key.ARROW_LEFT);
        topbar.add(arrowleft);

        arrowright.addClickListener(s -> inc());
        arrowright.addClickShortcut(Key.ARROW_RIGHT);
        topbar.add(arrowright);

        topbar.setAlignItems(Alignment.CENTER);
        topbar.setWidth("100%");

        add(topbar);

        diffview.setWidth("100%");
        diffview.setHeight("100%");
        diffview.getElement().setAttribute("frameBorder", "0");
        add(diffview);

        display();

        setHeightFull();
    }

    private void dec() {
        pointer--;
        if (pointer == 0) {
            arrowleft.setEnabled(false);
        }

        if (pointer == timedata.length - 2) {
            arrowright.setEnabled(true);
        }

        display();
    }

    private void inc() {
        pointer++;
        if (pointer == 1) {
            arrowleft.setEnabled(true);
        }

        if (pointer == timedata.length - 1) {
            arrowright.setEnabled(false);
        }

        display();
    }

    private void display() {
        ViewDiff viewDiff;

        if (time) {
            viewDiff = timedata[pointer];
        } else {
            viewDiff = notimedata[pointer];
        }

        diffview.setSrcdoc(viewDiff.html());

        long millis = viewDiff.endtime() - viewDiff.starttime();

        title.setText(RuneStoneAPI.getName(viewDiff.sid()) + " (" + viewDiff.pid() + " " + viewDiff.startindex() + "->" + viewDiff.endindex() + ") " + getAge(millis) + " (" + new Date(viewDiff.starttime()) + " " + new Date(viewDiff.endtime()) + ")");
    }

    public String getAge(long age) {
        String ageString = DurationFormatUtils.formatDuration(age, "d") + "d";
        if ("0d".equals(ageString)) {
            ageString = DurationFormatUtils.formatDuration(age, "H") + "h";
            if ("0h".equals(ageString)) {
                ageString = DurationFormatUtils.formatDuration(age, "m") + "m";
                if ("0m".equals(ageString)) {
                    ageString = DurationFormatUtils.formatDuration(age, "s") + "s";
                    if ("0s".equals(ageString)) {
                        ageString = age + "ms";
                    }
                }
            }
        }
        return ageString;
    }

    private void loadData(Document doc) {
        loadintoArray(doc.get("notimedata", Document.class), notimedata);
        loadintoArray(doc.get("timedata", Document.class), timedata);
    }

    public void loadintoArray(Document doc, ViewDiff[] data) {
        for (Map.Entry<String, Object> entry : doc.entrySet()) {
            int key = Integer.parseInt(entry.getKey());
            Document value = (Document) entry.getValue();
            ViewDiff viewDiff = new ViewDiff(value.getString("sid"), value.getString("pid"), value.getString("html"), value.getInteger("startindex"), value.getInteger("endindex"), value.getLong("starttime"), value.getLong("endtime"));

            data[key] = viewDiff;
        }
    }

    @Override
    public String getPageTitle() {
        return title.getText();
    }
}

