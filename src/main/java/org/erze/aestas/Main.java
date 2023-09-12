package org.erze.aestas;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.FishHook;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Main extends JavaPlugin implements Listener {

    private File file;
    private File minigameFile;
    private FileConfiguration fishDataConfig;
    private FileConfiguration minigameConfig;
    private FileConfiguration config;
    private MessageHandler messageHandler;
    private MiniGameManager miniGameManager;

    @Override
    public void onEnable() {
        this.getServer().getPluginManager().registerEvents(this, this);

        saveDefaultConfig();
        reloadConfig();

        file = new File(getDataFolder(), "fishdata.yml");
        minigameFile = new File(getDataFolder(), "minigame.yml");
        config = this.getConfig();

        if (!file.exists()) {
            saveResource("fishdata.yml", false);
        }
        if (!minigameFile.exists()) {
            saveResource("minigame.yml", false);
        }

        fishDataConfig = YamlConfiguration.loadConfiguration(file);
        minigameConfig = YamlConfiguration.loadConfiguration(minigameFile);

        boolean getMessage = getConfig().getBoolean("getMessage", true);
        List<String> messageGrade = getConfig().getStringList("messageGrade");
        String message = getConfig().getString("Message");

        messageHandler = new MessageHandler(getMessage, messageGrade, message, config);
        miniGameManager = new MiniGameManager(minigameConfig, this);
    }

    @EventHandler
    public void onFish(PlayerFishEvent event) {
        if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
            FishHook hook = event.getHook();
            event.setCancelled(true);
            Player player = event.getPlayer();
            double totalProbability = 0.00;

            for (String key : fishDataConfig.getKeys(false)) {
                totalProbability += fishDataConfig.getDouble(key + ".probability");
            }

            double trashProbability = 100.00 - totalProbability;
            double randomValue = new Random().nextDouble() * 100.00;

            ItemStack item = null;
            String itemName = "";
            String itemLore = "";
            String grade = "";

            if (randomValue <= trashProbability) {
                item = new ItemStack(Material.DISC_FRAGMENT_5);
                itemName = "쓰레기";
                itemLore = "누군가가 물에 버린 쓰레기네요";
                grade = "!";
            } else {
                randomValue -= trashProbability;

                double sum = 0.0;
                for (String key : fishDataConfig.getKeys(false)) {
                    sum += fishDataConfig.getDouble(key + ".probability");
                    if (randomValue <= sum) {
                        item = new ItemStack(Material.valueOf(fishDataConfig.getString(key + ".type")));
                        itemName = key;
                        itemLore = fishDataConfig.getString(key + ".lore");
                        grade = fishDataConfig.getString(key + ".grade").toLowerCase();
                        break;
                    }
                }
            }

            if (item != null) {
                String colorCode = "";

                switch (grade) {
                    case "common":
                        colorCode = "§f";  // White
                        break;
                    case "uncommon":
                        colorCode = "§a";  // Green
                        break;
                    case "rare":
                        colorCode = "§9";  // Blue
                        break;
                    case "epic":
                        colorCode = "§5";  // Purple
                        break;
                    case "legendary":
                        colorCode = "§6";  // Orange
                        break;
                }

                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(colorCode + "[" + grade.toUpperCase() + "] " + itemName);

                /*List<String> lore = new ArrayList<>();

                lore.add(itemLore);
                */

                List<String> lore = Arrays.asList("§f←--------- §e★§f ---------→", "§f등급: " + colorCode + grade.toUpperCase(), "§f설명: " + itemLore);

                meta.setLore(lore);
                item.setItemMeta(meta);

                player.getInventory().addItem(item);
                miniGameManager.tryStartMinigame(event.getPlayer()); // 미니게임 시도
                messageHandler.sendMessage(player, grade, itemName); // 전체 메시지
                hook.remove(); // 찌 제거
                player.sendMessage(colorCode + "§l[" + grade.toUpperCase() + "]§r " + "§l" + itemName + "§r을(를) 획득하셨습니다");
            }
        }
    }

    public void saveFishDataConfig() {
        try {
            fishDataConfig.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
