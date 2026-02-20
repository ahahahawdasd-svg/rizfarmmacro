package rizzfarms.rizzfarms.macro;

import rizzfarms.rizzfarms.RizzfarmsClient;
import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.core.PendingSend;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.concurrent.ThreadLocalRandom;

public class FarmingMacro {
    private static boolean active = false;
    private static State state = State.LEFT;
    private static long lastMoveTime = 0;
    private static Vec3 lastPos = Vec3.ZERO;

    private static State pendingState = null;
    private static long pendingStateSince = 0;

    // Command rate limiting + per-stone-bricks trigger guard (humanized with jitter)
    private static long lastCommandMs = 0L;
    private static boolean sentOnThisStone = false;
    private static long nextCommandCooldownMs = 1000L;
    private static long nextStuckThresholdMs = 3000L;
    /** After /p h: walk forward until this time (0 = not in align phase) */
    private static long walkForwardUntilMs = 0L;

    private enum State {
        LEFT,
        RIGHT
    }

    public static void toggle() {
        active = !active;
        if (active) {
            state = State.RIGHT; // Start by going right (arbitrary)
            lastMoveTime = System.currentTimeMillis();
            nextCommandCooldownMs = 900L + ThreadLocalRandom.current().nextLong(301);
            nextStuckThresholdMs = 2800L + ThreadLocalRandom.current().nextLong(601);
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                lastPos = mc.player.position();
            }
        } else {
            releaseKeys();
        }
        RizzfarmsClient.overlay("Farming Macro: " + (active ? "ENABLED" : "DISABLED"));
    }

    public static boolean isActive() {
        return active;
    }

    public static void tick(Minecraft mc) {
        if (!active || mc.player == null || mc.level == null) return;

        if (mc.screen != null || PendingSend.isArmed() || PendingSend.isReady()) {
            releaseKeys();
            return;
        }

        long now = System.currentTimeMillis();
        Options options = mc.options;

        // After /p h: walk forward 0.1–0.3s to align
        if (walkForwardUntilMs > 0) {
            if (now < walkForwardUntilMs) {
                options.keyUp.setDown(true);
                options.keyLeft.setDown(false);
                options.keyRight.setDown(false);
                options.keyAttack.setDown(true);
                return;
            }
            walkForwardUntilMs = 0;
            options.keyUp.setDown(false);
        }

        Vec3 currentPos = mc.player.position();
        double dist = currentPos.distanceToSqr(lastPos);

        // Detect horizontal movement
        if (dist > ModConfig.cfg().movementThreshold * ModConfig.cfg().movementThreshold) {
            lastMoveTime = now;
            lastPos = currentPos;
        }

        options.keyAttack.setDown(true); // Always mining

        // Detect block under feet
        BlockPos posBelow = mc.player.blockPosition().below();
        BlockState stateBelow = mc.level.getBlockState(posBelow);

        State targetState = null;
        if (stateBelow.is(Blocks.BLACK_WOOL)) {
            targetState = State.RIGHT;
        } else if (stateBelow.is(Blocks.WHITE_WOOL)) {
            targetState = State.LEFT;
        }

        if (targetState != null) {
            if (state != targetState) {
                if (pendingState != targetState) {
                    pendingState = targetState;
                    pendingStateSince = now;
                } else {
                    double baseDelay = ModConfig.cfg().directionChangeDelaySeconds;
                    double jitter = (ThreadLocalRandom.current().nextDouble() * 0.4 - 0.2); // ±0.2s
                    long delayMs = (long) (Math.max(0.3, baseDelay + jitter) * 1000.0);
                    if (now - pendingStateSince >= delayMs) {
                        state = targetState;
                        pendingState = null;
                        sentOnThisStone = false; // left recognized stone area
                    }
                }
            } else {
                pendingState = null;
                sentOnThisStone = false;
            }
        } else if (stateBelow.is(Blocks.QUARTZ_BLOCK)) {
            pendingState = null;
            // Only send one command per stone-bricks encounter; cooldown varies (900–1200ms)
            if (!sentOnThisStone) {
                long nowMs = System.currentTimeMillis();
                if (nowMs - lastCommandMs >= nextCommandCooldownMs) {
                    nextCommandCooldownMs = 900L + ThreadLocalRandom.current().nextLong(301);
                    triggerRestart(mc);
                    lastCommandMs = nowMs;
                    sentOnThisStone = true;
                }
            }
            return;
        } else {
            // Sea Lantern or other blocks: don't change state, and reset stone guard when we leave
            pendingState = null;
            sentOnThisStone = false;
        }

        if (state == State.RIGHT) {
            options.keyRight.setDown(true);
            options.keyLeft.setDown(false);
        } else { // State.LEFT
            options.keyLeft.setDown(true);
            options.keyRight.setDown(false);
        }

        // Stuck detection with varied threshold (2800–3400ms) and cooldown
        if (now - lastMoveTime > nextStuckThresholdMs) {
            long nowMs = System.currentTimeMillis();
            if (nowMs - lastCommandMs >= nextCommandCooldownMs) {
                nextStuckThresholdMs = 2800L + ThreadLocalRandom.current().nextLong(601);
                nextCommandCooldownMs = 900L + ThreadLocalRandom.current().nextLong(301);
                triggerRestart(mc);
                lastCommandMs = nowMs;
            }
        }
    }

    private static void triggerRestart(Minecraft mc) {
        releaseKeys();
        pendingState = null;
        // Prompt says "it types /p h to change directions"
        
        String cmd = ModConfig.cfg().rCommand;
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (mc.player != null && mc.player.connection != null) {
            mc.player.connection.sendCommand(cmd);
        }
        
        RizzfarmsClient.overlay("Macro: Restarting...");
        
        // Re-enable and reset state
        active = true;
        state = State.RIGHT;
        lastMoveTime = System.currentTimeMillis();
        if (mc.player != null) {
            lastPos = mc.player.position();
        }
        // Walk forward 0.1–0.3s to align after teleport
        long alignMs = 100 + ThreadLocalRandom.current().nextLong(201);
        walkForwardUntilMs = System.currentTimeMillis() + alignMs;
    }

    private static void releaseKeys() {
        walkForwardUntilMs = 0;
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.options != null) {
            mc.options.keyUp.setDown(false);
            mc.options.keyLeft.setDown(false);
            mc.options.keyRight.setDown(false);
            mc.options.keyAttack.setDown(false);
        }
    }
}
