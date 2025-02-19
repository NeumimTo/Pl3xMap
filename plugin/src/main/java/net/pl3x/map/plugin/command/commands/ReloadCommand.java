package net.pl3x.map.plugin.command.commands;

import cloud.commandframework.context.CommandContext;
import cloud.commandframework.minecraft.extras.MinecraftExtrasMetaKeys;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.pl3x.map.plugin.Pl3xMapPlugin;
import net.pl3x.map.plugin.command.CommandManager;
import net.pl3x.map.plugin.command.Pl3xMapCommand;
import net.pl3x.map.plugin.configuration.Advanced;
import net.pl3x.map.plugin.configuration.Config;
import net.pl3x.map.plugin.configuration.Lang;
import net.pl3x.map.plugin.util.FileUtil;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.PluginDescriptionFile;
import org.checkerframework.checker.nullness.qual.NonNull;

public final class ReloadCommand extends Pl3xMapCommand {

    public ReloadCommand(final @NonNull Pl3xMapPlugin plugin, final @NonNull CommandManager commandManager) {
        super(plugin, commandManager);
    }

    @Override
    public void register() {
        this.commandManager.registerSubcommand(builder ->
                builder.literal("reload")
                        .meta(MinecraftExtrasMetaKeys.DESCRIPTION, MiniMessage.miniMessage().deserialize(Lang.RELOAD_COMMAND_DESCRIPTION))
                        .permission("pl3xmap.command.reload")
                        .handler(this::execute));
    }

    public void execute(final @NonNull CommandContext<CommandSender> context) {
        final CommandSender sender = context.getSender();
        plugin.stop();

        Config.reload();
        Advanced.reload();
        Lang.reload();
        FileUtil.reload();

        plugin.start();

        PluginDescriptionFile desc = plugin.getDescription();
        Lang.send(sender, Lang.PLUGIN_RELOADED,
                Placeholder.unparsed("name", desc.getName()),
                Placeholder.unparsed("version", desc.getVersion())
        );
    }
}
