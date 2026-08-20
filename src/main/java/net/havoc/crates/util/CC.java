package net.havoc.crates.util;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Colour translation helper. Supports legacy '&' codes and &#RRGGBB hex codes.
 */
public final class CC {

    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private CC() {
    }

    public static String translate(String input) {
        if (input == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher(input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(ChatColor.of("#" + hex).toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes('&', buffer.toString());
    }

    public static List<String> translate(List<String> input) {
        List<String> out = new ArrayList<>();
        if (input == null) {
            return out;
        }
        for (String line : input) {
            out.add(translate(line));
        }
        return out;
    }

    public static String strip(String input) {
        return ChatColor.stripColor(translate(input));
    }
}
