package org.erze.aestas;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MiniGameManager implements Listener {
    private FileConfiguration minigameConfig;
    private JavaPlugin plugin;
    private Map<Player, String> playersInMinigame;
    private Map<Player, Long> minigameEndTime;
    private Map<Player, Long> lastInteractTime;

    public MiniGameManager(FileConfiguration minigameConfig, JavaPlugin plugin) {
        this.minigameConfig = minigameConfig;
        this.plugin = plugin;
        this.playersInMinigame = new HashMap<>();
        this.minigameEndTime = new HashMap<>();
        this.lastInteractTime = new HashMap<>();
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    public void tryStartMinigame(Player player) {
        double probability = minigameConfig.getDouble("minigame_probability") * 0.01;
        double randomValue = new Random().nextDouble();

        if (randomValue < probability) {
            startMinigame(player);
        }
    }

    private void startMinigame(Player player) {
        int minPattern = minigameConfig.getInt("min_minigame");
        int maxPattern = minigameConfig.getInt("max_minigame");
        int patternLength = new Random().nextInt(maxPattern - minPattern + 1) + minPattern;

        StringBuilder pattern = new StringBuilder();
        for (int i = 0; i < patternLength; i++) {
            pattern.append(new Random().nextBoolean() ? "L" : "R");
        }

        player.sendTitle("MiniGame Started", "Press " + pattern.toString(), 10, 70, 20);
        playersInMinigame.put(player, pattern.toString());

        if (minigameConfig.getInt("minigame_time") != 0) {
            long endTime = System.currentTimeMillis() + minigameConfig.getLong("minigame_time") * 1000;
            minigameEndTime.put(player, endTime);

            int taskID = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                @Override
                public void run() {
                    if (isInMinigame(player)) {
                        failMinigame(player);
                    }
                }
            }, minigameConfig.getLong("minigame_time") * 20);
        }
    }

    public boolean isInMinigame(Player player) {
        if (minigameEndTime.containsKey(player) && System.currentTimeMillis() > minigameEndTime.get(player)) {
            failMinigame(player);
        }
        return playersInMinigame.containsKey(player);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        long currentTime = System.currentTimeMillis();

        if (lastInteractTime.containsKey(player) && currentTime - lastInteractTime.get(player) < 200) {
            return;
        }

        lastInteractTime.put(player, currentTime);

        if (isInMinigame(player)) {
            String pattern = playersInMinigame.get(player);
            char firstChar = pattern.charAt(0);
            Action action = event.getAction();

            if ((firstChar == 'L' && action == Action.LEFT_CLICK_AIR) || (firstChar == 'R' && action == Action.RIGHT_CLICK_AIR)) {
                pattern = pattern.substring(1);

                if (pattern.isEmpty()) {
                    successMinigame(player);
                } else {
                    playersInMinigame.put(player, pattern);
                }
            } else {
                failMinigame(player);
            }
        }
    }

    private void successMinigame(Player player) {
        player.sendTitle("MiniGame", "Success", 10, 70, 20);
        playersInMinigame.remove(player);
        minigameEndTime.remove(player);

        ItemStack item = new ItemStack(Material.EMERALD, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName("§b[ TOKEN ]");
        List<String> lore = Arrays.asList("§f←--------- §e★§f ---------→", "§f등급: §bMYTH", "§f설명: 미니게임을 완료한 자에게 지급되는 토큰입니다.");
        meta.setLore(lore);
        item.setItemMeta(meta);
        player.getInventory().addItem(item);
    }

    private void failMinigame(Player player) {
        player.sendTitle("MiniGame", "Failed", 10, 70, 20);
        playersInMinigame.remove(player);
        minigameEndTime.remove(player);
    }
}
