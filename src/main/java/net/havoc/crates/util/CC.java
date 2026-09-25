/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  java.lang.CharSequence
 *  java.lang.Object
 *  java.lang.String
 *  java.lang.StringBuffer
 *  java.util.ArrayList
 *  java.util.List
 *  java.util.regex.Matcher
 *  java.util.regex.Pattern
 *  net.md_5.bungee.api.ChatColor
 */
package net.havoc.crates.util;

import java.lang.CharSequence;
import java.lang.Object;
import java.lang.String;
import java.lang.StringBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.md_5.bungee.api.ChatColor;

public final class CC {
    private static final Pattern HEX_PATTERN = Pattern.compile((String)"&#([A-Fa-f0-9]{6})");

    private CC() {
    }

    public static String translate(String input) {
        if (input == null) {
            return "";
        }
        Matcher matcher = HEX_PATTERN.matcher((CharSequence)input);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            String hex = matcher.group(1);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement((String)ChatColor.of((String)("#" + hex)).toString()));
        }
        matcher.appendTail(buffer);
        return ChatColor.translateAlternateColorCodes((char)'&', (String)buffer.toString());
    }

    public static List<String> translate(List<String> input) {
        ArrayList out = new ArrayList();
        if (input == null) {
            return out;
        }
        for (String line : input) {
            out.add(CC.translate(line));
        }
        return out;
    }

    public static String strip(String input) {
        return ChatColor.stripColor((String)CC.translate(input));
    }
}
