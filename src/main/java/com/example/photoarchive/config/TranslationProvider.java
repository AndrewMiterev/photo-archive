package com.example.photoarchive.config;

import com.vaadin.flow.i18n.I18NProvider;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.ResourceBundle;

@Log4j2
@Component
public class TranslationProvider implements I18NProvider {
    @Override
    public List<Locale> getProvidedLocales() {
        return List.of(
                new Locale("en"),
                new Locale("ru")
        );
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        try {
            final ResourceBundle bundle = ResourceBundle.getBundle("translation", locale);
            String value = bundle.getString(key);
            if (params.length > 0)
                value = MessageFormat.format(value, params);
            return value;
        } catch (MissingResourceException e) {
            log.warn("No translation parameter {} for locale {}", key, locale);
            return key;
        }
    }
}
