package com.massivecraft.factions.util;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ItemUtil {

    public static ItemStack getItemFromConfig(ConfigurationSection configurationSection) {
        ItemStack itemStack = new ItemStack(Material.valueOf(configurationSection.getString("type", "WHITE_STAINED_GLASS_PANE").toUpperCase()), configurationSection.getInt("amount", 1));
        ItemMeta itemMeta = itemStack.getItemMeta();

        if (configurationSection.contains("name")) {
            itemMeta.setDisplayName(ChatColor.translateAlternateColorCodes('&', configurationSection.getString("name")));
        }

        if (configurationSection.contains("lore")) {
            List<String> lore = new ArrayList<>();

            for (String line : configurationSection.getStringList("lore")) {
                lore.add(ChatColor.translateAlternateColorCodes('&', line));
            }

            itemMeta.setLore(lore);
        }

        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}