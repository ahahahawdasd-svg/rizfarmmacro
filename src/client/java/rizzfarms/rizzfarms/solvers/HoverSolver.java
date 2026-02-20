package rizzfarms.rizzfarms.solvers;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import rizzfarms.rizzfarms.core.ChatGameSolver;
import rizzfarms.rizzfarms.core.MathSolver;
import rizzfarms.rizzfarms.core.SolveAttempt;
import rizzfarms.rizzfarms.util.Timing;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat game:
 * "Hover over this, be the first one to type it to win!"
 *
 * The real payload is hidden in a hover event (SHOW_TEXT) on a sibling component.
 * Payload can be either:
 * - raw token text (e.g. "mSwl")
 * - a simple arithmetic expression (e.g. "3 * 4 = ???")
 */
public final class HoverSolver implements ChatGameSolver {

    private static final Pattern HOVER_PROMPT = Pattern.compile("\\bhover\\s+over\\b", Pattern.CASE_INSENSITIVE);

    // Text payloads tend to be a single token, but keep this flexible.
    private static final Pattern TOKEN = Pattern.compile("([A-Za-z0-9_]+)");

    // Conservative: only treat as math if it starts with a digit.
    private static final Pattern MATHISH = Pattern.compile("([0-9][0-9+\\-*/().xX×÷\\s?=]{1,})");

    @Override
    public SolveAttempt trySolve(String prompt) {
        return null; // Requires Component to read the hover payload.
    }

    @Override
    public SolveAttempt trySolve(String prompt, Component message) {
        if (prompt == null || !HOVER_PROMPT.matcher(prompt).find()) return null;
        if (message == null) return null;

        String hover = findFirstHoverText(message);
        if (hover == null || hover.isBlank()) return null;

        String payload = firstNonEmptyLine(hover);
        if (payload == null || payload.isBlank()) return null;

        // 1) Equation variant
        String mathCandidate = extractMathCandidate(payload);
        if (mathCandidate != null) {
            String ans = MathSolver.solve(mathCandidate);
            if (ans != null) {
                return SolveAttempt.solved(ans, Timing.tMath(mathCandidate), "hover-math");
            }
        }

        // 2) Plain token variant
        String token = extractLastToken(payload);
        if (token == null || token.isBlank()) return null;

        return SolveAttempt.solved(token, Timing.tSequence(token.length()), "hover-text");
    }

    private static String extractMathCandidate(String s) {
        if (s == null) return null;
        if (s.indexOf('+') < 0 && s.indexOf('-') < 0 && s.indexOf('*') < 0 && s.indexOf('/') < 0
                && s.indexOf('×') < 0 && s.indexOf('÷') < 0 && s.indexOf('x') < 0 && s.indexOf('X') < 0
                && s.indexOf('=') < 0) {
            return null;
        }

        Matcher m = MATHISH.matcher(s);
        if (m.find()) return m.group(1).trim();
        return null;
    }

    private static String extractLastToken(String s) {
        if (s == null) return null;
        Matcher m = TOKEN.matcher(s);
        String last = null;
        while (m.find()) last = m.group(1);
        return last;
    }

    private static String firstNonEmptyLine(String s) {
        if (s == null) return null;
        String[] lines = s.split("\\R");
        for (String line : lines) {
            if (line != null) {
                String t = line.trim();
                if (!t.isEmpty()) return t;
            }
        }
        return s.trim();
    }

    private static String findFirstHoverText(Component component) {
        if (component == null) return null;

        Style style = component.getStyle();
        if (style != null) {
            HoverEvent hover = style.getHoverEvent();
            if (hover != null && hover.action() == HoverEvent.Action.SHOW_TEXT && hover instanceof HoverEvent.ShowText ht) {
                Component c = ht.value();
                if (c != null) {
                    String out = c.getString();
                    if (out != null && !out.isBlank()) return out;
                }
            }
        }

        for (Component child : component.getSiblings()) {
            String found = findFirstHoverText(child);
            if (found != null) return found;
        }

        return null;
    }
}
