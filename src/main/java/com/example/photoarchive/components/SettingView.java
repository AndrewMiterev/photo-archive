package com.example.photoarchive.components;

import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import javax.annotation.security.PermitAll;
import java.util.Locale;

@Route(value = "setting", layout = MainAppLayout.class)
@PermitAll
public class SettingView extends VerticalLayout
//        implements LocaleChangeObserver
{
//    private final Button button = new Button();

//    @Override
//    public void localeChange(LocaleChangeEvent localeChangeEvent) {
//        button.setText(getTranslation("btn.key"));
//    }

	public SettingView(I18NProvider provider) {

//        button.getStyle();
//        button.setText(getTranslation("btn.key"));
//        button.addClickListener(e -> Notification.show(getTranslation("clicked")));

		ComboBox<Locale> localeComboBox = new ComboBox<>();
		localeComboBox.setItems(provider.getProvidedLocales());
		localeComboBox.setValue(VaadinSession.getCurrent().getLocale());
		localeComboBox.addValueChangeListener(e -> {
			VaadinSession.getCurrent().setLocale(e.getValue());
		});
		add(localeComboBox);
//        add(button);
	}

}
