package rizzfarms.rizzfarms.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public final class ModConfig {
    private ModConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config CFG = new Config();

    public static Config cfg() { return CFG; }

    public static Path configDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("rizzfarms");
    }

    public static Path configPath() {
        return configDir().resolve("chatgamesolver.json");
    }

    public static void load() {
        try {
            Files.createDirectories(configDir());

            Path p = configPath();
            if (!Files.exists(p)) {
                save();
                return;
            }

            String json = Files.readString(p);
            Config loaded = GSON.fromJson(json, Config.class);
            if (loaded != null) {
                CFG = loaded;

                if (CFG.chatGameWindowSeconds < 0.5) CFG.chatGameWindowSeconds = 0.5;
                if (CFG.blankScanCap < 100) CFG.blankScanCap = 100;

                if (CFG.readySoundRepeats < 1) CFG.readySoundRepeats = 1;
                if (CFG.readySoundRepeatGapMs < 0) CFG.readySoundRepeatGapMs = 0;

                if (CFG.readyPopupMs < 250) CFG.readyPopupMs = 250;

                // popup sizing guards
                if (CFG.readyTitleScale < 1.0) CFG.readyTitleScale = 1.0;
                if (CFG.readySubtitleScale < 0.5) CFG.readySubtitleScale = 0.5;

                if (CFG.maxWordLength < 1) CFG.maxWordLength = 32;

                // Humanized auto-chat-game timing guards
                if (CFG.autoChatHumanBaseMinSeconds < 0) CFG.autoChatHumanBaseMinSeconds = 0;
                if (CFG.autoChatHumanBaseMaxSeconds < CFG.autoChatHumanBaseMinSeconds) CFG.autoChatHumanBaseMaxSeconds = CFG.autoChatHumanBaseMinSeconds;

                if (CFG.autoChatHumanTypeBaseMinSeconds < 0) CFG.autoChatHumanTypeBaseMinSeconds = 0;
                if (CFG.autoChatHumanTypeBaseMaxSeconds < CFG.autoChatHumanTypeBaseMinSeconds) CFG.autoChatHumanTypeBaseMaxSeconds = CFG.autoChatHumanTypeBaseMinSeconds;

                if (CFG.autoChatHumanTypePerCharMinSeconds < 0) CFG.autoChatHumanTypePerCharMinSeconds = 0;
                if (CFG.autoChatHumanTypePerCharMaxSeconds < CFG.autoChatHumanTypePerCharMinSeconds) CFG.autoChatHumanTypePerCharMaxSeconds = CFG.autoChatHumanTypePerCharMinSeconds;

                if (CFG.autoChatHumanHoverRegisterMinSeconds < 0) CFG.autoChatHumanHoverRegisterMinSeconds = 0;
                if (CFG.autoChatHumanHoverRegisterMaxSeconds < CFG.autoChatHumanHoverRegisterMinSeconds) CFG.autoChatHumanHoverRegisterMaxSeconds = CFG.autoChatHumanHoverRegisterMinSeconds;

                if (CFG.autoChatHumanHoverExtraMinSeconds < 0) CFG.autoChatHumanHoverExtraMinSeconds = 0;
                if (CFG.autoChatHumanHoverExtraMaxSeconds < CFG.autoChatHumanHoverExtraMinSeconds) CFG.autoChatHumanHoverExtraMaxSeconds = CFG.autoChatHumanHoverExtraMinSeconds;

                if (CFG.autoChatHumanMathEasyMinSeconds < 0) CFG.autoChatHumanMathEasyMinSeconds = 0;
                if (CFG.autoChatHumanMathEasyMaxSeconds < CFG.autoChatHumanMathEasyMinSeconds) CFG.autoChatHumanMathEasyMaxSeconds = CFG.autoChatHumanMathEasyMinSeconds;

                if (CFG.autoChatHumanMathBaseMinSeconds < 0) CFG.autoChatHumanMathBaseMinSeconds = 0;
                if (CFG.autoChatHumanMathBaseMaxSeconds < CFG.autoChatHumanMathBaseMinSeconds) CFG.autoChatHumanMathBaseMaxSeconds = CFG.autoChatHumanMathBaseMinSeconds;

                if (CFG.autoChatHumanMathPerOpSeconds < 0) CFG.autoChatHumanMathPerOpSeconds = 0;
                if (CFG.autoChatHumanMathPerCharSeconds < 0) CFG.autoChatHumanMathPerCharSeconds = 0;
                if (CFG.autoChatHumanMathCapSeconds < 0.15) CFG.autoChatHumanMathCapSeconds = 0.15;

                if (CFG.autoChatGamesAutoSendReactionMinMs < 0) CFG.autoChatGamesAutoSendReactionMinMs = 0;
                if (CFG.autoChatGamesAutoSendReactionMaxMs < CFG.autoChatGamesAutoSendReactionMinMs) {
                    CFG.autoChatGamesAutoSendReactionMaxMs = CFG.autoChatGamesAutoSendReactionMinMs;
                }
            }
        } catch (Exception e) {
            System.out.println("[rizzfarms] Config load failed: " + e.getMessage());
        }
    }

    public static void save() {
        try {
            Files.createDirectories(configDir());
            Files.writeString(configPath(), GSON.toJson(CFG));
        } catch (Exception e) {
            System.out.println("[rizzfarms] Config save failed: " + e.getMessage());
        }
    }

    public static final class Config {
        public double chatGameWindowSeconds = 3.0;

        // R hotkey
        public double rDelaySeconds = 0.67;
        public String rCommand = "/p h";

        public boolean testMode = false;

        // READY text
        public String readyTitleText = "READY!";
        public String readySubtitleText = "Press G";
        public boolean readyAlsoShowActionbar = false;
        public int readyPopupMs = 1300;

        // Slightly smaller than before
        public double readyTitleScale = 3.2;
        public double readySubtitleScale = 1.6;

        // Colors are ARGB (0xAARRGGBB)
        // Big text GREEN
        public int readyTitleColorARGB = 0xFF55FF55;

        // Subtitle alternates per word (yellow/pink)
        public int readySubtitleWordColor1ARGB = 0xFFFFFF55; // yellow
        public int readySubtitleWordColor2ARGB = 0xFFFF55FF; // pink

        // (kept for compatibility; not used by alternating mode)
        public int readySubtitleColorARGB = 0xFFFFFF55;

        // Sound
        public boolean readyPlayXpSound = true;
        public boolean readyUseLevelUpSound = true;
        public float readySoundVolume = 1.8f;
        public float readySoundPitch = 1.1f;
        public int readySoundRepeats = 3;
        public int readySoundRepeatGapMs = 85;

        // Solver timing + caps
        public double blanksBaseMin = 0.4;
        public double blanksBaseMax = 0.8;
        public double blanksPerBlankMin = 0.15;
        public double blanksPerBlankMax = 0.25;
        public double blanksCapSeconds = 2.2;

        public double lettersMin = 0.3;
        public double lettersMax = 0.7;

        public double sequenceMin = 0.3;
        public double sequenceMax = 0.4;

        public double mathBaseMin = 0.5;
        public double mathBaseMax = 1.0;
        public double mathExtraPerChar = 0.05;
        public double mathExtraPerOp = 0.12;
        public double mathMin = 0.5;
        public double mathMax = 1.5;

        public int blankScanCap = 50000;

        public boolean loadExtraWordlist = true;
        public String extraWordlistFilename = "extra_words.txt";
        public int maxWordLength = 32;

        // Farming Macro
        public boolean farmingMacroEnabled = false;
        public double movementThreshold = 0.01;

        public boolean autoCaptchaEnabled = true;
        public boolean autoChatGamesEnabled = true;
        public double autoChatGamesExtraDelaySeconds = 0.5;

        // Direction change delay
        public double directionChangeDelaySeconds = 1.0;
    }
}
