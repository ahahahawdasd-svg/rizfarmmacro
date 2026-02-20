package rizzfarms.rizzfarms;

import rizzfarms.rizzfarms.command.ReloadCommand;
import rizzfarms.rizzfarms.command.TestCommand;
import rizzfarms.rizzfarms.config.ModConfig;
import rizzfarms.rizzfarms.core.PendingSend;
import rizzfarms.rizzfarms.core.ChatListener;
import rizzfarms.rizzfarms.gui.CapGuiAssist;
import rizzfarms.rizzfarms.gui.ReadyOverlay;
import rizzfarms.rizzfarms.macro.FarmingMacro;
import rizzfarms.rizzfarms.util.WordDb;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class RizzfarmsClient implements ClientModInitializer {

    private static KeyMapping keySendReady;
    private static KeyMapping keyPartyHome;
    private static KeyMapping keyFarmingMacro;

    private static final AtomicBoolean rInflight = new AtomicBoolean(false);
    private static final ScheduledExecutorService EXECUTOR = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "rizzfarms-worker");
        t.setDaemon(true);
        return t;
    });

    public static String getSendReadyKeyName() {
        try {
            return (keySendReady == null) ? "G" : keySendReady.getTranslatedKeyMessage().getString();
        } catch (Throwable t) {
            return "G";
        }
    }

    @Override
    public void onInitializeClient() {
        System.out.println("[rizzfarms] Client initializer loaded");

        ModConfig.load();
        WordDb.load();
        TestCommand.init();
        CapGuiAssist.init();
        ReadyOverlay.init();

        keySendReady = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rizzfarms.send_ready",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                KeyMapping.Category.MISC
        ));

        keyPartyHome = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rizzfarms.party_home",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_R,
                KeyMapping.Category.MISC
        ));

        keyFarmingMacro = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.rizzfarms.farming_macro",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_F6,
                KeyMapping.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            PendingSend.tick();
            FarmingMacro.tick(client);

            boolean blocked = client.screen != null;

            while (keySendReady.consumeClick()) {
                if (blocked) continue;
                boolean sent = PendingSend.trySend(Minecraft.getInstance());
                if (!sent) overlay("Nothing ready");
            }

            while (keyPartyHome.consumeClick()) {
                if (blocked) continue;
                triggerPartyHome(client);
            }

            while (keyFarmingMacro.consumeClick()) {
                if (blocked) continue;
                FarmingMacro.toggle();
            }
        });

        ChatListener.init();
        TestCommand.init();
        ReloadCommand.init(); // if you added /cgreload

        overlay("ChatGames loaded");
    }

    private static void triggerPartyHome(Minecraft client) {
        if (!rInflight.compareAndSet(false, true)) return;

        double baseDelay = ModConfig.cfg().rDelaySeconds;
        long delayMs = (long) ((baseDelay + (Math.random() * 0.24 - 0.12)) * 1000); // ±0.12s jitter
        delayMs = Math.max(200, delayMs);
        EXECUTOR.schedule(() -> {
            try {
                if (client.player != null && client.player.connection != null) {
                    String cmd = ModConfig.cfg().rCommand;
                    if (cmd.startsWith("/")) cmd = cmd.substring(1);
                    client.player.connection.sendCommand(cmd);
                }
            } finally {
                rInflight.set(false);
            }
        }, delayMs, TimeUnit.MILLISECONDS);
    }

    public static void overlay(String msg) {
        Minecraft client = Minecraft.getInstance();
        if (client != null && client.gui != null) {
            client.gui.setOverlayMessage(Component.literal(msg), false);
        }
    }

    // Called by PendingSend when answer becomes READY
    public static void readyCue() {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;

        String sub = ModConfig.cfg().readySubtitleText;
        if ("Press G".equals(sub)) {
            sub = "Press " + getSendReadyKeyName();
        }
        ReadyOverlay.show(
                ModConfig.cfg().readyTitleText,
                sub,
                ModConfig.cfg().readyPopupMs
        );

        if (ModConfig.cfg().readyPlayXpSound && mc.player != null) {
            float vol = ModConfig.cfg().readySoundVolume;
            float pitch = ModConfig.cfg().readySoundPitch;

            var sound = ModConfig.cfg().readyUseLevelUpSound
                    ? SoundEvents.PLAYER_LEVELUP
                    : SoundEvents.EXPERIENCE_ORB_PICKUP;

            int repeats = Math.max(1, ModConfig.cfg().readySoundRepeats);
            int gapMs = Math.max(0, ModConfig.cfg().readySoundRepeatGapMs);

            for (int i = 0; i < repeats; i++) {
                EXECUTOR.schedule(() -> {
                    Minecraft m = Minecraft.getInstance();
                    if (m != null && m.player != null) {
                        m.player.playSound(sound, vol, pitch);
                    }
                }, (long) i * gapMs, TimeUnit.MILLISECONDS);
            }
        }

        if (ModConfig.cfg().readyAlsoShowActionbar) {
            overlay("READY: Press " + getSendReadyKeyName());
        }
    }
}
