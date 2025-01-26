package net.elytraautopilot.utils;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.elytraautopilot.config.ModConfig;
import net.elytraautopilot.xearomapintegration.XearomapWaypointReader;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;

import java.util.concurrent.CompletableFuture;

public class CommandSuggestionProvider implements SuggestionProvider<FabricClientCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<FabricClientCommandSource> context, SuggestionsBuilder builder) {
        var locations = ModConfig.INSTANCE.flyLocations;

        for (String s : locations) {
            String[] tokens = s.split(";");
            builder.suggest(tokens[0]);
        }

        var waypoints = XearomapWaypointReader.GetXearomapWaypoints();
        if(waypoints != null) {
            for (String s : waypoints) {
                String[] tokens = s.split(";");
                builder.suggest('"'+tokens[0]+'"');
            }
        }

        return builder.buildFuture();
    }
}
