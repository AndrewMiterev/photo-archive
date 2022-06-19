package com.example.photoarchive.components;

import com.example.photoarchive.services.FileMetaService;
import com.example.photoarchive.services.PhotoArchiveProcessor;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.extern.log4j.Log4j2;

import javax.annotation.security.RolesAllowed;
import java.io.IOException;

@Log4j2
@Route(value = "files", layout = MainAppLayout.class)
@RolesAllowed("admin")
public class FilesView extends VerticalLayout {
	private final FileMetaService service;
	private final PhotoArchiveProcessor processor;

	public FilesView(FileMetaService service, PhotoArchiveProcessor processor) {
		this.service = service;
		this.processor = processor;

		ConfirmDialog dialog = new ConfirmDialog();
		dialog.setTitle("Clear all collections?");
		dialog.setMainContent(new Paragraph("Will be clear those collections: photos, protocols"));

		dialog.setConfirmText("Delete");
		dialog.addConfirmListener(e -> {
			log.trace("clear all collection confirmed");
			processor.processClearAllCollections();
		});
		dialog.setConfirmTheme(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		dialog.setCancelText("Cancel");

		var buttonClearCollections = new Button("Clear all collections");
		buttonClearCollections.addClickListener(e -> {
			log.trace("clear all collections pressed");
			dialog.open();
		});

		buttonClearCollections.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
		setAlignSelf(Alignment.END, buttonClearCollections);
		add(buttonClearCollections);

		var buttonCalculateHash = new Button("Calculate HASH");
		buttonCalculateHash.addClickListener(e -> {
			log.trace("calculate hash pressed");
			var list = service.getPhotosWithStatus("hash");
			list.forEach(p -> {
				try {
					processor.processFileHash(p.getHash());
				} catch (IOException ignored) {
				}
			});
		});
		add(buttonCalculateHash);

		var buttonExtractGeo = new Button("Extract Exif information");
		buttonExtractGeo.addClickListener(e -> {
			log.trace("extract Exif pressed");
			var list = service.getPhotosWithStatus("exif");
			list.forEach(processor::processExtractExif);
		});
		add(buttonExtractGeo);

		var buttonGoogle = new Button("Obtain GEO code");
		buttonGoogle.addClickListener(e -> {
			log.trace("obtain geocodes pressed");
			var list = service.getPhotosWithStatus("google");
//            list.stream().findFirst().ifPresent(p->processor.processResolveGeocodes(p));
			list.forEach(processor::processObtainGeocode);
		});
		add(buttonGoogle);

		var buttonParse = new Button("Resolve GEO names from GEO codes");
		buttonParse.addClickListener(e -> {
			log.trace("resolve GEO names pressed");
			var list = service.getPhotosWithStatus("resolve");
//            list.stream().filter(p -> p.getHash().equalsIgnoreCase("e71106ec2a2534a3d0348c33de1b2d86")).findFirst().ifPresent(p -> processor.processResolveGeocode(p));
			list.forEach(processor::processResolveGeocode);
		});
		add(buttonParse);

		var buttonPredict = new Button("Predict names");
		buttonPredict.addClickListener(e -> {
			log.trace("predict names pressed");
			var list = service.getPhotosWithStatus("predict");
			list.forEach(processor::processPredict);
		});
		add(buttonPredict);

		var buttonMove = new Button("Move to Permanently space");
		buttonMove.addClickListener(e -> {
			log.trace("move to permanently place pressed");
			var list = service.getPhotosWithStatus("move");
			list.forEach(processor::processMove);
		});
		add(buttonMove);

		add(new Button("Re-calculate hash and check availability") {{
			addClickListener(e -> {
				log.trace("re-calculate files pressed");
				processor.processCheckCollections();
			});
		}});

		add(new Button("Re-scan permanent folder") {{
			addClickListener(e -> {
				log.trace("re-scan permanent folder pressed");
				processor.processCheckPermanentFolder();
			});
		}});
	}
}
