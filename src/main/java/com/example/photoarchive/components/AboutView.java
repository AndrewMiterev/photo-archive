package com.example.photoarchive.components;

import com.example.photoarchive.services.FileMetaService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.HtmlComponent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.Cursor;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.PlotOptionsPie;
import com.vaadin.flow.component.charts.model.Tooltip;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.internal.MessageDigestUtil;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

@Log4j2
@Route(value = "", layout = MainAppLayout.class)
@RouteAlias(value = "home", layout = MainAppLayout.class)
@RouteAlias(value = "home1", layout = MainAppLayout.class)
@AnonymousAllowed
public class AboutView extends VerticalLayout {

	private final FileMetaService service;

	public AboutView(FileMetaService service) {
		this.service = service;

		setSpacing(false);
		add(new HorizontalLayout(
					new Image("https://icons.iconarchive.com/icons/thiago-silva/palm/128/Photos-icon.png", "Logo") {{
						setMaxWidth("100px");
						getStyle().set("object-fit", "contain");
					}},
					new H2("Welcome to Family-Photo-Archive!") {{
						getStyle().set("margin", "0");
					}}
			) {{
				setDefaultVerticalComponentAlignment(Alignment.CENTER);
			}},
				new H3("This program is designed to maintain a family photo archive"),
				new H4("Accumulates photos by:"),
				new UnorderedList(
						new ListItem("Drag-and-drop in any browser into the program window"),
						new ListItem("Subscriptions to the WhatsApp or Telegram family group"),
						new ListItem("Initial scanning (by contacting the developers) of the catalog on a physical disk or USB drive")
				),
				new Div()
		);
		add(new H4("Debug section"));
		add(new HorizontalLayout(
				new Button("Wow 1!") {{
					addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
					addClickListener(event -> {
						Notification.show("Wow pressed!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
						var statusStatistics = service.getPhotosCountByStatus();

						Chart chart = new Chart(ChartType.PIE);
						Configuration conf = chart.getConfiguration();
						conf.setTitle("Distribution of photos by status");

						Tooltip tooltip = new Tooltip();
						conf.setTooltip(tooltip);

						PlotOptionsPie options = new PlotOptionsPie();
//						options.setInnerSize("60%"); // бублик с дыркой
//						options.setSize("75%");  // Default
//						options.setCenter("50%", "50%"); // Default
						options.setAllowPointSelect(true);
						options.setCursor(Cursor.POINTER);
						options.setShowInLegend(true);
						conf.setPlotOptions(options);

						DataSeries series = new DataSeries();
						conf.addSeries(series);
						series.setName("Photos count");
						statusStatistics.forEach(s -> {
							var name = Objects.isNull(s.getKey())? "In permanent storage": s.getKey();
							var item = new DataSeriesItem(name, s.getValue());
							if (Objects.isNull(s.getKey())) item.setSliced(true);
							series.add(item);
						});

						add(chart);
					});
				}},

				new Button("Wow 2!") {{
					addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
					addClickListener(event -> {
						Notification.show("Wow pressed!").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
						var statusStatistics = service.getPhotosCountByMime();
					});
				}}
		));
		add(new Button("FUTURE: to show generated text file", event -> {
			final StreamResource resource = new StreamResource("foo.txt",
					() -> new ByteArrayInputStream("foo".getBytes()));
			final StreamRegistration registration = VaadinSession.getCurrent().getResourceRegistry().registerResource(resource);
			UI.getCurrent().getPage().setLocation(registration.getResourceUri());
		}));
	}

	/**
	 * Открыть новый компонент Имаже и Текст
	 * до конца файла
	 */

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
				mimeType, Arrays.toString(MessageDigestUtil.sha256(stream.toString())));
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
