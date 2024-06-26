package com.massivecraft.factions.zcore.util;

import cn.nukkit.item.Item;
import cn.nukkit.utils.TextFormat;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextUtil {
    public static final transient Pattern patternTag = Pattern.compile("<([a-zA-Z0-9_]*)>");
    /**
     * Using this function you transform a delta in milliseconds
     * to a String like "2 weeks from now" or "7 days ago".
     */
    public static final long millisPerSecond = 1000;

    // -------------------------------------------- //
    // Top-level parsing functions.
    // -------------------------------------------- //
    public static final long millisPerMinute = 60 * millisPerSecond;
    public static final long millisPerHour = 60 * millisPerMinute;

    // -------------------------------------------- //
    // Tag parsing
    // -------------------------------------------- //
    public static final long millisPerDay = 24 * millisPerHour;
    public static final long millisPerWeek = 7 * millisPerDay;
    public static final long millisPerMonth = 31 * millisPerDay;

    // -------------------------------------------- //
    // Color parsing
    // -------------------------------------------- //
    public static final long millisPerYear = 365 * millisPerDay;
    private final static String titleizeLine = repeat("_", 52);
    private final static int titleizeBalance = -1;
    public Map<String, String> tags;

    // -------------------------------------------- //
    // Standard utils like UCFirst, implode and repeat.
    // -------------------------------------------- //

    public TextUtil() {
        this.tags = new HashMap<String, String>();
    }

    public static String replaceTags(String str, Map<String, String> tags) {
        StringBuffer ret = new StringBuffer();
        Matcher matcher = patternTag.matcher(str);
        while (matcher.find()) {
            String tag = matcher.group(1);
            String repl = tags.get(tag);
            if (repl == null) {
                matcher.appendReplacement(ret, "<" + tag + ">");
            } else {
                matcher.appendReplacement(ret, repl);
            }
        }
        matcher.appendTail(ret);
        return ret.toString();
    }

    public static String parseColor(String string) {
        string = parseColorAmp(string);
        string = parseColorAcc(string);
        string = parseColorTags(string);
        return string;
    }

    // -------------------------------------------- //
    // Material name tools
    // -------------------------------------------- //

    public static String parseColorAmp(String string) {
        string = string.replaceAll("(§([a-z0-9]))", "\u00A7$2");
        string = string.replaceAll("(&([a-z0-9]))", "\u00A7$2");
        string = string.replace("&&", "&");
        return string;
    }

    public static String parseColorAcc(String string) {
        return string.replace("`e", "")
                .replace("`r", TextFormat.RED.toString()).replace("`R", TextFormat.DARK_RED.toString())
                .replace("`y", TextFormat.YELLOW.toString()).replace("`Y", TextFormat.GOLD.toString())
                .replace("`g", TextFormat.GREEN.toString()).replace("`G", TextFormat.DARK_GREEN.toString())
                .replace("`a", TextFormat.AQUA.toString()).replace("`A", TextFormat.DARK_AQUA.toString())
                .replace("`b", TextFormat.BLUE.toString()).replace("`B", TextFormat.DARK_BLUE.toString())
                .replace("`p", TextFormat.LIGHT_PURPLE.toString()).replace("`P", TextFormat.DARK_PURPLE.toString())
                .replace("`k", TextFormat.BLACK.toString()).replace("`s", TextFormat.GRAY.toString())
                .replace("`S", TextFormat.DARK_GRAY.toString()).replace("`w", TextFormat.WHITE.toString());
    }

    // -------------------------------------------- //
    // Paging and chrome-tools like titleize
    // -------------------------------------------- //

    public static String parseColorTags(String string) {
        return string.replace("<empty>", "")
                .replace("<black>", "\u00A70")
                .replace("<navy>", "\u00A71")
                .replace("<green>", "\u00A72")
                .replace("<teal>", "\u00A73")
                .replace("<red>", "\u00A74")
                .replace("<purple>", "\u00A75")
                .replace("<gold>", "\u00A76")
                .replace("<silver>", "\u00A77")
                .replace("<gray>", "\u00A78")
                .replace("<blue>", "\u00A79")
                .replace("<lime>", "\u00A7a")
                .replace("<aqua>", "\u00A7b")
                .replace("<rose>", "\u00A7c")
                .replace("<pink>", "\u00A7d")
                .replace("<yellow>", "\u00A7e")
                .replace("<white>", "\u00A7f");
    }

    public static String upperCaseFirst(String string) {
        return string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    public static String implode(List<String> list, String glue) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            if (i != 0) {
                ret.append(glue);
            }
            ret.append(list.get(i));
        }
        return ret.toString();
    }

    public static String repeat(String s, int times) {
        if (times <= 0) return "";
        else return s + repeat(s, times - 1);
    }

    // -------------------------------------------- //
    // Describing Time
    // -------------------------------------------- //

    public static String getMaterialName(Item material) {
        return material.getName().replace('_', ' ').toLowerCase();
    }

    public static String getMaterialName(String materialId) {
        return getMaterialName(Item.get(materialId));
    }

    public static String getTimeDeltaDescriptionRelNow(long millis) {
        double absmillis = (double) Math.abs(millis);
        String agofromnow = "from now";
        String unit;
        long num;
        if (millis <= 0) {
            agofromnow = "ago";
        }

        // We use a factor 3 below for a reason... why do you think?
        // Answer: it is a way to make our round of error smaller.
        if (absmillis < 3 * millisPerSecond) {
            unit = "milliseconds";
            num = (long) (absmillis);
        } else if (absmillis < 3 * millisPerMinute) {
            unit = "seconds";
            num = (long) (absmillis / millisPerSecond);
        } else if (absmillis < 3 * millisPerHour) {
            unit = "minutes";
            num = (long) (absmillis / millisPerMinute);
        } else if (absmillis < 3 * millisPerDay) {
            unit = "hours";
            num = (long) (absmillis / millisPerHour);
        } else if (absmillis < 3 * millisPerWeek) {
            unit = "days";
            num = (long) (absmillis / millisPerDay);
        } else if (absmillis < 3 * millisPerMonth) {
            unit = "weeks";
            num = (long) (absmillis / millisPerWeek);
        } else if (absmillis < 3 * millisPerYear) {
            unit = "months";
            num = (long) (absmillis / millisPerMonth);
        } else {
            unit = "years";
            num = (long) (absmillis / millisPerYear);
        }

        return "" + num + " " + unit + " " + agofromnow;
    }

    public static String getBestStartWithCI(Collection<String> candidates, String start) {
        String ret = null;
        int best = 0;

        start = start.toLowerCase();
        int minlength = start.length();
        for (String candidate : candidates) {
            if (candidate.length() < minlength) continue;
            if (!candidate.toLowerCase().startsWith(start)) continue;

            // The closer to zero the better
            int lendiff = candidate.length() - minlength;
            if (lendiff == 0) {
                return candidate;
            }
            if (lendiff < best || best == 0) {
                best = lendiff;
                ret = candidate;
            }
        }
        return ret;
    }

    public String parse(String str, Object... args) {
        return String.format(this.parse(str), args);
    }

    public String parse(String str) {
        return this.parseTags(parseColor(str));
    }

    public String parseTags(String str) {
        return replaceTags(str, this.tags);
    }

    public String titleize(String str) {
        String center = ".[ " + parseTags("<l>") + str + parseTags("<a>") + " ].";
        int centerlen = TextFormat.clean(center).length();
        int pivot = titleizeLine.length() / 2;
        int eatLeft = (centerlen / 2) - titleizeBalance;
        int eatRight = (centerlen - eatLeft) + titleizeBalance;

        if (eatLeft < pivot)
            return parseTags("<a>") + titleizeLine.substring(0, pivot - eatLeft) + center + titleizeLine.substring(pivot + eatRight);
        else
            return parseTags("<a>") + center;
    }

    // -------------------------------------------- //
    // String comparison
    // -------------------------------------------- //

	/*private static int commonStartLength(String a, String b)
	{
		int len = a.length() < b.length() ? a.length() : b.length();
		int i;
		for (i = 0; i < len; i++)
		{
			if (a.charAt(i) != b.charAt(i)) break;
		}
		return i;
	}*/

    public ArrayList<String> getPage(List<String> lines, int pageHumanBased, String title) {
        ArrayList<String> ret = new ArrayList<String>();
        int pageZeroBased = pageHumanBased - 1;
        int pageheight = 9;
        int pagecount = (lines.size() / pageheight) + 1;

        ret.add(this.titleize(title + " " + pageHumanBased + "/" + pagecount));

        if (pagecount == 0) {
            ret.add(this.parseTags("<i>Sorry. No Pages available."));
            return ret;
        } else if (pageZeroBased < 0 || pageHumanBased > pagecount) {
            ret.add(this.parseTags("<i>Invalid page. Must be between 1 and " + pagecount));
            return ret;
        }

        int from = pageZeroBased * pageheight;
        int to = from + pageheight;
        if (to > lines.size()) {
            to = lines.size();
        }

        ret.addAll(lines.subList(from, to));

        return ret;
    }
}
