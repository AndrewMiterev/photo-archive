package com.example.photoarchive.components;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.services.ConfigProperties;
import com.example.photoarchive.services.SlideshowService;
import com.example.photoarchive.tools.OnlyOneJobExecutor;
import com.example.photoarchive.tools.RingRandomSequence;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.shared.Registration;
import lombok.extern.log4j.Log4j2;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.BiConsumer;

@Log4j2
@Route(value = "slideshow", layout = MainAppLayout.class)
@AnonymousAllowed
//public class SlideShow extends Composite<VerticalLayout> implements HasComponents, HasDynamicTitle {
public class SlideShow extends VerticalLayout {
	private final SlideshowService service;
	private final ConfigProperties config;

	private final Image image = new Image();
	private final Text photoDescription = new Text("");

	private final Div slideshowDiv = new Div();
	private Boolean fullScreen = false;

	private Registration registrationExecutionStopper;
	private final VaadinSession registrationExecutionStopperSession;
	private OnlyOneJobExecutor executor;

	private BiConsumer<StreamResource, Photo> consumer;
	private LocalDateTime timestamp;
	private RingRandomSequence sequence;

	private void reloadSequence() {
		if (Objects.nonNull(timestamp) && Objects.nonNull(sequence) && timestamp.isEqual(service.directoryIsLoadedAt()))
			return;
		sequence = service.makeSequence();
		timestamp = service.directoryIsLoadedAt();
//		log.debug("random sequence loaded. {}", sequence);
	}

	public SlideShow(SlideshowService service, ConfigProperties config) {
		this.service = service;
		this.config = config;

		setHeightFull();

		Div prev = new Div(iconForButton(VaadinIcon.CHEVRON_CIRCLE_LEFT_O));
		Div next = new Div(iconForButton(VaadinIcon.CHEVRON_CIRCLE_RIGHT_O));
		Div full = new Div(iconForButton(VaadinIcon.EXPAND_FULL));

		prev.setTitle(getTranslation("Previous"));
		prev.getStyle()
				.set("position", "absolute")
				.set("top", "0em")
				.set("left", "0em")
				.set("background-color", "rgba(0,0,0,.2)")
				.set("color", "white")
				.set("margin", ".5em")
				.set("padding", ".5em")
				.set("border-radius", "50%")
		;
		prev.addClickListener(e -> {
			service.previous(sequence, consumer);
			executor.runAfter(config.getSlideshowStartDelayAfterManual());
		});
		prev.addClickShortcut(Key.ARROW_LEFT);
		prev.addClickShortcut(Key.ARROW_DOWN);
		prev.addClickShortcut(Key.BACKSPACE);
		prev.addClickShortcut(Key.NUMPAD_SUBTRACT);

		next.setTitle(getTranslation("Next"));
		next.getStyle()
				.set("position", "absolute")
				.set("top", "0em")
				.set("right", "0em")
				.set("background-color", "rgba(0,0,0,.2)")
				.set("color", "white")
				.set("margin", ".5em")
				.set("padding", ".5em")
				.set("border-radius", "50%")
		;
		next.addClickListener(e -> {
			service.next(sequence, consumer);
			executor.runAfter(config.getSlideshowStartDelayAfterManual());
		});
		next.addClickShortcut(Key.ARROW_RIGHT);
		next.addClickShortcut(Key.ARROW_UP);
		next.addClickShortcut(Key.NUMPAD_ADD);

		full.setTitle(getTranslation("SlideShowToFullscreen"));
		full.getStyle()
				.set("position", "absolute")
				.set("top", "0em")
				.set("left", "50%")
				.set("transform", "translate(-50%, 0)")
				.set("background-color", "rgba(0,0,0,.2)")
				.set("color", "white")
				.set("margin", ".5em 0 0 0")
				.set("padding", ".5em")
				.set("border-radius", "50%")
		;

		full.addClickListener(e -> {
			fullScreen = !fullScreen;
			executor.runAfter(config.getSlideshowStartDelayAfterManual());
			setParametersForSlideshowDif();
			if (fullScreen) {
				full.removeAll();
				full.add(iconForButton(VaadinIcon.COMPRESS_SQUARE));
				full.setTitle(getTranslation("SlideShowToWindowed"));
			} else {
				full.removeAll();
				full.add(iconForButton(VaadinIcon.EXPAND_FULL));
				full.setTitle(getTranslation("SlideShowToFullscreen"));
			}
		});
		full.addClickShortcut(Key.ENTER);

		setParametersForSlideshowDif();

		Div cardWithControlDiv = new Div();
		cardWithControlDiv.getStyle()
				.set("position", "relative")
				.set("width", "100%")
				.set("height", "100%")
		;

		cardWithControlDiv.add(image);
		image.getStyle()
				.set("object-fit", "cover")
				.set("width", "100%")
				.set("height", "100%")
		;

		var descriptionDiv = new Div(photoDescription);
		descriptionDiv.getStyle()
				.set("position", "absolute")
				.set("bottom", "0em")
				.set("right", "0em")
				.set("background-color", "rgba(0,0,0,.4)")
				.set("color", "white")
				.set("margin", ".5em")
				.set("padding", ".5em")
				.set("border-radius", ".5em")
		;
		cardWithControlDiv.add(descriptionDiv);
		cardWithControlDiv.add(next);
		cardWithControlDiv.add(prev);
		cardWithControlDiv.add(full);

		Div refresh = new Div(iconForButton(VaadinIcon.REFRESH));
		refresh.getStyle()
				.set("position", "absolute")
				.set("top", "5em")
				.set("left", "50%")
				.set("transform", "translate(-50%, 0)")
				.set("background-color", "rgba(0,0,0,.2)")
				.set("color", "white")
				.set("margin", ".5em 0 0 0")
				.set("padding", ".5em")
				.set("border-radius", "50%")
		;
		refresh.addClickShortcut(Key.F10);
		refresh.addClickListener(e -> {
			service.current(sequence, consumer);
			service.reload();
		});
		cardWithControlDiv.add(refresh);
		slideshowDiv.add(cardWithControlDiv);

		registrationExecutionStopperSession = VaadinSession.getCurrent();
		registrationExecutionStopper = VaadinService.getCurrent().addUIInitListener(e -> {
			if (registrationExecutionStopperSession == e.getUI().getSession())
				stopTimerEngine();
		});

		Runnable doNextPhotoOnTimer = () -> {
			reloadSequence();
			service.next(sequence, consumer);
			executor.runAfter(config.getSlideshowDelayBetweenPhotos());
		};
		executor = new OnlyOneJobExecutor(doNextPhotoOnTimer);
		reloadSequence();
		if (service.isEmpty()) add(new H2("Photo-Archive is empty"));
		else add(slideshowDiv);
	}

	private void stopTimerEngine() {
		executor.stop();
		if (Objects.nonNull(registrationExecutionStopper)) {
			registrationExecutionStopper.remove();
			registrationExecutionStopper = null;
		}
	}

	@Override
	protected void onDetach(DetachEvent detachEvent) {
		stopTimerEngine();
	}

	@Override
	protected void onAttach(AttachEvent attachEvent) {
		executor.runAfter(config.getSlideshowDelayBetweenPhotos());
		consumer = (stream, photo) -> getUI().ifPresent(ui -> ui.access(() -> {
			image.getElement().setAttribute("src", stream)
					.setAttribute("alt", photo.getName())
					.setAttribute("title", Objects.isNull(photo.getTitle()) ? "" : photo.getTitle())
			;
			photoDescription.setText(photo.getTitle());
		}));
		service.reload();
		if (!service.isEmpty())
			service.current(sequence, consumer);
	}

	private Icon iconForButton(VaadinIcon viewIcon) {
		Icon icon = viewIcon.create();
		icon.setSize("2em");
		return icon;
	}

	private void setParametersForSlideshowDif() {
		if (fullScreen)
			slideshowDiv.getStyle()
					.set("position", "fixed")
					.set("left", "0")
					.set("top", "0")
					.set("bottom", "0")
					.set("right", "0")
					.set("z-index", "10")
					.set("margin", "0")
//                    .set("overflow", "auto")
					.set("box-sizing", "")
					.set("border-radius", "")
					.set("border", "")
					.set("overflow", "")
					.set("height", "")
					.set("width", "");
		else
			slideshowDiv.getStyle()
					.set("position", "")
					.set("left", "")
					.set("top", "")
					.set("bottom", "")
					.set("right", "")
					.set("z-index", "")
					.set("margin", "")
					.set("box-sizing", "border-box")
					.set("border-radius", "1em")
					.set("border", "solid 5px black")
					.set("overflow", "hidden")
					.set("height", "100%")
					.set("width", "100%");
	}
}
