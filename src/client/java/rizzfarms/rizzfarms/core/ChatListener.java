package rizzfarms.rizzfarms.core;

import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.gui.CapGuiAssist;
import rizzfarms.rizzfarms.util.Regexes;
import rizzfarms.rizzfarms.util.TextUtil;
import rizzfarms.rizzfarms.util.WordDb;

import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.network.chat.Component;
import rizzfarms.rizzfarms.solvers.*;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

public final class ChatListener {

    private ChatListener() {}

    private static volatile long lastChatGameMillis = 0L;

    private static volatile LearnMode learnMode = LearnMode.NONE;
    private static volatile boolean learnArmed = false;
    private static volatile String learnKey = null;

    private static final Object DEDUP_LOCK = new Object();
    private static String lastLine = "";
    private static long lastLineMs = 0L;

    // Solvers that need to see ALL lines (state machines / feedback loops, hover payloads, etc.)
    private static final List<ChatGameSolver> STATEFUL_SOLVERS = List.of(
            new RandomNumberSolver(),
            new HoverSolver()
    );

    // Solvers that only apply to the actual "| ..." prompt line inside the chat-game window
    private static final List<ChatGameSolver> PROMPT_SOLVERS = List.of(
            new ReverseWordSolver(),
            new TypeSequenceSolver(),
            new MathGameSolver(),
            new FillBlanksSolver(),
            new UnscrambleSolver()
    );

    public static void init() {
        ClientReceiveMessageEvents.GAME.register((message, overlay) -> onIncomingMessage(message));
        ClientReceiveMessageEvents.CHAT.register((message, signedMessage, sender, params, receptionTimestamp) ->
                onIncomingMessage(message)
        );

        ClientSendMessageEvents.CHAT.register((msg) -> {
            if (!ModConfig.cfg().testMode) return;
            onIncomingLine(msg, null);
        });

        ClientSendMessageEvents.COMMAND.register((cmd) -> {
            if (!ModConfig.cfg().testMode) return;
            onIncomingLine("/" + cmd, null);
        });
    }

    private static void onIncomingMessage(Component message) {
        if (message == null) return;
        onIncomingLine(message.getString(), message);
    }

    private static void onIncomingLine(String raw, Component message) {
        if (raw == null || raw.isBlank()) return;

        String line = TextUtil.stripFormatting(raw);
        if (line == null) return;
        line = line.trim();
        if (line.isEmpty()) return;

        // Check for captcha grid reports in chat (CAP: item_id)
        Matcher capMatch = Regexes.CAP_MESSAGE.matcher(line);
        if (capMatch.find()) {
            CapGuiAssist.handleCapMessage(capMatch.group(1));
            return; // Don't process grid reports as games
        }

        if (isDuplicate(line)) return;

        // Learn from reveal lines ASAP
        Matcher revealAny = Regexes.ANSWER_REVEAL_ANY.matcher(line);
        if (revealAny.find()) {
            learnFromReveal(revealAny.group(2));
        } else {
            Matcher reveal = Regexes.ANSWER_REVEAL.matcher(line);
            if (reveal.find()) learnFromReveal(reveal.group(1));
        }

        // Normalize once (strip leading '|') for solvers that don't care about the prefix
        String normalized = normalizePrompt(line);

        // 1) Always run stateful solvers on every line (HIGHER/LOWER feedback, hover payloads, etc.)
        for (ChatGameSolver solver : STATEFUL_SOLVERS) {
            SolveAttempt a = solver.trySolve(normalized, message);
            if (a != null) handleAttempt(a);
        }

        // 2) Gate prompt solvers (only true chat-game prompt lines)
        if (!isChatGameRelated(line)) return;

        String prompt = normalizePrompt(line);
        SolveAttempt attempt = dispatchPrompt(prompt, message);
        if (attempt == null) return;

        handleAttempt(attempt);
    }

    private static void handleAttempt(SolveAttempt attempt) {
        clearLearning();
        applyLearning(attempt);

        if (attempt.answer != null && !attempt.answer.trim().isEmpty()) {
            double delay = attempt.delaySeconds;
            if (rizzfarms.rizzfarms.macro.FarmingMacro.isActive() && ModConfig.cfg().autoChatGamesEnabled) {
                delay += ModConfig.cfg().autoChatGamesExtraDelaySeconds;
            }
            PendingSend.arm(delay, attempt.answer, attempt.why);
        }
    }

    private static SolveAttempt dispatchPrompt(String prompt, Component message) {
        for (ChatGameSolver solver : PROMPT_SOLVERS) {
            SolveAttempt a = solver.trySolve(prompt, message);
            if (a != null) return a;
        }
        return null;
    }

    private static void applyLearning(SolveAttempt attempt) {
        learnMode = attempt.learnMode == null ? LearnMode.NONE : attempt.learnMode;
        learnKey = attempt.learnKey;
        learnArmed = attempt.answer != null && !attempt.answer.trim().isEmpty();

        if (learnMode == LearnMode.NONE) {
            learnKey = null;
            learnArmed = false;
        }
    }

    private static boolean isDuplicate(String line) {
        long now = System.currentTimeMillis();
        synchronized (DEDUP_LOCK) {
            if (line.equals(lastLine) && (now - lastLineMs) <= 150L) {
                return true;
            }
            lastLine = line;
            lastLineMs = now;
            return false;
        }
    }

    private static boolean isChatGameRelated(String line) {
        long now = System.currentTimeMillis();
        String t = line.trim();

        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.contains("chat game") || lower.contains("chat games") || lower.contains("chatgame")) {
            lastChatGameMillis = now;
            return true;
        }

        if (!t.isEmpty() && t.charAt(0) == '|') {
            long dt = now - lastChatGameMillis;
            return dt >= 0 && dt <= (long) (ModConfig.cfg().chatGameWindowSeconds * 1000.0);
        }

        return false;
    }

    private static String normalizePrompt(String line) {
        String t = line.trim();
        if (!t.isEmpty() && t.charAt(0) == '|') {
            t = t.substring(1).trim();
        }
        return t;
    }

    private static void learnFromReveal(String ansRaw) {
        String ans = (ansRaw == null ? "" : ansRaw.trim().toLowerCase(Locale.ROOT));
        if (ans.isEmpty()) return;

        if (learnMode == LearnMode.BLANKS && learnKey != null) {
            if (!learnArmed && WordDb.matchesBlankPattern(learnKey, ans)) {
                WordDb.addLearnedWord(ans);
            }
            clearLearning();
            return;
        }

        if (learnMode == LearnMode.UNSCRAMBLE && learnKey != null) {
            if (!learnArmed && ans.length() == learnKey.length()) {
                WordDb.addLearnedWord(ans);
            }
            clearLearning();
        }
    }

    private static void clearLearning() {
        learnMode = LearnMode.NONE;
        learnArmed = false;
        learnKey = null;
    }
}
