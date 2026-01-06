package org.gabooj.commands;

import org.bukkit.command.CommandSender;

import java.util.List;

public interface SubCommand {

    String name();
    List<String> aliases();

    boolean needsOp();
    boolean needsToBePlayer();
    String description(CommandSender sender);

    void execute(CommandSender sender, String[] args);

    List<String> tabComplete(CommandSender sender, String[] args);

}
