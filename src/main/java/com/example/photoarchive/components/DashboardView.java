package com.example.photoarchive.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import javax.annotation.security.PermitAll;

@PermitAll
@Route(value = "dashboard", layout = MainAppLayout.class)
public class DashboardView extends Div implements RouterLayout {
    private Span status;

    public DashboardView() {
        add(new H1("Dashboard"));

        HorizontalLayout layout = new HorizontalLayout();
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        status = new Span();
        status.setVisible(false);

//        ConfirmDialog dialog = new ConfirmDialog();
//        dialog.setHeader("Unsaved changes");
//        dialog.setText("Question ?");
//
//        dialog.setCancelable(true);
//        dialog.addCancelListener(event -> setStatus("Canceled"));
//
//        dialog.setRejectable(true);
//        dialog.setRejectText("Discard");
//        dialog.addRejectListener(event -> setStatus("Discarded"));
//
//        dialog.setConfirmText("Save");
//        dialog.addConfirmListener(event -> setStatus("Saved"));
//
        Button buttonq = new Button("Open confirm dialog");
        buttonq.addClickListener(event -> {
//            dialog.open();
            status.setVisible(false);
        });

        layout.add(buttonq, status);
        add(layout);

    }

    private void setStatus(String value) {
        status.setText("Status: " + value);
        status.setVisible(true);
    }

}
