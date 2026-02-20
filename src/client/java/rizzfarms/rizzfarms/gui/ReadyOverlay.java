package rizzfarms.rizzfarms.gui;

import rizzfarms.rizzfarms.config.ModConfig;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

public final class ReadyOverlay {

    private ReadyOverlay() {}

    private static volatile long untilMs = 0L;
    private static volatile String title = "READY!";
    private static volatile String subtitle = "Press G";

    public static void init() {
        HudRenderCallback.EVENT.register((GuiGraphics gg, DeltaTracker delta) -> render(gg));
    }

    public static void show(String t, String sub, int durationMs) {
        title = (t == null || t.isBlank()) ? "READY!" : t;
        subtitle = (sub == null) ? "" : sub;
        untilMs = System.currentTimeMillis() + Math.max(250, durationMs);
    }

    private static void render(GuiGraphics gg) {
        if (System.currentTimeMillis() > untilMs) return;

        Minecraft mc = Minecraft.getInstance();

        int w = gg.guiWidth();
        int h = gg.guiHeight();

        float titleScale = (float) ModConfig.cfg().readyTitleScale;
        float subtitleScale = (float) ModConfig.cfg().readySubtitleScale;

        drawCenteredScaled(gg, mc, title, w / 2, h / 2 - 18, titleScale, ModConfig.cfg().readyTitleColorARGB);
        drawCenteredScaled(gg, mc, subtitle, w / 2, h / 2 + 10, subtitleScale, ModConfig.cfg().readySubtitleColorARGB);
    }

    private static void drawCenteredScaled(GuiGraphics gg, Minecraft mc,
                                           String s, int cx, int cy, float scale, int argb) {
        if (s == null || s.isBlank()) return;

        var pose = gg.pose();

        // Matrix3x2fStack in your env uses pushMatrix/popMatrix, not push/pop or pushPose/popPose
        pose.pushMatrix();
        pose.translate((float) cx, (float) cy);
        pose.scale(scale, scale);

        int textWidth = mc.font.width(s);
        gg.drawString(mc.font, Component.literal(s), -textWidth / 2, 0, argb, true);

        pose.popMatrix();
    }
}
