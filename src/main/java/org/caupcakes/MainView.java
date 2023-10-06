package org.caupcakes;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import org.caupcakes.api.RunestoneAPI2;

@Route("")
public class MainView extends VerticalLayout {
    private RunestoneAPI2 api;
    public MainView() {
        RunestoneAPI2 api = new RunestoneAPI2();
        HorizontalLayout hstack = new HorizontalLayout();
        TextField pid_input = new TextField();
        TextField sid_input = new TextField();
        Label errorText = new Label("");
        Button analyze = new Button("Analyze");
        pid_input.setPlaceholder("Assignment ID (ex. 'lhs_test_3')");
        sid_input.setPlaceholder("Student ID (ex. 'lhs_520001')");
        hstack.add(pid_input, sid_input);
        errorText.setVisible(false);
        errorText.getStyle().set("color", "red");
        analyze.addClickListener(event -> {
            String pid = pid_input.getValue();
            String sid = sid_input.getValue();
            if (pid.equals("") || sid.equals("")) {
                return;
            }
            api.requestHistory(sid, pid);
        });
        add(
                hstack,
                analyze
        );
    }
}