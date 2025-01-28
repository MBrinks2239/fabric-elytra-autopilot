package net.elytraautopilot.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.isxander.yacl3.api.*;
import dev.isxander.yacl3.api.controller.*;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.util.log.Log;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ModConfig {

    public static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static final Path CONFIG_FILE = FabricLoader.getInstance()
            .getConfigDir().resolve("fabric-elytra-autopilot.json");

    public static ModConfig INSTANCE = loadConfig(CONFIG_FILE.toFile());

    // Gui defaults
    public static final boolean showGuiDefault = true;
    public static final int guiScaleDefault = 100;
    public static final int minGuiScale = 0;
    public static final int maxGuiScale = 300;
    public static final int guiXDefault = 5;
    public static final int minGuiX = 0;
    public static final int maxGuiX = 300;
    public static final int guiYDefault = 5;
    public static final int minGuiY = 0;
    public static final int maxGuiY = 300;

    // Flight profile defaults
    public static final int maxHeightDefault = 360;
    public static final int minHeightDefault = 180;
    public static final int maxMaxHeight = 10000;
    public static final int minMaxHeight = 0;
    public static final int maxMinHeight = 10000;
    public static final int minMinHeight = 0;
    public static final boolean autoLandingDefault = true;
    public static final String playSoundOnLandingDefault = "minecraft:block.note_block.pling";
    public static final double autoLandSpeedDefault = 3.0;
    public static final double turningSpeedDefault = 3.0;
    public static final double takeOffPullDefault = 10.0;
    public static final boolean riskyLandingDefault = false;
    public static final boolean poweredFlightDefault = false;
    public static final boolean elytraHotswapDefault = true;
    public static final boolean fireworkHotswapDefault = true;
    public static final boolean emergencyLandDefault = true;
    public static final boolean elytraAutoSwapDefault = false;

    // Advanced defaults
    public static final int groundCheckTicksDefault = 1;
    public static final double pullUpAngleDefault = -46.63;
    public static final double pullDownAngleDefault = 37.20;
    public static final double pullUpMinVelocityDefault = 1.91;
    public static final double pullDownMaxVelocityDefault = 2.33;
    public static final double pullUpSpeedDefault = 2.16;
    public static final double pullDownSpeedDefault = 0.21;
    public static List<String> flyLocationsDefault = new ArrayList<>();

    // Gui values
    public boolean showGui = showGuiDefault;
    public int guiScale = guiScaleDefault;
    public int guiX = guiXDefault;
    public int guiY = guiYDefault;

    // Flight profile values
    public int maxHeight = maxHeightDefault;
    public int minHeight = minHeightDefault;
    public boolean autoLanding = autoLandingDefault;
    public String playSoundOnLanding = playSoundOnLandingDefault;
    public double autoLandSpeed = autoLandSpeedDefault;
    public double turningSpeed = turningSpeedDefault;
    public double takeOffPull = takeOffPullDefault;
    public boolean riskyLanding = riskyLandingDefault;
    public boolean poweredFlight = poweredFlightDefault;
    public boolean elytraHotswap = elytraHotswapDefault;
    public boolean fireworkHotswap = fireworkHotswapDefault;
    public boolean emergencyLand = emergencyLandDefault;
    public boolean elytraAutoSwap = elytraAutoSwapDefault;

    // Advanced values
    public int groundCheckTicks = groundCheckTicksDefault;
    public double pullUpAngle = pullUpAngleDefault;
    public double pullDownAngle = pullDownAngleDefault;
    public double pullUpMinVelocity = pullUpMinVelocityDefault;
    public double pullDownMaxVelocity = pullDownMaxVelocityDefault;
    public double pullUpSpeed = pullUpSpeedDefault;
    public double pullDownSpeed = pullDownSpeedDefault;
    public List<String> flyLocations = flyLocationsDefault;

    public static Screen createConfigScreen(Screen parent) {
        return YetAnotherConfigLib.createBuilder()
                .title(Text.of("Elytra Autopilot config"))
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("GUI"))
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Show GUI"))
                                .description(OptionDescription.of(Text.of("Toggle for if the flight GUI should be shown while flying")))
                                .binding(
                                        showGuiDefault,
                                        () -> ModConfig.INSTANCE.showGui,
                                        newVal -> ModConfig.INSTANCE.showGui = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("GUI Scale"))
                                .description(OptionDescription.of(Text.of("Scale of the flight GUI")))
                                .binding(
                                        guiScaleDefault,
                                        () -> ModConfig.INSTANCE.guiScale,
                                        newVal -> ModConfig.INSTANCE.guiScale = newVal)
                                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                                        .range(minGuiScale, maxGuiScale).step(1))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("GUI X Position"))
                                .description(OptionDescription.of(Text.of("X position of the flight GUI")))
                                .binding(
                                        guiXDefault,
                                        () -> ModConfig.INSTANCE.guiX,
                                        newVal -> ModConfig.INSTANCE.guiX = newVal)
                                .controller(opt ->
                                        IntegerFieldControllerBuilder.create(opt)
                                                .min(minGuiX)
                                                .max(maxGuiX))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("GUI Y Position"))
                                .description(OptionDescription.of(Text.of("Y position of the flight GUI")))
                                .binding(
                                        guiYDefault,
                                        () -> ModConfig.INSTANCE.guiY,
                                        newVal -> ModConfig.INSTANCE.guiY = newVal)
                                .controller(opt ->
                                        IntegerFieldControllerBuilder.create(opt)
                                                .min(minGuiY)
                                                .max(maxGuiY))
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Flight Profile"))
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("Max Height"))
                                .description(OptionDescription.of(Text.of("Maximum flight height")))
                                .binding(
                                        maxHeightDefault,
                                        () -> ModConfig.INSTANCE.maxHeight,
                                        newVal -> ModConfig.INSTANCE.maxHeight = newVal)
                                .controller(opt ->
                                        IntegerFieldControllerBuilder.create(opt)
                                                .min(minMaxHeight)
                                                .max(maxMaxHeight))
                                .build())
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("Min Height"))
                                .description(OptionDescription.of(Text.of("Minimum flight height")))
                                .binding(
                                        minHeightDefault,
                                        () -> ModConfig.INSTANCE.minHeight,
                                        newVal -> ModConfig.INSTANCE.minHeight = newVal)
                                .controller(opt ->
                                        IntegerFieldControllerBuilder.create(opt)
                                                .min(minMinHeight)
                                                .max(maxMinHeight))
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Auto Landing"))
                                .description(OptionDescription.of(Text.of("Toggle for automatic landing")))
                                .binding(
                                        autoLandingDefault,
                                        () -> ModConfig.INSTANCE.autoLanding,
                                        newVal -> ModConfig.INSTANCE.autoLanding = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<String>createBuilder()
                                .name(Text.of("Play Sound On Landing"))
                                .description(OptionDescription.of(Text.of("Sound to play on landing")))
                                .binding(
                                        playSoundOnLandingDefault,
                                        () -> ModConfig.INSTANCE.playSoundOnLanding,
                                        newVal -> ModConfig.INSTANCE.playSoundOnLanding = newVal)
                                .controller(StringControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Auto Land Speed"))
                                .description(OptionDescription.of(Text.of("Speed for automatic landing")))
                                .binding(
                                        autoLandSpeedDefault,
                                        () -> ModConfig.INSTANCE.autoLandSpeed,
                                        newVal -> ModConfig.INSTANCE.autoLandSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Turning Speed"))
                                .description(OptionDescription.of(Text.of("Turning speed of the flight")))
                                .binding(
                                        turningSpeedDefault,
                                        () -> ModConfig.INSTANCE.turningSpeed,
                                        newVal -> ModConfig.INSTANCE.turningSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Take Off Pull"))
                                .description(OptionDescription.of(Text.of("Pull force during take off")))
                                .binding(
                                        takeOffPullDefault,
                                        () -> ModConfig.INSTANCE.takeOffPull,
                                        newVal -> ModConfig.INSTANCE.takeOffPull = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Risky Landing"))
                                .description(OptionDescription.of(Text.of("Enable risky landing mode")))
                                .binding(
                                        riskyLandingDefault,
                                        () -> ModConfig.INSTANCE.riskyLanding,
                                        newVal -> ModConfig.INSTANCE.riskyLanding = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Powered Flight"))
                                .description(OptionDescription.of(Text.of("Enable powered flight mode")))
                                .binding(
                                        poweredFlightDefault,
                                        () -> ModConfig.INSTANCE.poweredFlight,
                                        newVal -> ModConfig.INSTANCE.poweredFlight = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Elytra Hotswap"))
                                .description(OptionDescription.of(Text.of("Enable automatic elytra hotswap")))
                                .binding(
                                        elytraHotswapDefault,
                                        () -> ModConfig.INSTANCE.elytraHotswap,
                                        newVal -> ModConfig.INSTANCE.elytraHotswap = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Firework Hotswap"))
                                .description(OptionDescription.of(Text.of("Enable automatic firework hotswap")))
                                .binding(
                                        fireworkHotswapDefault,
                                        () -> ModConfig.INSTANCE.fireworkHotswap,
                                        newVal -> ModConfig.INSTANCE.fireworkHotswap = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Emergency Land"))
                                .description(OptionDescription.of(Text.of("Enable emergency landing mode")))
                                .binding(
                                        emergencyLandDefault,
                                        () -> ModConfig.INSTANCE.emergencyLand,
                                        newVal -> ModConfig.INSTANCE.emergencyLand = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .option(Option.<Boolean>createBuilder()
                                .name(Text.of("Elytra Auto equip/swap"))
                                .description(OptionDescription.of(Text.of("Enable automatic elytra equip or chestplate swap when double jump (try to glide)")))
                                .binding(
                                        elytraAutoSwapDefault,
                                        () -> ModConfig.INSTANCE.elytraAutoSwap,
                                        newVal -> ModConfig.INSTANCE.elytraAutoSwap = newVal)
                                .controller(BooleanControllerBuilder::create)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Text.of("Advanced"))
                        .option(Option.<Integer>createBuilder()
                                .name(Text.of("Ground Check Ticks"))
                                .description(OptionDescription.of(Text.of("Number of ticks to check the ground")))
                                .binding(
                                        groundCheckTicksDefault,
                                        () -> ModConfig.INSTANCE.groundCheckTicks,
                                        newVal -> ModConfig.INSTANCE.groundCheckTicks = newVal)
                                .controller(IntegerFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Up Angle"))
                                .description(OptionDescription.of(Text.of("Angle for pulling up during flight")))
                                .binding(
                                        pullUpAngleDefault,
                                        () -> ModConfig.INSTANCE.pullUpAngle,
                                        newVal -> ModConfig.INSTANCE.pullUpAngle = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Down Angle"))
                                .description(OptionDescription.of(Text.of("Angle for pulling down during flight")))
                                .binding(
                                        pullDownAngleDefault,
                                        () -> ModConfig.INSTANCE.pullDownAngle,
                                        newVal -> ModConfig.INSTANCE.pullDownAngle = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Up Min Velocity"))
                                .description(OptionDescription.of(Text.of("Minimum velocity for pulling up during flight")))
                                .binding(
                                        pullUpMinVelocityDefault,
                                        () -> ModConfig.INSTANCE.pullUpMinVelocity,
                                        newVal -> ModConfig.INSTANCE.pullUpMinVelocity = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Down Max Velocity"))
                                .description(OptionDescription.of(Text.of("Maximum velocity for pulling down during flight")))
                                .binding(
                                        pullDownMaxVelocityDefault,
                                        () -> ModConfig.INSTANCE.pullDownMaxVelocity,
                                        newVal -> ModConfig.INSTANCE.pullDownMaxVelocity = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Up Speed"))
                                .description(OptionDescription.of(Text.of("Speed for pulling up during flight")))
                                .binding(
                                        pullUpSpeedDefault,
                                        () -> ModConfig.INSTANCE.pullUpSpeed,
                                        newVal -> ModConfig.INSTANCE.pullUpSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(Option.<Double>createBuilder()
                                .name(Text.of("Pull Down Speed"))
                                .description(OptionDescription.of(Text.of("Speed for pulling down during flight")))
                                .binding(
                                        pullDownSpeedDefault,
                                        () -> ModConfig.INSTANCE.pullDownSpeed,
                                        newVal -> ModConfig.INSTANCE.pullDownSpeed = newVal)
                                .controller(DoubleFieldControllerBuilder::create)
                                .build())
                        .option(ListOption.<String>createBuilder()
                                .name(Text.of("Fly Locations"))
                                .binding(
                                        flyLocationsDefault,
                                        () -> ModConfig.INSTANCE.flyLocations,
                                        newVal -> ModConfig.INSTANCE.flyLocations = newVal)
                                .controller(StringControllerBuilder::create)
                                .initial("Location;0;0")
                                .build())
                        .build())
                .save(() -> INSTANCE.saveConfig(CONFIG_FILE.toFile()))
                .build()
                .generateScreen(parent);
    }

    public void saveConfig(File file) {
        Logger logger = LoggerFactory.getLogger("ElytraAutoPilot");
        logger.info(GSON.toString());
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static ModConfig loadConfig(File file) {
        ModConfig config = null;

        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8)
            )) {
                config = GSON.fromJson(reader, ModConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (config == null) {
            config = new ModConfig();
        }

        if (config.flyLocations == null) {
            config.flyLocations = new ArrayList<>();
        }

        config.saveConfig(file);
        return config;
    }
}
