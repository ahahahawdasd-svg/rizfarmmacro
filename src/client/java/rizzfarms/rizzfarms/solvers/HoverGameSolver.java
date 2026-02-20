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

public final class HoverGameSolver implements ChatGameSolver {

    private static final Pattern HOVER_PROMPT = Pattern.compile("Hover over this", Pattern.CASE_INSENSITIVE);

    @Override
    public SolveAttempt trySolve(String prompt) {
        return null; // Requires component
    }

    @Override
    public SolveAttempt trySolve(String prompt, Component message) {
        if (!HOVER_PROMPT.matcher(prompt).find()) return null;
        if (message == null) return null;

        String hidden = getHoverText(message);
        if (hidden == null || hidden.isBlank()) return null;

        // Try Math
        if (hidden.contains("=")) {
             // e.g. "3 * 4 = ???"
             // We can use MathSolver.solve() directly on the hidden text
             String ans = MathSolver.solve(hidden);
             if (ans != null) {
                 return SolveAttempt.solved(ans, Timing.tMath(hidden), "hover-math");
             }
        }

        // Try Text (m8wl)
        // Usually it's just the text.
        // Or maybe "Type: m8wl"? No, screenshot shows just "m8wl".
        // But we should verify it's not a math problem that failed to solve.
        // If it doesn't contain operators, it's likely text.
        
        // Simple heuristic: if it's short and no spaces, it's text.
        String trimmed = hidden.trim();
        if (!trimmed.contains(" ") && !trimmed.contains("=")) {
            return SolveAttempt.solved(trimmed, Timing.tSequence(trimmed.length()), "hover-text");
        }

        return null;
    }

    private String getHoverText(Component component) {
        Style style = component.getStyle();
        if (style != null) {
            HoverEvent hover = style.getHoverEvent();
            if (hover != null && hover.action() == HoverEvent.Action.SHOW_TEXT && hover instanceof HoverEvent.ShowText ht) {
                Component c = ht.value();
                if (c != null) return c.getString();
            }
        }
        for (Component child : component.getSiblings()) {
            String found = getHoverText(child);
            if (found != null) return found;
        }
        return null;
    }
}
