package com.example.photoarchive.components;

import com.example.photoarchive.services.FileMetaService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLayout;

import javax.annotation.security.PermitAll;
import java.time.LocalDate;
import java.util.Map;

@PermitAll
@Route(value = "dashboard", layout = MainAppLayout.class)
public class DashboardView extends Div implements RouterLayout {
    private final FileMetaService service;

    public DashboardView(FileMetaService service) {
        this.service = service;

        VerticalLayout layout = new VerticalLayout();
        add(layout);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        layout.add(new H1("Dashboard"));

        Map<LocalDate, Integer> histogram = service.getPhotosStatistics();

        layout.add(new Button("Wow!") {{
            addClickListener(event->{
                Notification.show("Wow pressed!");
            });
        }});
    }
}
