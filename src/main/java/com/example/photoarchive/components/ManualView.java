package com.example.photoarchive.components;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.experiment.BlobSourceVideo;
import com.example.photoarchive.experiment.Video;
import com.example.photoarchive.services.FileMetaService;
import com.example.photoarchive.services.FileService;
import com.example.photoarchive.services.PhotoArchiveProcessor;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.radiobutton.RadioGroupVariant;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.log4j.Log4j2;

import javax.annotation.security.PermitAll;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.BiConsumer;

@Route(value = "manual", layout = MainAppLayout.class)
@PermitAll
@AnonymousAllowed
@Log4j2
public class ManualView extends VerticalLayout {
	private final PhotoArchiveProcessor processor;
	private final FileMetaService metaService;
	private final FileService fileService;

	private final List<Photo> list;
	private int photoNumber = 0;
	private Photo photo;
	private BiConsumer<StreamResource, Photo> consumer;

	public ManualView(PhotoArchiveProcessor processor, FileMetaService metaService, FileService fileService) {
		this.processor = processor;
		this.metaService = metaService;
		this.fileService = fileService;

		list = metaService.getPhotosWithStatus("manual");
		var countManual = list.size();

		if (countManual == 0) {
			add(new Text("Mo photos to manual processing"));
			return;
		}
		photo = list.get(photoNumber);

		var manuals = new Div();
		add(manuals);
		manuals.setWidthFull();


		Text count = new Text("Count photos to manual: %s".formatted(countManual));
		manuals.add(count);

		HorizontalLayout photoCard = new HorizontalLayout();
		photoCard.setAlignItems(Alignment.CENTER);
		photoCard.setWidthFull();
		manuals.add(photoCard);

		Div imageDiv = new Div();
		imageDiv.getStyle()
				.set("position", "relative")
				.set("width", "50vw")
				.set("height", "50vh");

		Image image = new Image();
		image.getStyle()
				.set("object-fit", "contain")
				.set("width", "100%")
				.set("height", "100%")
		;
		Video video = new BlobSourceVideo();
		video.getStyle()
				.set("object-fit", "contain")
				.set("width", "300px")
//				.set("height", "100%")
		;
		video.setControls(true);
		imageDiv.add(image);
		var imageDescription = new VerticalLayout();
		photoCard.add(imageDiv, video, imageDescription);

		var originalPhotoNameText = new Label("Original photo name");
		var originalPhotoNameSpan = new Span();
		imageDescription.add(new HorizontalLayout(originalPhotoNameText, originalPhotoNameSpan));

		var originalPhotoSourceText = new Label("Original photo source");
		var originalPhotoSourceSpan = new Span();
		imageDescription.add(new HorizontalLayout(originalPhotoSourceText, originalPhotoSourceSpan));

		var originalPhotoMimeText = new Label("Original photo source");
		var originalPhotoMimeSpan = new Span();
		imageDescription.add(new HorizontalLayout(originalPhotoMimeText, originalPhotoMimeSpan));

		var originalPhotoDateText = new Label("Original photo source");
		var originalPhotoDateSpan = new Span();
		imageDescription.add(new HorizontalLayout(originalPhotoDateText, originalPhotoDateSpan));

		consumer = (stream, photo) -> getUI().ifPresent(ui -> ui.access(() -> {
			if(photo.getOriginal().getMime().equalsIgnoreCase("video/quicktime"))
				video.setSource(stream);
			else
			image.getElement().setAttribute("src", stream)
					.setAttribute("alt", photo.getName())
					.setAttribute("title", photo.getName())
			;
			originalPhotoNameSpan.setText(photo.getOriginal().getName());
			originalPhotoSourceSpan.setText(photo.getOriginal().getSource());
			originalPhotoMimeSpan.setText(photo.getOriginal().getMime());
			originalPhotoDateSpan.setText(photo.getOriginal().getDate().format(DateTimeFormatter.ISO_DATE));
		}));

		HorizontalLayout moveToLayout = new HorizontalLayout();
		manuals.add(moveToLayout);
		moveToLayout.setAlignItems(Alignment.CENTER);

		RadioButtonGroup<String> toCatalogsChoice = new RadioButtonGroup<>();
		toCatalogsChoice.addThemeVariants(RadioGroupVariant.LUMO_VERTICAL);
		toCatalogsChoice.setLabel("Move photo to");
		toCatalogsChoice.setItems("Cataloge Prognoz1", "Cataloge prognoz2", "Manual");
		Button moveButton = new Button("Move");
		moveToLayout.add(toCatalogsChoice, moveButton);

		HorizontalLayout buttons = new HorizontalLayout();
		buttons.setAlignItems(Alignment.END);
		manuals.add(buttons);
		Button nextButton = new Button("Next", e -> {
			photoNumber++;
			if (photoNumber >= list.size()) photoNumber = 0;
			photo=list.get(photoNumber);
			fileService.callConsumerOnLoad(consumer, photo, fileService.readPhotoDataAsync(photo));
		});
		buttons.add(nextButton);

		fileService.callConsumerOnLoad(consumer, photo, fileService.readPhotoDataAsync(photo));
	}
}