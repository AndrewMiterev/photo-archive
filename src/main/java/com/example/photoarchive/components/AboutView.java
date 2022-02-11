package com.example.photoarchive.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.dialog.DialogVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.UploadI18N;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import elemental.json.Json;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Log4j2
@Route(value = "", layout = MainAppLayout.class)
@RouteAlias(value = "home", layout = MainAppLayout.class)
@RouteAlias(value = "home1", layout = MainAppLayout.class)
@AnonymousAllowed
public class AboutView extends Composite<VerticalLayout> implements HasComponents {

	public AboutView() {
		Div output = new Div();

		add(new H2("Welcome to Photo-Archive"));

		Button button = new Button("Click me", event -> {
			final StreamResource resource = new StreamResource("foo.txt",
					() -> new ByteArrayInputStream("foo".getBytes()));
			final StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry().registerResource(resource);
			UI.getCurrent().getPage().setLocation(registration.getResourceUri());
		});
		add(button);
		Button sessions = new Button("Sessions") {{
			addClickListener(e -> {
//				var aaa = VaadinSession.getAllSessions(HttpSession);
				var aaa = VaadinSession.getCurrent().getUIs();
				aaa.forEach(s -> log.debug("UI {}",s.getSession()));
				aaa.forEach(s -> s.getSession().getSession().setMaxInactiveInterval(10));
			});
		}};
		add(sessions);

		var buttonTest = new Button("Test");
		buttonTest.addClickListener(e->{
			log.trace("test pressed");
			Dialog dialog = new Dialog();
			dialog.getElement().setAttribute("aria-label", "Create new employee");
			dialog.addThemeVariants(DialogVariant.LUMO_NO_PADDING);
			add(dialog);
			dialog.open();
		});
		add(buttonTest);


//		UI.getCurrent().setPollInterval(1000);
//		VaadinSession.getCurrent().getSession().setMaxInactiveInterval(10);
	}

	private Component createComponent(String mimeType, String fileName,
									  InputStream stream) {
		log.warn("MIME {} filename {}", mimeType, fileName);
		if (mimeType.startsWith("text") || mimeType.startsWith("application/octet-stream")) {
			return createTextComponent(stream);
		} else if (mimeType.startsWith("image")) {
			Image image = new Image();
			try {

				byte[] bytes = IOUtils.toByteArray(stream);
				image.getElement().setAttribute("src", new StreamResource(
						fileName, () -> new ByteArrayInputStream(bytes)));
				try (ImageInputStream in = ImageIO.createImageInputStream(
						new ByteArrayInputStream(bytes))) {
					final Iterator<ImageReader> readers = ImageIO
							.getImageReaders(in);
					if (readers.hasNext()) {
						ImageReader reader = readers.next();
						try {
							reader.setInput(in);
							image.setWidth(reader.getWidth(0) + "px");
							image.setHeight(reader.getHeight(0) + "px");
						} finally {
							reader.dispose();
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			return image;
		}
		Div content = new Div();
		String text = String.format("Mime type: '%s'\nSHA-256 hash: '%s'",
				mimeType, MessageDigestUtil.sha256(stream.toString()));
		content.setText(text);
		return content;

	}

	private Component createTextComponent(InputStream stream) {
		String text;
		try {
			text = IOUtils.toString(stream, StandardCharsets.UTF_8);
		} catch (IOException e) {
			text = "exception reading stream";
		}
		return new Text(text);
	}

	private void showOutput(String text, Component content,
							HasComponents outputContainer) {
		HtmlComponent p = new HtmlComponent(Tag.P);
		p.getElement().setText(text);
		outputContainer.add(p);
		outputContainer.add(content);
	}
}
