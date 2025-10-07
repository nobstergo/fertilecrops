package me.FertileCrops;

import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;


import java.util.List;
import java.util.Random;

public class GrowthListener implements Listener {

    private final FertileCrops plugin;
    private final Random random = new Random();
    private List<String> getFailureBlocks() {
        return plugin.getConfig().getStringList("withered-blocks");
    }

    private String getMessage(String path, java.util.Map<String, String> placeholders) {
        return plugin.getMessage(path, placeholders);
    }

    private String getMessage(String path) {
        return plugin.getMessage(path);
    }

    public GrowthListener(FertileCrops plugin) {
        this.plugin = plugin;
    }


    @EventHandler
    public void onBoneMealUse(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK) return;
        if (event.getItem() == null || event.getItem().getType() != Material.BONE_MEAL) return;

        Block block = event.getClickedBlock();
        if (block == null) return;
        if (!(block.getBlockData() instanceof Ageable ageable)) return;
        if (ageable.getAge() != ageable.getMaximumAge()) return;

        Player player = event.getPlayer();
        Material cropType = block.getType();

        if (player.getGameMode() != GameMode.CREATIVE) {
            if (!plugin.isCropAllowed(cropType)) {
                player.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                                FertileCrops.getInstance().getMessage("not-allowed-crop"));
                return;
            }

            int xpCost = plugin.getXpCost();
            if (player.getTotalExperience() < xpCost) {
                player.sendMessage(FertileCrops.getInstance().getMessage("prefix") +
                                FertileCrops.getInstance().getMessage("not-enough-xp",
                                    Map.of("amount", String.valueOf(xpCost))));
                return;
            }

            player.giveExp(-xpCost);

            ItemStack boneMeal = event.getItem();
            boneMeal.setAmount(boneMeal.getAmount() - 1);
        }

        event.setCancelled(true);
        spreadNearby(block, player);
    }

    private void spreadNearby(Block origin, Player player) {
        int radius = plugin.getSpreadRadius();
        double chance = plugin.getSuccessChance();
        Material cropType = origin.getType();
        if (!plugin.isCropAllowed(cropType)) {
            player.sendMessage(ChatColor.RED + "This crop cannot be spread by Fertile Growth!");
            return;
        }

        Block originBlock = origin.getRelative(0, -1, 0); // block beneath the crop

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx*dx + dz*dz > radius*radius) continue; 
                Block target = originBlock.getRelative(dx, 0, dz);
                Material type = target.getType();


                if (type == Material.FARMLAND || type == Material.DIRT || type == Material.GRASS_BLOCK) {
                    Block above = target.getRelative(0, 1, 0); 

                    List<String> failureBlocks = getFailureBlocks();

                    if (!failureBlocks.isEmpty() && random.nextDouble() > chance) {
                        Material failMat = Material.matchMaterial(
                                failureBlocks.get(random.nextInt(failureBlocks.size()))
                        );
                        if (failMat != null) target.setType(failMat);
                        player.spawnParticle(Particle.CLOUD, above.getLocation().add(0.5, 1, 0.5), 5, 0.2, 0.2, 0.2, 0.01);
                        continue;
                    }

                    if (type != Material.FARMLAND) target.setType(Material.FARMLAND);

                    above.setType(origin.getType());
                    if (above.getBlockData() instanceof Ageable newCrop) {
                        newCrop.setAge(newCrop.getMaximumAge());
                        above.setBlockData(newCrop);
                    }
                    player.spawnParticle(Particle.HAPPY_VILLAGER, above.getLocation().add(0.5, 0.5, 0.5), 5);
                }
            }
        }
    }
}
