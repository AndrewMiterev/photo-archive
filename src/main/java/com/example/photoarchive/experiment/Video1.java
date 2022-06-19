package com.example.photoarchive.experiment;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.HtmlContainer;
import com.vaadin.flow.component.PropertyDescriptor;
import com.vaadin.flow.component.PropertyDescriptors;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.server.StreamResource;

@Tag("video1")
public class Video1 extends HtmlContainer implements ClickNotifier<com.vaadin.flow.component.html.Image> {

	private static final PropertyDescriptor<String, String> srcDescriptor = PropertyDescriptors
			.attributeWithDefault("src", "");

	public Video1() {
		super();
		getElement().setProperty("controls", true);
	}

	public Video1(StreamResource resource) {
		super();
		getElement().setAttribute("src", resource);
	}

	public void setSrc(StreamResource resource) {
		getElement().setAttribute("src", resource);
	}

	public Video1(String src) {
		setSrc(src);
		getElement().setProperty("controls", true);
	}

	public String getSrc() {
		return get(srcDescriptor);
	}

	public void setSrc(String src) {
		set(srcDescriptor, src);
	}
}
