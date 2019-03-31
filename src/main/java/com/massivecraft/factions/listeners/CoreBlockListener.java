package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.ItemUtil;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.util.TL;
import de.inventivegames.hologram.Hologram;
import de.inventivegames.hologram.HologramAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class CoreBlockListener implements Listener {
    private Set<Player> confirmingBreak = new HashSet<>();

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent e) {
        Player player = e.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        Faction factionAt = Board.getInstance().getFactionAt(new FLocation(e.getBlock().getLocation()));
        ItemStack hand = e.getItemInHand();
        ItemStack coreItem = ItemUtil.getItemFromConfig(SavageFactions.plugin.getConfig().getConfigurationSection("core.core-block"));

        if (!hand.isSimilar(coreItem) || faction.isWilderness() || faction.isWarZone() || faction.isSafeZone() || !factionAt.isWilderness()) return;

        if (fPlayer.getRole() != Role.LEADER) {
            e.setCancelled(true);
            fPlayer.msg(TL.CORE_CANT_PLACE);
            return;
        }

        if (faction.isCoreSet()) {
            e.setCancelled(true);
            fPlayer.msg(TL.CORE_ALREADY_SET);
            return;
        }
        boolean claimSuccess = fPlayer.attemptClaim(faction, new FLocation(e.getBlock().getLocation()), true, false);
        Hologram hologram = HologramAPI.createHologram(e.getBlock().getLocation().clone().add(0.5, 1, 0.5),
                ChatColor.translateAlternateColorCodes('&', SavageFactions.plugin.getConfig().getString("core.hologram", "&c%faction%'s Core")
                        .replace("%faction%", faction.getTag())));

        if (claimSuccess) {
            faction.setCoreSet(true);
            faction.setCore(e.getBlock().getLocation());
            SavageFactions.plugin.hologramMap.remove(faction);
            SavageFactions.plugin.hologramMap.put(faction, hologram);
            hologram.spawn();

            for (FPlayer factionMember : faction.getFPlayersWhereOnline(true)) {
                factionMember.msg(TL.CORE_SET);
            }
        } else {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        Faction factionAt = Board.getInstance().getFactionAt(new FLocation(e.getBlock().getLocation()));
        Block block = e.getBlock();
        Location currentLoc = block.getLocation();

        if (!factionAt.isCoreSet() || !currentLoc.equals(factionAt.getCore())) return;

        e.setCancelled(true);
        if (faction.getId().equals(factionAt.getId())) {
            if (fPlayer.getRole() != Role.LEADER) {
                fPlayer.msg(TL.CORE_CANT_BREAK);
                return;
            }

            if (confirmingBreak.contains(player)) {
                faction.setCoreSet(false);
                faction.setCoreLives(SavageFactions.plugin.getConfig().getInt("core.default-lives"));
                Board.getInstance().unclaimAll(faction.getId());
                confirmingBreak.remove(player);
                block.setType(Material.AIR);
                SavageFactions.plugin.hologramMap.get(faction).despawn();
                for (FPlayer factionMember : faction.getFPlayersWhereOnline(true)) {
                    factionMember.msg(TL.CORE_BROKEN);
                }
                return;
            }

            confirmingBreak.add(player);
            fPlayer.msg(TL.CORE_CONFIRM_BREAK);
            Bukkit.getScheduler().scheduleSyncDelayedTask(SavageFactions.plugin, () -> confirmingBreak.remove(player), 200L);
            return;
        }
        double factionLives = factionAt.getCoreLives();
        double maxLives = SavageFactions.plugin.getConfig().getInt("core.default-lives");
        String healthRemaining = MiscUtil.formatDouble(factionLives / maxLives);

        if (factionAt.getCoreLives() == 1) {
            factionAt.setCoreSet(false);
            factionAt.setCoreLives((int) maxLives);
            Board.getInstance().unclaimAll(factionAt.getId());
            block.setType(Material.AIR);
            SavageFactions.plugin.hologramMap.get(factionAt).despawn();

            for (FPlayer factionMember : factionAt.getFPlayersWhereOnline(true)) {
                factionMember.msg(TL.CORE_BROKEN);
            }

            Bukkit.broadcastMessage(TL.FACTION_RAIDED.toString()
            .replace("%defender%", factionAt.getTag())
            .replace("%attacker%", faction.getTag()));
            return;
        }

        factionAt.setCoreLives(factionAt.getCoreLives() - 1);

        for (FPlayer factionMember : factionAt.getFPlayersWhereOnline(true)) {
            factionMember.sendMessage(TL.CORE_LIVE_LOST.toString()
            .replace("%health%", healthRemaining));
        }
    }
}