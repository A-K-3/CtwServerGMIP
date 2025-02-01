package net.craftersland.ctw.server.kits.items;

import net.craftersland.ctw.server.CTW;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ItemKitHandler {
    private final CTW ctw;
    private final List<String> kitKeys;
    private final List<ItemStack> items;

    public ItemKitHandler(final CTW ctw) {
        this.kitKeys = new LinkedList<String>();
        this.items = new LinkedList<ItemStack>();
        this.ctw = ctw;
        this.createItems();
    }

    public void sendKit(final Player p, final int slotClicked) {
        if (slotClicked + 1 <= this.kitKeys.size()) {
            final String kitKey = this.kitKeys.get(slotClicked);
            final Double initialBal = CTW.economy.getBalance(p);
            final boolean hasPermission = this.hasKitPermission(p, kitKey);
            final boolean hasAchievement = this.hasKitAchievement(p, kitKey);
            if (!hasPermission) {
                this.ctw.getSoundHandler().sendFailedSound(p.getLocation(), p);
                p.sendMessage(this.ctw.getKitConfigHandler().getString(kitKey + ".Requirements.Premission.NoPermissionMessage").replaceAll("&", "§"));
            } else if (!hasAchievement) {
                this.ctw.getSoundHandler().sendFailedSound(p.getLocation(), p);
                p.sendMessage(this.ctw.getLanguageHandler().getMessage("ChatMessages.KitLocked").replaceAll("&", "§"));
            } else if (initialBal >= this.ctw.getKitConfigHandler().getDouble(kitKey + ".Requirements.Price")) {
                CTW.economy.withdrawPlayer(p, this.ctw.getKitConfigHandler().getDouble(kitKey + ".Requirements.Price"));
                this.runKitCommands(p, kitKey);
                final Double finalBal = initialBal - this.ctw.getKitConfigHandler().getDouble(kitKey + ".Requirements.Price");
                this.ctw.getSoundHandler().sendItemPickupSound(p.getLocation(), p);
                final String s = this.ctw.getLanguageHandler().getMessage("ChatMessages.KitReceived").replaceAll("%balance%", new StringBuilder(String.valueOf(finalBal.intValue())).toString());
                p.sendMessage(s.replaceAll("&", "§"));
            } else {
                this.ctw.getSoundHandler().sendFailedSound(p.getLocation(), p);
                String s2 = this.ctw.getLanguageHandler().getMessage("ChatMessages.NotEnoughCoins").replaceAll("%coinsNeeded%", new StringBuilder(String.valueOf(this.ctw.getKitConfigHandler().getDouble(kitKey + ".Requirements.Price").intValue())).toString());
                s2 = s2.replaceAll("%balance%", new StringBuilder(String.valueOf(initialBal.intValue())).toString());
                p.sendMessage(s2.replaceAll("&", "§"));
            }
        }
    }

    private void runKitCommands(final Player p, final String kitKey) {
        if (!this.ctw.getKitConfigHandler().getStringList(kitKey + ".KitCommands").isEmpty()) {
            for (final String s : this.ctw.getKitConfigHandler().getStringList(kitKey + ".KitCommands")) {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), s.replaceAll("%player%", p.getName()));
            }
        }
    }

    public void reloadKits() {
        this.ctw.getKitConfigHandler().loadConfig();
        this.kitKeys.clear();
        this.items.clear();
        this.createItems();
    }

    public void addItemsToMenu(final Inventory inv, final Player p) {
        if (!this.kitKeys.isEmpty()) {
            for (final ItemStack i : this.items) {
                final Integer slot = this.items.indexOf(i);
                inv.setItem(slot, this.updateItem(i, p, this.kitKeys.get(slot)));
            }
        }
    }

    private ItemStack updateItem(final ItemStack i, final Player p, final String kitKey) {
        final ItemStack item = new ItemStack(i);
        final ItemMeta meta = item.getItemMeta();
        final boolean hasPermission = this.hasKitPermission(p, kitKey);
        final boolean hasAchievement = this.hasKitAchievement(p, kitKey);
        if (hasPermission && hasAchievement) {
            meta.setDisplayName(new StringBuilder().append(ChatColor.GREEN).append(ChatColor.BOLD).append(meta.getDisplayName()).toString());
        } else {
            meta.setDisplayName(new StringBuilder().append(ChatColor.RED).append(ChatColor.BOLD).append(meta.getDisplayName()).append(ChatColor.GRAY).append(" (").append(this.ctw.getLanguageHandler().getMessage("Words.Locked")).append(")").toString());
        }
        final ArrayList<String> lore = new ArrayList<String>(meta.getLore());
        if (!hasPermission) {
            for (final String s : this.ctw.getKitConfigHandler().getStringList(kitKey + ".DisplayItem.NoPermissionLore")) {
                lore.add(s.replaceAll("&", "§"));
            }
        } else if (!hasAchievement) {
            for (final String s : this.ctw.getKitConfigHandler().getStringList(kitKey + ".DisplayItem.NoAchievementLore")) {
                lore.add(s.replaceAll("&", "§"));
            }
        } else {
            for (final String s : this.ctw.getKitConfigHandler().getStringList(kitKey + ".DisplayItem.FinalLore")) {
                lore.add(s.replaceAll("&", "§"));
            }
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private Boolean hasKitAchievement(final Player p, final String kitKey) {
        if (this.ctw.getKitConfigHandler().getBoolean(kitKey + ".Requirements.Achievement.Enabled")) {
            return this.ctw.getPlayerHandler().hasAchievement(p, this.ctw.getKitConfigHandler().getString(kitKey + ".Requirements.Achievement.AchievementRequired"));
        }
        return true;
    }

    private Boolean hasKitPermission(final Player p, final String kitKey) {
        if (!this.ctw.getKitConfigHandler().getBoolean(kitKey + ".Requirements.Premission.Enabled")) {
            return true;
        }
        return p.hasPermission(this.ctw.getKitConfigHandler().getString(kitKey + ".Requirements.Premission.Node"));
    }

    private void createItems() {
        final List<String> enabledKits = this.ctw.getKitConfigHandler().getStringList("EnabledKits");
        if (!enabledKits.isEmpty()) {
            for (final String s : enabledKits) {
                try {
                    this.items.add(this.createItemStack(s));
                    this.kitKeys.add(s);
                } catch (Exception e) {
                    CTW.log.warning("Failed to add kit: " + s + " .Error: " + e.getMessage() + " .Details Below:");
                    e.printStackTrace();
                }
            }
        }
    }

    private ItemStack createItemStack(final String kitName) {
        final ItemStack item = new ItemStack(Material.getMaterial(this.ctw.getKitConfigHandler().getString(kitName + ".DisplayItem.Material")), 1, Short.parseShort(this.ctw.getKitConfigHandler().getString(kitName + ".DisplayItem.Data")));
        final ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(this.ctw.getKitConfigHandler().getString(kitName + ".DisplayItem.DisplayName"));
        final ArrayList<String> lore = new ArrayList<String>();
        for (final String s : this.ctw.getKitConfigHandler().getStringList(kitName + ".DisplayItem.BaseLore")) {
            lore.add(s.replaceAll("&", "§"));
        }
        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }
}
