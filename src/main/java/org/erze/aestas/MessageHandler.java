package org.erze.aestas;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.List;

public class MessageHandler {
    private boolean getMessage;
    private List<String> messageGrade;
    private String message;
    private FileConfiguration config;

    public MessageHandler(boolean getMessage, List<String> messageGrade, String message, FileConfiguration config) {
        this.getMessage = getMessage;
        this.messageGrade = messageGrade;
        this.message = message;
        this.config = config;
    }

    public void sendMessage(Player player, String grade, String itemName) {
        if (getMessage && messageGrade.contains(grade)) {
            double radius = config.getDouble("radiusMessage");

            String colorCode = "";
            switch (grade.toLowerCase()) {
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

            String finalGrade = colorCode + grade.toUpperCase();

            String finalMessage = message.replace("{player}", player.getName())
                    .replace("{grade}", "§l" + finalGrade + "§r§f")
                    .replace("{itemName}", "§l" + itemName + "§f");

            if (radius > 0) {
                for (Player nearbyPlayer : player.getWorld().getPlayers()) {
                    if (player.getLocation().distance(nearbyPlayer.getLocation()) <= radius) {
                        nearbyPlayer.sendMessage(finalMessage);
                    }
                }
            } else {
                Bukkit.broadcastMessage(finalMessage);
            }
        }
    }
}
