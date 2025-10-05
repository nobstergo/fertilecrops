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
import org.bukkit.ChatColor;




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

        getServer().getPluginManager().registerEvents(new GrowthListener(this), this);
        getLogger().info("FertileCrops v1.0 enabled!");
    }

    public String getMessage(String path, Map<String, String> placeholders) {
        String msg = getMessages().getString(path, "&cMissing message: " + path);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            msg = msg.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        return ChatColor.translateAlternateColorCodes('&', msg);
    }

    public String getMessage(String path) {
        return getMessage(path, Collections.emptyMap());
    }

    @Override
    public void onDisable() {
        getLogger().info("FertileCrops v1.0 disabled!");
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
                Material mat = Material.matchMaterial(key.toUpperCase());
                if (mat != null) {
                    allowedCrops.put(mat, config.getBoolean("allowed-crops." + key));
                }
            }
        }
        getLogger().info("Allowed crops loaded: " + allowedCrops.entrySet().stream()
            .filter(Map.Entry::getValue)
            .map(e -> e.getKey().name())
            .toList());
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
                sender.sendMessage(getMessage("prefix") + getMessage("reload-complete", Map.of("command", label)));
            }

            case "item" -> {
                if (args.length < 3) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.item", Map.of("command", label)));
                    return true;
                }
                handleItemCommand(sender, args[1], args[2]);
            }

            case "withered" -> {
                if (args.length < 3) {
                    sender.sendMessage(getMessage("prefix") + getMessage("usage.withered", Map.of("command", label)));
                    return true;
                }
                handleWitheredCommand(sender, args[1], args[2]);
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

            default -> sender.sendMessage(FertileCrops.getInstance().getMessage("prefix")
                                   + FertileCrops.getInstance().getMessage("unknown-subcommand",
                                         Map.of("subcommand", args[0])));
        }

        return true;
    }

    private void handleItemCommand(CommandSender sender, String action, String itemName) {
        Material mat = Material.matchMaterial(itemName.toUpperCase());
        if (mat == null) {
            sender.sendMessage(
                FertileCrops.getInstance().getMessage("prefix")
                + FertileCrops.getInstance().getMessage("unknown-material", Map.of("material", itemName))
            );
            return;
        }

        FileConfiguration config = getConfig();
        if (action.equalsIgnoreCase("add")) {
            config.set("allowed-crops." + mat.name().toLowerCase(), true);
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("allowed-add",
                                    Map.of("material", mat.name())));
        } else if (action.equalsIgnoreCase("remove")) {
            config.set("allowed-crops." + mat.name().toLowerCase(), false);
            sender.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                            FertileCrops.getInstance().getMessage("allowed-remove",
                                    Map.of("material", mat.name())));
        }
        saveConfig();
        loadConfigValues();
    }

    private void handleWitheredCommand(CommandSender sender, String action, String itemName) {
        Material mat = Material.matchMaterial(itemName.toUpperCase());
        if (mat == null) {
            sender.sendMessage(
                FertileCrops.getInstance().getMessage("prefix")
                + FertileCrops.getInstance().getMessage("unknown-material", Map.of("material", itemName))
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
