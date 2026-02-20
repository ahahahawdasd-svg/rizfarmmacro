package rizzfarms.rizzfarms;

import rizzfarms.rizzfarms.config.ModConfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class RizzfarmsConfigScreen {
    private RizzfarmsConfigScreen() {}

    public static Screen create(Screen parent) {
        ModConfig.Config cfg = ModConfig.cfg();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.literal("Rizzfarms Chat Game Solver"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder eb = builder.entryBuilder();

        // General
        ConfigCategory general = builder.getOrCreateCategory(Component.literal("General"));
        general.addEntry(eb.startDoubleField(Component.literal("Chat game window (seconds)"), cfg.chatGameWindowSeconds)
                .setMin(0.5).setMax(10.0)
                .setDefaultValue(3.0)
                .setSaveConsumer(v -> cfg.chatGameWindowSeconds = v)
                .build());

        general.addEntry(eb.startIntField(Component.literal("Blank scan cap"), cfg.blankScanCap)
                .setMin(100).setMax(500000)
                .setDefaultValue(50000)
                .setSaveConsumer(v -> cfg.blankScanCap = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Test mode"), cfg.testMode)
                .setDefaultValue(false)
                .setSaveConsumer(v -> cfg.testMode = v)
                .build());

        general.addEntry(eb.startBooleanToggle(Component.literal("Load extra wordlist"), cfg.loadExtraWordlist)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.loadExtraWordlist = v)
                .build());

        general.addEntry(eb.startStrField(Component.literal("Extra wordlist filename"), cfg.extraWordlistFilename)
                .setDefaultValue("extra_words.txt")
                .setSaveConsumer(v -> cfg.extraWordlistFilename = v)
                .build());

        general.addEntry(eb.startIntField(Component.literal("Max word length"), cfg.maxWordLength)
                .setMin(1).setMax(256)
                .setDefaultValue(32)
                .setSaveConsumer(v -> cfg.maxWordLength = v)
                .build());

        // Hotkeys
        ConfigCategory hotkeys = builder.getOrCreateCategory(Component.literal("Hotkeys"));
        hotkeys.addEntry(eb.startStrField(Component.literal("R key command"), cfg.rCommand)
                .setDefaultValue("/p h")
                .setSaveConsumer(v -> cfg.rCommand = v)
                .build());

        hotkeys.addEntry(eb.startDoubleField(Component.literal("R key delay (seconds)"), cfg.rDelaySeconds)
                .setMin(0.0).setMax(10.0)
                .setDefaultValue(0.67)
                .setSaveConsumer(v -> cfg.rDelaySeconds = v)
                .build());

        // READY overlay
        ConfigCategory ready = builder.getOrCreateCategory(Component.literal("READY Overlay"));
        ready.addEntry(eb.startStrField(Component.literal("Title text"), cfg.readyTitleText)
                .setDefaultValue("READY!")
                .setSaveConsumer(v -> cfg.readyTitleText = v)
                .build());

        ready.addEntry(eb.startStrField(Component.literal("Subtitle text"), cfg.readySubtitleText)
                .setDefaultValue("Press G")
                .setSaveConsumer(v -> cfg.readySubtitleText = v)
                .build());

        ready.addEntry(eb.startBooleanToggle(Component.literal("Also show actionbar"), cfg.readyAlsoShowActionbar)
                .setDefaultValue(false)
                .setSaveConsumer(v -> cfg.readyAlsoShowActionbar = v)
                .build());

        ready.addEntry(eb.startIntField(Component.literal("Popup duration (ms)"), cfg.readyPopupMs)
                .setMin(250).setMax(10000)
                .setDefaultValue(1300)
                .setSaveConsumer(v -> cfg.readyPopupMs = v)
                .build());

        ready.addEntry(eb.startDoubleField(Component.literal("Title scale"), cfg.readyTitleScale)
                .setMin(1.0).setMax(10.0)
                .setDefaultValue(3.2)
                .setSaveConsumer(v -> cfg.readyTitleScale = v)
                .build());

        ready.addEntry(eb.startDoubleField(Component.literal("Subtitle scale"), cfg.readySubtitleScale)
                .setMin(0.5).setMax(10.0)
                .setDefaultValue(1.6)
                .setSaveConsumer(v -> cfg.readySubtitleScale = v)
                .build());

        ready.addEntry(eb.startIntField(Component.literal("Title color (ARGB int)"), cfg.readyTitleColorARGB)
                .setDefaultValue(0xFF55FF55)
                .setSaveConsumer(v -> cfg.readyTitleColorARGB = v)
                .build());

        ready.addEntry(eb.startIntField(Component.literal("Subtitle color (ARGB int)"), cfg.readySubtitleColorARGB)
                .setDefaultValue(0xFFFFFF55)
                .setSaveConsumer(v -> cfg.readySubtitleColorARGB = v)
                .build());

        // Sound
        ConfigCategory sound = builder.getOrCreateCategory(Component.literal("Sound"));
        sound.addEntry(eb.startBooleanToggle(Component.literal("Play READY sound"), cfg.readyPlayXpSound)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.readyPlayXpSound = v)
                .build());

        sound.addEntry(eb.startBooleanToggle(Component.literal("Use level-up sound"), cfg.readyUseLevelUpSound)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.readyUseLevelUpSound = v)
                .build());

        sound.addEntry(eb.startFloatField(Component.literal("Sound volume"), cfg.readySoundVolume)
                .setMin(0.0f).setMax(5.0f)
                .setDefaultValue(1.8f)
                .setSaveConsumer(v -> cfg.readySoundVolume = v)
                .build());

        sound.addEntry(eb.startFloatField(Component.literal("Sound pitch"), cfg.readySoundPitch)
                .setMin(0.1f).setMax(3.0f)
                .setDefaultValue(1.1f)
                .setSaveConsumer(v -> cfg.readySoundPitch = v)
                .build());

        sound.addEntry(eb.startIntField(Component.literal("Sound repeats"), cfg.readySoundRepeats)
                .setMin(1).setMax(20)
                .setDefaultValue(3)
                .setSaveConsumer(v -> cfg.readySoundRepeats = v)
                .build());

        sound.addEntry(eb.startIntField(Component.literal("Repeat gap (ms)"), cfg.readySoundRepeatGapMs)
                .setMin(0).setMax(2000)
                .setDefaultValue(85)
                .setSaveConsumer(v -> cfg.readySoundRepeatGapMs = v)
                .build());

        // Farming Macro
        ConfigCategory macro = builder.getOrCreateCategory(Component.literal("Farming Macro"));
        macro.addEntry(eb.startBooleanToggle(Component.literal("Enabled (F6)"), cfg.farmingMacroEnabled)
                .setDefaultValue(false)
                .setSaveConsumer(v -> cfg.farmingMacroEnabled = v)
                .build());

        macro.addEntry(eb.startDoubleField(Component.literal("Movement threshold"), cfg.movementThreshold)
                .setDefaultValue(0.01)
                .setSaveConsumer(v -> cfg.movementThreshold = v)
                .build());

        macro.addEntry(eb.startBooleanToggle(Component.literal("Auto solve captchas"), cfg.autoCaptchaEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.autoCaptchaEnabled = v)
                .build());

        macro.addEntry(eb.startBooleanToggle(Component.literal("Auto solve chat games"), cfg.autoChatGamesEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.autoChatGamesEnabled = v)
                .build());

        macro.addEntry(eb.startDoubleField(Component.literal("Auto solve extra delay (s)"), cfg.autoChatGamesExtraDelaySeconds)
                .setDefaultValue(0.5)
                .setSaveConsumer(v -> cfg.autoChatGamesExtraDelaySeconds = v)
                .build());

        macro.addEntry(eb.startDoubleField(Component.literal("Direction change delay (s)"), cfg.directionChangeDelaySeconds)
                .setMin(0.0).setMax(10.0)
                .setDefaultValue(1.0)
                .setSaveConsumer(v -> cfg.directionChangeDelaySeconds = v)
                .build());

        return builder.build();
    }
}
