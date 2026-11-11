package org.levimc.launcher.core.ai;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Removes common credential forms before user text is retained in the in-memory conversation. */
public final class AIPrivacySanitizer {
    private static final String REDACTED = "[REDACTED]";
    private static final Pattern AUTHORIZATION = Pattern.compile(
            "(?i)(authorization\\s*[:=]\\s*(?:bearer\\s+|basic\\s+)?|bearer\\s+)([^\\s,;\\\"']+)");
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)((?:access[_-]?token|refresh[_-]?token|id[_-]?token|xsts[_-]?token|device[_-]?token|"
                    + "password|passwd|secret|credential|client[_-]?secret|api[ _-]?key|cookie|set-cookie|"
                    + "authorization[_-]?code|auth[_-]?code|oauth[_-]?code|code)"
                    + "\\s*[\\\"']?\\s*[:=]\\s*[\\\"']?)([^\\s,;\\\"'}]+)");
    private static final Pattern XBOX_TOKEN = Pattern.compile("(?i)(XBL3\\.0\\s+x=[^;\\s]+;)([^\\s]+)");
    private static final Pattern JWT = Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\b");
    private static final Pattern CURSEFORGE_KEY = Pattern.compile("\\$2a\\$10\\$[A-Za-z0-9./]{40,}");
    private static final Pattern COMMON_API_TOKEN = Pattern.compile(
            "(?i)\\b(?:sk-[A-Za-z0-9_-]{20,}|gh[pousr]_[A-Za-z0-9]{20,}|AIza[A-Za-z0-9_-]{30,})\\b");

    private AIPrivacySanitizer() {}

    public static String sanitize(String input) {
        if (input == null || input.isEmpty()) return "";
        String value = replaceGroup(AUTHORIZATION, input, 1);
        value = replaceGroup(SECRET_FIELD, value, 1);
        value = replaceGroup(XBOX_TOKEN, value, 1);
        value = JWT.matcher(value).replaceAll(REDACTED);
        value = CURSEFORGE_KEY.matcher(value).replaceAll(REDACTED);
        return COMMON_API_TOKEN.matcher(value).replaceAll(REDACTED);
    }

    private static String replaceGroup(Pattern pattern, String input, int prefixGroup) {
        Matcher matcher = pattern.matcher(input);
        StringBuffer output = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(output,
                    Matcher.quoteReplacement(matcher.group(prefixGroup) + REDACTED));
        }
        matcher.appendTail(output);
        return output.toString();
    }
}
