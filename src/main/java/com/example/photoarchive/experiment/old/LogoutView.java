package com.example.photoarchive.experiment.old;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.History;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.server.VaadinSession;

//@AnonymousAllowed
//@Route(value = "logout", layout = MainAppLayout.class)
public class LogoutView extends Div implements BeforeEnterObserver {
	@Override
	public void beforeEnter(BeforeEnterEvent event) {
		Dialog confirmDialog = new Dialog();
		Paragraph text = new Paragraph(getTranslation("Are_you_sure_to_log_off?"));

		Button confirmButton = new Button(getTranslation("Confirm"), e -> {
			VaadinSession.getCurrent().getSession().invalidate();
			confirmDialog.close();
		});
		confirmButton.addClickShortcut(Key.ENTER);

		Button cancelButton = new Button(getTranslation("Cancel"), e -> {
			History history = UI.getCurrent().getPage().getHistory();
			history.back();
			confirmDialog.close();
		});

		var buttons = new HorizontalLayout(confirmButton, cancelButton);
		var messagePlusButtons = new VerticalLayout(text, buttons);
		messagePlusButtons.setAlignItems(FlexComponent.Alignment.END);
		confirmDialog.add(messagePlusButtons);
		confirmDialog.open();
	}
}
