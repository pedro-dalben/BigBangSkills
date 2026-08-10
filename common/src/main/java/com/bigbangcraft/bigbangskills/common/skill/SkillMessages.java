package com.bigbangcraft.bigbangskills.common.skill;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

public final class SkillMessages {
    private static final String BASE = "com.bigbangcraft.bigbangskills.common.messages";
    private SkillMessages() {}

    public static Locale locale(String language) {
        if (language == null || language.isBlank()) return Locale.US;
        var parsed = Locale.forLanguageTag(language.replace('_', '-'));
        return parsed.getLanguage().isBlank() ? Locale.US : parsed;
    }

    public static String text(String key, Locale locale, Object... arguments) {
        ResourceBundle bundle;
        try { bundle = ResourceBundle.getBundle(BASE, locale == null ? Locale.US : locale); }
        catch (MissingResourceException ignored) { bundle = ResourceBundle.getBundle(BASE, Locale.US); }
        var template = bundle.containsKey(key) ? bundle.getString(key) : key;
        return arguments.length == 0 ? template : MessageFormat.format(template, arguments);
    }
}
