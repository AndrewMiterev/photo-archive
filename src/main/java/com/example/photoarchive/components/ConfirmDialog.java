package com.example.photoarchive.components;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Synchronize;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.shared.Registration;
import org.checkerframework.common.subtyping.qual.Bottom;

import java.util.Objects;

public class ConfirmDialog extends Dialog {
	private final H2 dialogTitle = new H2("Confirmation dialog");

	public void setTitle(String title) {
		dialogTitle.setText(title);
		setAriaLabel(dialogTitle.getText());
	}

	private final Button okButton = new Button("Ok", e -> close()) {{
		addThemeVariants(ButtonVariant.LUMO_PRIMARY);
	}};

	public void setConfirmText(String text) {
		okButton.setText(text);
	}

	public Registration addConfirmListener(ComponentEventListener<ConfirmDialog.ConfirmEvent> listener) {
		return okButton.addClickListener(e -> listener.onComponentEvent(new ConfirmEvent(this)));
	}

	public void setConfirmTheme(ButtonVariant... variants) {
		okButton.addThemeVariants(variants);
	}

	private final Button cancelButton = new Button("Cancel", e -> close()) {{
		addThemeVariants(ButtonVariant.LUMO_TERTIARY);
	}};

	public void setCancelText(String text) {
		cancelButton.setText(text);
	}

	public Registration addCancelListener(ComponentEventListener<ConfirmDialog.CancelEvent> listener) {
		return cancelButton.addClickListener(e -> listener.onComponentEvent(new CancelEvent(this)));
	}

	public void setCancelTheme(ButtonVariant... variants) {
		cancelButton.addThemeVariants(variants);
	}

	private final VerticalLayout mainContent = new VerticalLayout();

	public void setMainContent(Component... components) {
		mainContent.removeAll();
		mainContent.add(components);
	}

	public ConfirmDialog() {
		setAriaLabel(dialogTitle.getText());

		dialogTitle.getStyle().set("margin", ".5em").set("font-size", "1.5em")
				.set("font-weight", "bold");

		Header header = new Header(dialogTitle);
		header.addClassName("draggable");

		header.getStyle()
				.set("border-bottom", "1px solid var(--lumo-contrast-20pct)")
				.set("cursor", "move");

		Scroller scroller = new Scroller(mainContent);
		scroller.setMaxHeight("50vh");
		scroller.setMaxWidth("80vw");

		HorizontalLayout footerLayout = new HorizontalLayout(cancelButton, okButton) {{
			setJustifyContentMode(JustifyContentMode.END);
		}};
		Footer footer = new Footer(footerLayout);
		VerticalLayout dialogContent = new VerticalLayout(header, scroller, footer);
//		dialogContent.setPadding(false);
		dialogContent.setSpacing(false);
//		dialogContent.getStyle().remove("width");
		dialogContent.setAlignItems(FlexComponent.Alignment.STRETCH);
//		dialogContent.setClassName("dialog-no-padding-example-overlay");

		add(dialogContent);
		addThemeVariants(DialogVariant.LUMO_NO_PADDING);
		setDraggable(true);
	}

	public static class CancelEvent extends ComponentEvent<ConfirmDialog> {
		public CancelEvent(ConfirmDialog source) {
			super(source, true);
		}
	}

	public static class RejectEvent extends ComponentEvent<ConfirmDialog> {
		public RejectEvent(ConfirmDialog source) {
			super(source, true);
		}
	}

	public static class ConfirmEvent extends ComponentEvent<ConfirmDialog> {
		public ConfirmEvent(ConfirmDialog source) {
			super(source, true);
		}
	}
}
