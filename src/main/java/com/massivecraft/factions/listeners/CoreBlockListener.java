package com.massivecraft.factions.listeners;

import com.massivecraft.factions.*;
import com.massivecraft.factions.struct.Role;
import com.massivecraft.factions.util.ItemUtil;
import com.massivecraft.factions.util.MiscUtil;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.Bukkit;
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

        player.setItemInHand(null);
        e.setCancelled(true);
        if (fPlayer.getRole() != Role.LEADER) {
            fPlayer.msg(TL.CORE_CANT_PLACE);
            return;
        }

        if (faction.isCoreSet()) {
            fPlayer.msg(TL.CORE_ALREADY_SET);
            return;
        }
        boolean claimSuccess = fPlayer.attemptClaim(faction, new FLocation(e.getBlock().getLocation()), true, false);

        if (claimSuccess) {
            faction.setCoreSet(true);
            faction.setCore(e.getBlock().getLocation());

            for (FPlayer factionMember : faction.getFPlayersWhereOnline(true)) {
                factionMember.msg(TL.CORE_SET);
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent e) {
        Player player = e.getPlayer();
        FPlayer fPlayer = FPlayers.getInstance().getByPlayer(player);
        Faction faction = fPlayer.getFaction();
        Faction factionAt = Board.getInstance().getFactionAt(new FLocation(e.getBlock().getLocation()));
        Block block = e.getBlock();

        if (!block.getLocation().equals(factionAt.getCore())) return;

        e.setCancelled(true);
        if (faction == factionAt) {
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