package com.massivecraft.factions.cmd;

import com.massivecraft.factions.SavageFactions;
import com.massivecraft.factions.struct.Permission;
import com.massivecraft.factions.util.ItemUtil;
import com.massivecraft.factions.zcore.util.TL;
import org.bukkit.inventory.ItemStack;

public class CmdCore extends FCommand {

    public CmdCore() {
        this.aliases.add("core");
        this.permission = Permission.CORE.node;

        this.senderMustBePlayer = true;
        this.senderMustBeMember = false;
        this.senderMustBeModerator = false;
        this.senderMustBeColeader = false;
        this.senderMustBeAdmin = true;
    }

    @Override
    public void perform() {
        ItemStack coreItem = ItemUtil.getItemFromConfig(SavageFactions.plugin.getConfig().getConfigurationSection("core.core-block"));

        if (fme.getFaction().isCoreSet()) {
            fme.msg(TL.CORE_ALREADY_SET);
            return;
        }

        for (ItemStack itemStack : fme.getPlayer().getInventory().getContents()) {
            if (!itemStack.isSimilar(coreItem)) continue;

            fme.msg(TL.CORE_ALREADY_OWNED.toString());
            return;
        }

        fme.getPlayer().getInventory().addItem(coreItem);
        fme.msg(TL.CORE_RECEIVED);
    }

    @Override
    public TL getUsageTranslation() {
        return TL.COMMAND_CORE_DESCRIPTION;
    }
}