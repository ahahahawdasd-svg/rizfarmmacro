package rizzfarms.rizzfarms.core;

import net.minecraft.network.chat.Component;

/**
 * A lightweight solver.
 *
 * Input: normalized prompt text (no leading '|', no formatting).
 *
 * Most solvers only need the prompt text. Some games hide the real payload in the message component
 * (e.g. hover events), so solvers may optionally consume the full {@link Component}.
 */
public interface ChatGameSolver {
    SolveAttempt trySolve(String prompt);

    default SolveAttempt trySolve(String prompt, Component message) {
        return trySolve(prompt);
    }
}
