package net.craftersland.ctw.server.utils;

import net.craftersland.ctw.server.CTW;
import org.bukkit.entity.Player;

public class EconomyHandler {
    private final CTW ctw;

    public EconomyHandler(final CTW ctw) {
        this.ctw = ctw;
    }

    public void addCoins(final Player p, final Double amount) {
        if (this.ctw.isValutEnabled) {
            if (p.hasPermission("CTW.3xCoinMultiplier")) {
                CTW.economy.depositPlayer(p, amount * 3.0);
            } else if (p.hasPermission("CTW.2xCoinMultiplier")) {
                CTW.economy.depositPlayer(p, amount * 2.0);
            } else {
                CTW.economy.depositPlayer(p, amount);
            }
        }
    }

    public void takeCoins(final Player p, final Double amount) {
        if (this.ctw.isValutEnabled) {
            final Double balance = CTW.economy.getBalance(p);
            if (balance >= amount) {
                CTW.economy.withdrawPlayer(p, amount);
            } else {
                this.ctw.getSoundHandler().sendFailedSound(p.getLocation(), p);
                final String msg = this.ctw.getLanguageHandler().getMessage("ChatMessages.NotEnoughCoins").replaceAll("%coinsNeeded%", new StringBuilder(String.valueOf(amount.intValue())).toString());
                p.sendMessage(msg.replaceAll("%balance%", balance.toString()));
            }
        }
    }

    public Double getCoins(final Player p) {
        Double balance = 0.0;
        if (this.ctw.isValutEnabled) {
            balance = CTW.economy.getBalance(p);
        }
        return balance;
    }
}
