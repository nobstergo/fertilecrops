package me.FertileCrops;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.Collections; 
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.TabExecutor; 
import org.bukkit.command.TabCompleter;
import org.bukkit.ChatColor;
import java.util.List;
import java.util.Collections;
import java.io.InputStreamReader;
import java.io.IOException;




public class FertileCrops extends JavaPlugin {

    private static FertileCrops instance;
    private double successChance;
    private int spreadRadius;
    private Map<Material, Boolean> allowedCrops;
    private int xpCost;

    private FileConfiguration messagesConfig;
    private File messagesFile;

    public void loadMessages() {
        if (messagesFile == null) {
            messagesFile = new File(getDataFolder(), "messages.yml");
        }
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public FileConfiguration getMessages() {
        return messagesConfig;
    }

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        loadMessages();
        loadConfigValues();

        getCommand("fertilecrops").setExecutor(this); 
        getCommand("fertilecrops").setTabCompleter(new FertileCropsCommand(this));

        getServer().getPluginManager().registerEvents(new GrowthListener(this), this);
        getLogger().info("FertileCrops enabled!");

        getLogger().info("Allowed crops: " + allowedCrops.entrySet().stream()
                .filter(Map.Entry::getValue)
                .map(e -> e.getKey().name())
                .sorted()
                .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b));

        getLogger().info("Spread radius: " + spreadRadius);
        getLogger().info("Success chance (rate): " + successChance);

        List<String> withered = getConfig().getStringList("withered-blocks");
        getLogger().info("Withered blocks: " + (withered.isEmpty() ? "None" : String.join(", ", withered)));
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = getMessages().getString(path, "&cMissing message: " + path);
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                msg = msg.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String path) {
        return getMessage(path, Collections.emptyMap());
    }

    @Override
    public void onDisable() {
        getLogger().info("FertileCrops disabled!");
    }

    public Map<Material, Boolean> getAllowedCrops() {
        return Collections.unmodifiableMap(allowedCrops);
    }

    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        spreadRadius = config.getInt("spread-radius", 3);
        successChance = config.getDouble("success-chance", 0.7);
        xpCost = config.getInt("xp-cost", 1);

        allowedCrops = new HashMap<>();
        if (config.getConfigurationSection("allowed-crops") != null) {
            for (String key : config.getConfigurationSection("allowed-crops").getKeys(false)) {
                Material mat = Material.getMaterial(key);
                if (mat != null) {
                    allowedCrops.put(mat, config.getBoolean("allowed-crops." + key, true));
                }
            }
        }
    }

    public void reloadMessages() {
        messagesFile = new File(getDataFolder(), "messages.yml");

        // Load default messages from the jar
        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
            new InputStreamReader(getResource("messages.yml"))
        );

        // If the file doesn't exist, create it
        if (!messagesFile.exists()) {
            saveResource("messages.yml", false);
        }

        // Load existing messages
        messagesConfig = YamlConfiguration.loadConfiguration(messagesFile);

        // Merge missing keys from defaultConfig into messagesConfig
        for (String key : defaultConfig.getKeys(true)) {
            if (!messagesConfig.contains(key)) {
                messagesConfig.set(key, defaultConfig.get(key));
            }
        }

        // Save merged config
        try {
            messagesConfig.save(messagesFile);
        } catch (IOException e) {
            getLogger().warning("Could not save messages.yml: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage(getMessage("prefix") + getMessage("usage.main", Map.of("command", label)));
            return true;
        }

        String sub = args[0].toLowerCase();

        switch (sub) {
            case "reload" -> {
                if (!sender.hasPermission("fertilecrops.reload")) {
                    sender.sendMessage(getMessage("prefix") + getMessage("no-permission", Map.of("command", label)));
                    return true;
                }
                reloadConfig();
                loadConfigValues();
                reloadMessages();
                sender.sendMessage(getMessage("prefix") + getMessage("reload-complete", Map.of("command", label)));
            }

            case "crop" -> {
                if (args.length < 2) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.crop", Map.of("command", label)));
                    return true;
                }

                String action = args[1].toLowerCase();

                if (action.equals("list")) {
                    // Only show allowed crops
                    Map<Material, Boolean> allowedCrops = getAllowedCrops();

                    String allowed = allowedCrops.entrySet().stream()
                            .filter(Map.Entry::getValue)
                            .map(e -> e.getKey().name())
                            .sorted()
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);

                    sender.sendMessage(getMessage("crop-list.allowed", Map.of("list", allowed.isEmpty() ? "None" : allowed)));
                } else {
                    // Handle add/remove as usual
                    if (args.length < 3) {
                        sender.sendMessage(getMessage("prefix") + getMessage("usage.crop", Map.of("command", label)));
                        return true;
                    }
                    handleCropCommand(sender, args[1], args[2]);
                }
            }

            case "withered" -> {
                if (args.length < 2) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.withered", Map.of("command", label)));
                    return true;
                }

                String action = args[1].toLowerCase();

                if (action.equals("list")) {
                    List<String> withered = getConfig().getStringList("withered-blocks");
                    String list = withered.stream()
                            .sorted()
                            .reduce("", (a, b) -> a.isEmpty() ? b : a + ", " + b);

                    sender.sendMessage(getMessage("prefix") + getMessage(
                            "crop-list.not-allowed", Map.of("list", list.isEmpty() ? "None" : list))
                    );
                    return true;
                }

                if (args.length < 3) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.withered", Map.of("command", label)));
                    return true;
                }

                handleWitheredCommand(sender, action, args[2]);
            }

            case "cost" -> {
                if (args.length < 2) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.cost", Map.of("command", label)));
                    return true;
                }
                handleCostCommand(sender, args[1]);
            }

            case "rate" -> {
                if (args.length < 2) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.rate", Map.of("command", label)));
                    return true;
                }
                handleRateCommand(sender, args[1]);
            }
            
            case "radius" -> {
                if (args.length < 2) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.radius", Map.of("command", label)));
                    return true;
                }
                handleRadiusCommand(sender, args[1]);
            }

            case "help" -> {
                sender.sendMessage(getMessage("help.header"));
                sender.sendMessage(getMessage("help.crop-add"));
                sender.sendMessage(getMessage("help.crop-remove"));
                sender.sendMessage(getMessage("help.crop-list"));
                sender.sendMessage(getMessage("help.withered-add"));
                sender.sendMessage(getMessage("help.withered-remove"));
                sender.sendMessage(getMessage("help.withered-list"));
                sender.sendMessage(getMessage("help.cost"));
                sender.sendMessage(getMessage("help.rate"));
                sender.sendMessage(getMessage("help.radius"));
                sender.sendMessage(getMessage("help.reload"));
                sender.sendMessage(getMessage("help.help"));
                return true;
            }

            default -> sender.sendMessage(FertileCrops.getInstance().getMessage("prefix")
                                   + FertileCrops.getInstance().getMessage("unknown-subcommand",
                                         Map.of("subcommand", args[0])));
        }

        return true;
    }

    private void handleCropCommand(CommandSender sender, String action, String CropName) {
        Material mat = Material.matchMaterial(CropName.toUpperCase());
        if (mat == null) {
            sender.sendMessage(
                FertileCrops.getInstance().getMessage("prefix")
                + FertileCrops.getInstance().getMessage("unknown-material", Map.of("material", CropName))
            );
            return;
        }

        FileConfiguration config = getConfig();
        List<String> list = config.getStringList("withered-blocks");

        if (action.equalsIgnoreCase("add")) {
            config.set("allowed-crops." + mat.name(), true);
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("allowed-add",
                                    Map.of("material", mat.name())));
        } else if (action.equalsIgnoreCase("remove")) {
            config.set("allowed-crops." + mat.name(), false); 
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("allowed-remove",
                                    Map.of("material", mat.name())));
        }
        config.set("withered-blocks", list);
        saveConfig();
        loadConfigValues();
    }

    private void handleWitheredCommand(CommandSender sender, String action, String CropName) {
        Material mat = Material.matchMaterial(CropName.toUpperCase());
        if (mat == null) {
            sender.sendMessage(
                FertileCrops.getInstance().getMessage("prefix")
                + FertileCrops.getInstance().getMessage("unknown-material", Map.of("material", CropName))
            );
            return;
        }

        FileConfiguration config = getConfig();
        java.util.List<String> list = config.getStringList("withered-blocks");

        if (action.equalsIgnoreCase("add")) {
            if (!list.contains(mat.name())) {
                list.add(mat.name());
                sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                                FertileCrops.getInstance().getMessage("withered-add",
                                        Map.of("material", mat.name())));
            }
        } else if (action.equalsIgnoreCase("remove")) {
            list.remove(mat.name());
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("withered-remove",
                                    Map.of("material", mat.name())));
        }

        config.set("withered-blocks", list);
        saveConfig();
    }

    private void handleCostCommand(CommandSender sender, String value) {
        try {
            int newCost = Integer.parseInt(value);
            getConfig().set("xp-cost", newCost);
            saveConfig();
            loadConfigValues();
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("xp-cost-set",
                                    Map.of("amount", String.valueOf(newCost))));
        } catch (NumberFormatException e) {
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("invalid-number",
                                    Map.of("value", value)));
        }
    }
    
    private void handleRateCommand(CommandSender sender, String value) {
        try {
            float newRate = Float.parseFloat(value);

            if (newRate < 0.0f || newRate > 1.0f) {
                sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                                FertileCrops.getInstance().getMessage("rate-invalid"));
                return;
            }

            getConfig().set("success-chance", newRate);
            saveConfig();
            loadConfigValues();

            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("rate-set",
                                    Map.of("rate", String.valueOf(newRate))));

        } catch (NumberFormatException e) {
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("invalid-number",
                                    Map.of("value", value)));
        }
    }

    private void handleRadiusCommand(CommandSender sender, String value) {
        try {
            int newRadius = Integer.parseInt(value);
            if (newRadius < 1) {
                sender.sendMessage(getMessage("prefix") + getMessage("radius-invalid"));
                return;
            }

            getConfig().set("spread-radius", newRadius);
            saveConfig();
            loadConfigValues();

            sender.sendMessage(getMessage("prefix") + getMessage("radius-set", Map.of("radius", String.valueOf(newRadius))));
        } catch (NumberFormatException e) {
            sender.sendMessage(getMessage("prefix") + getMessage("invalid-number", Map.of("value", value)));
        }
    }

    public class FertileCropsCommand implements TabCompleter {
        private final FertileCrops plugin;

        public FertileCropsCommand(FertileCrops plugin) {
            this.plugin = plugin;
        }

        @Override
        public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
            if (args.length == 1) {
                return List.of("crop", "withered", "cost", "rate", "radius", "reload");
            }

            if (args.length == 2) {
                switch (args[0].toLowerCase()) {
                    case "crop" -> { return List.of("add", "remove", "list"); }
                    case "withered" -> { return List.of("add", "remove", "list"); }
                    case "cost" -> { return List.of("<xp>"); }
                    case "rate" -> { return List.of("<0.0-1.0>"); }
                    case "radius" -> { return List.of("<blocks>"); }
                }
            }

            return Collections.emptyList();
        }
    }

    public boolean isCropAllowed(Material mat) {
        return allowedCrops.getOrDefault(mat, false);
    }


    public static FertileCrops getInstance() {
        return instance;
    }

    public int getSpreadRadius() {
        return spreadRadius;
    }

    public double getSuccessChance() {
        return successChance;
    }

    public int getXpCost() {
        return xpCost;
    }
    
}
