package rizzfarms.rizzfarms.core;

import rizzfarms.rizzfarms.macro.FarmingMacro;
import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.RizzfarmsClient;

import net.minecraft.client.Minecraft;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Tick-based pending send (no threads).
 * Arm -> wait delay -> READY cue -> user presses G -> send.
 */
public final class PendingSend {

    private PendingSend() {}

    private static final Object LOCK = new Object();

    private static boolean armed = false;
    private static boolean ready = false;
    private static String msg = "";
    private static long readyAtMs = 0L;
    /** When to auto-send after ready (human reaction delay 80–250ms) */
    private static long autoSendAfterMs = 0L;

    private static final AtomicLong GEN = new AtomicLong(0);
    private static long genAtArm = 0L;

    public static void arm(double delaySeconds, String answer, String why) {
        if (answer == null) return;
        String a = answer.trim();
        if (a.isEmpty()) return;

        long delayMs = Math.max(0L, (long) (delaySeconds * 1000.0));
        long now = System.currentTimeMillis();

        long myGen = GEN.incrementAndGet();
        synchronized (LOCK) {
            msg = a;
            armed = true;
            ready = false;
            readyAtMs = now + delayMs;
            genAtArm = myGen;
        }
    }

    /** Call every client tick */
    public static void tick() {
        boolean cue = false;

        synchronized (LOCK) {
            if (!armed) return;
            if (!ready) {
                if (msg.isEmpty()) {
                    armed = false;
                    return;
                }
                if (GEN.get() != genAtArm) {
                    armed = false;
                    ready = false;
                    msg = "";
                    return;
                }
                long now = System.currentTimeMillis();
                if (now < readyAtMs) return;
                ready = true;
                cue = true;
                autoSendAfterMs = now + 80 + ThreadLocalRandom.current().nextLong(171);
            }
            // When ready we fall through so auto-send check below runs (fix: used to return and never auto-sent during macro)
        }

        if (cue) {
            RizzfarmsClient.readyCue();
        }

        long now = System.currentTimeMillis();
        boolean shouldAutoSend = ready && FarmingMacro.isActive() && ModConfig.cfg().autoChatGamesEnabled;
        if (shouldAutoSend && now >= autoSendAfterMs) {
            trySend(Minecraft.getInstance());
            autoSendAfterMs = 0L;
        }
    }

    /** Called when user presses G */
    public static boolean trySend(Minecraft mc) {
        final String out;
        synchronized (LOCK) {
            if (!ready) return false;
            out = msg;

            // clear
            armed = false;
            ready = false;
            msg = "";
            readyAtMs = 0L;
            autoSendAfterMs = 0L;
            genAtArm = GEN.get();
        }

        if (mc != null && mc.player != null && mc.player.connection != null) {
            // Mojmap: LocalPlayer.connection is ClientPacketListener
            mc.player.connection.sendChat(out);
            return true;
        }
        return false;
    }

    public static boolean isReady() {
        synchronized (LOCK) {
            return ready;
        }
    }

    public static boolean isArmed() {
        synchronized (LOCK) {
            return armed;
        }
    }

    public static void clear() {
        GEN.incrementAndGet();
        synchronized (LOCK) {
            armed = false;
            ready = false;
            msg = "";
            readyAtMs = 0L;
            autoSendAfterMs = 0L;
            genAtArm = GEN.get();
        }
    }
}
