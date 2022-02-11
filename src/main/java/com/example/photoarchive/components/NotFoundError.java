package com.example.photoarchive.components;

import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.ParentLayout;

//@Tag("div")
@ParentLayout(MainAppLayout.class)
public class NotFoundError extends Div implements HasErrorParameter<NotFoundException>, HasComponents {
    H1 header = new H1();
    public NotFoundError() {
        add(header);
    }
    @Override
    public int setErrorParameter(BeforeEnterEvent event, ErrorParameter<NotFoundException> parameter) {
        header.setText("404. Could navigate to '%s'".formatted(event.getLocation().getPath()));
        return 404;
    }
}
