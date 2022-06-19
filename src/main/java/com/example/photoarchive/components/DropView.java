package com.example.photoarchive.components;

import com.example.photoarchive.domain.entities.User;
import com.example.photoarchive.security.AuthenticatedUser;
import com.example.photoarchive.services.PhotoArchiveProcessor;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MultiFileMemoryBuffer;
import com.vaadin.flow.router.Route;
import elemental.json.Json;
import lombok.extern.log4j.Log4j2;

import javax.annotation.security.PermitAll;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Log4j2
@Route(value = "drop", layout = MainAppLayout.class)
@PermitAll
public class DropView extends VerticalLayout implements HasComponents {
	private final PhotoArchiveProcessor processorService;
	private final AuthenticatedUser securityService;

	private String getUsername() {
		return securityService.get().map(User::getUsername).orElse("");
	}

	public DropView(PhotoArchiveProcessor processorService, AuthenticatedUser securityService) {
		this.processorService = processorService;
		this.securityService = securityService;

		MultiFileMemoryBuffer buffer = new MultiFileMemoryBuffer();
		Upload upload = new Upload(buffer);
		upload.setUploadButton(new Button(getTranslation("Upload_files")));
		upload.setDropLabel(new Label(getTranslation("Drop_files")));
//		upload.setMaxFileSize(0);
//		upload.setMaxFiles(0);

//        ByteArrayOutputStream uploadBuffer = new ByteArrayOutputStream();
//        upload.setReceiver((fileName, mimeType) -> {
//            log.debug("!!! filename {} mimetype {}",fileName, mimeType);
//            return uploadBuffer;
//        });


		upload.addAllFinishedListener(e ->
				Executors.newScheduledThreadPool(1).schedule(() ->
						upload.getUI().ifPresent(ui ->
								ui.access(() ->
										upload.getElement().setPropertyJson("files", Json.createArray())
								)), 3, TimeUnit.SECONDS)
		);

		upload.addFinishedListener(e -> {
			String fileName = e.getFileName();
			Long contentLength = e.getContentLength();
			String mimeType = e.getMIMEType();

			Notification notification = Notification.show(getTranslation("File_uploaded") + fileName);
			notification.setPosition(Notification.Position.BOTTOM_CENTER);
			notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

			try (InputStream stream = buffer.getInputStream(fileName)) {
//				var added = processorService.storeNewFile("drop", stream, fileName, mimeType, contentLength, LocalDateTime.now(), getUsername());
//
//				Notification notification = Notification.show(getTranslation("File_uploaded") + fileName);
//				notification.setPosition(Notification.Position.BOTTOM_CENTER);
//				notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
//				if (!added) {
//					notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
//					notification.setDuration(notification.getDuration() * 3);
//				}
			} catch (IOException ex) {
				log.error("error on finished listener {{}}", ex.getMessage());
			}
		});

		add(upload);

		setAlignItems(Alignment.STRETCH);
		setWidthFull();
	}
}
