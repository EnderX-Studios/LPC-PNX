package com.enderx.lpc;

import cn.nukkit.Player;
import cn.nukkit.command.Command;
import cn.nukkit.command.CommandSender;
import cn.nukkit.event.EventHandler;
import cn.nukkit.event.Listener;
import cn.nukkit.event.player.PlayerChatEvent;
import cn.nukkit.lang.PluginI18n;
import cn.nukkit.plugin.PluginBase;
import cn.nukkit.utils.*;
import cn.nukkit.lang.PluginI18nManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.user.User;

import java.text.Format;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LPC extends PluginBase implements Listener {


    private static final Pattern HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");

    private LuckPerms luckPerms;

    @Override
    public void onEnable() {
        this.luckPerms = LuckPermsProvider.get();

        try {
            this.luckPerms = LuckPermsProvider.get();
        } catch (IllegalStateException e) {
            getLogger().error("LuckPerms don't found! Disabling plugin..");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }


        saveDefaultConfig();
        getServer().getPluginManager().registerEvents(this, this);
    }


    @Override
    public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
        if (args.length == 1 && "reload".equals(args[0])) {
            reloadConfig();

            sender.sendMessage(colorize("&aLPC has been reloaded."));
            return true;
        }

        return false;
    }
    
    @EventHandler
    public void onChat(final PlayerChatEvent event) {
        final String message = event.getMessage();
        final Player player = event.getPlayer();

        // Get LuckPerms metadata for the player
        final User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return;
        }
        final CachedMetaData metaData = user.getCachedData().getMetaData();
        final String group = metaData.getPrimaryGroup();


        String format = getConfig().getString("group-formats." + group);
        if (format == null) {
            format = getConfig().getString("chat-format");
        }

        if (format == null || format.isEmpty()) {
            return;
        }

        // Replace placeholders with actual values
        format = format
                .replace("{prefix}", metaData.getPrefix() != null ? metaData.getPrefix() : "")
                .replace("{suffix}", metaData.getSuffix() != null ? metaData.getSuffix() : "")
                .replace("{name}", player.getName())
                .replace("{displayname}", player.getDisplayName())
                .replace("{username-color}", metaData.getMetaValue("username-color") != null ? metaData.getMetaValue("username-color") : "")
                .replace("{message-color}", metaData.getMetaValue("message-color") != null ? metaData.getMetaValue("message-color") : "");

        format = colorize(translateHexColorCodes(format));

        event.setFormat(format.replace("{message}", player.hasPermission("lpc.colorcodes") && player.hasPermission("lpc.rgbcodes")
                ? colorize(translateHexColorCodes(message)) : player.hasPermission("lpc.colorcodes") ? colorize(message) : player.hasPermission("lpc.rgbcodes")
                ? translateHexColorCodes(message) : message).replace("{", "{}"));
    }

    private String colorize(final String message) {
        return message.replace('&', '§');
    }


    private String translateHexColorCodes(final String message) {
        final char colorChar = '§';

        final Matcher matcher = HEX_PATTERN.matcher(message);
        final StringBuffer buffer = new StringBuffer(message.length() + 4 * 8);

        while (matcher.find()) {
            final String group = matcher.group(1);

            matcher.appendReplacement(buffer, colorChar + "x"
                    + colorChar + group.charAt(0) + colorChar + group.charAt(1)
                    + colorChar + group.charAt(2) + colorChar + group.charAt(3)
                    + colorChar + group.charAt(4) + colorChar + group.charAt(5));
        }

        return matcher.appendTail(buffer).toString();
    }
}