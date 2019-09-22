package me.mmis1000.mindustryGameCTL;

import io.anuke.arc.Core;
import io.anuke.arc.Events;
import io.anuke.arc.collection.Array;
import io.anuke.arc.util.CommandHandler;
import io.anuke.arc.util.Log;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.GameState;
import io.anuke.mindustry.entities.type.Player;
import io.anuke.mindustry.game.EventType;
import io.anuke.mindustry.game.EventType.PlayerJoin;
import io.anuke.mindustry.game.Gamemode;
import io.anuke.mindustry.game.Team;
import io.anuke.mindustry.gen.Call;
import io.anuke.mindustry.maps.Map;
import io.anuke.mindustry.maps.MapException;
import io.anuke.mindustry.plugin.Plugin;

import java.io.IOException;
import java.net.BindException;

@SuppressWarnings("unused")
public class Main extends Plugin {

    //    register event handlers and create variables in the constructor
    @SuppressWarnings("unused")
    public Main() {
        //listen for a block selection event
        Events.on(PlayerJoin.class, event -> {
            if (Vars.state.getState() == GameState.State.paused) {
                event.player.sendMessage("[GameCTL] Game is being paused, use /play to continue the game");
            }

            if (!Vars.state.rules.waveTimer) {
                event.player.sendMessage("[GameCTL] Wave Timer is stopped currently, use /run-wave [on|off] to enable again");
            }
        });
    }

    @Override
    public void registerClientCommands(CommandHandler handler) {
        handler.<Player>register("pause", "Pause the game.", (args, player) -> {
            if (Vars.state.getState() == GameState.State.paused) {
                player.sendMessage("Game is already paused, use /play to continue");
                return;
            }
            Vars.state.set(GameState.State.paused);
            Call.sendMessage("[GameCTL] Game being paused, use /play to continue");
        });

        handler.<Player>register("play", "Continue the game.", (args, player) -> {
            if (Vars.state.getState() == GameState.State.playing) {
                player.sendMessage("Game is already playing");
                return;
            }
            Vars.state.set(GameState.State.playing);
            Call.sendMessage("[GameCTL] Game continues");
        });

        handler.<Player>register("run-wave", "[on/off]", "Toggle wave run.", (arg, player) -> {
            if (arg.length == 0) {
                player.sendMessage("[GameCTL] Wave Timer is '" + Vars.state.rules.waveTimer + "' currently, use /run-wave [on|off] to toggle");
                return;
            }

            boolean value = arg[0].equalsIgnoreCase("on");

            Vars.state.rules.waveTimer = value;
            Call.sendMessage("Wave Timer toggled to '" + value + "'.");
        });

        handler.<Player>register("run-now", "Trigger the next wave.", (arg, player) -> {
            if (!player.isAdmin) {
                player.sendMessage("This command is admin only");
            }

            if (!Vars.state.is(GameState.State.playing)) {
                player.sendMessage("Not playing. Unpause first.");
            } else {
                Vars.logic.runWave();
                Call.sendMessage("Wave spawned.");
            }
        });

        handler.<Player>register("gameover", "Reset the game", (arg, player) -> {
            if (!player.isAdmin) {
                player.sendMessage("This command is admin only");
            }

            if (Vars.state.is(GameState.State.menu)) {
                Log.info("Not playing a map.");
                return;
            }

            Log.info("&lyCore destroyed.");

            Events.fire(new EventType.GameOverEvent(Team.crux));
        });
    }

    //register commands that player can invoke in-game
    @Override
    public void registerServerCommands(CommandHandler handler) {
        handler.<Player>register("pause", "Pause the game.", (args, player) -> {
            if (Vars.state.getState() == GameState.State.paused) {
                Log.info("Game is already paused, use /play to continue");
                return;
            }
            Vars.state.set(GameState.State.paused);
            Log.info("Game being paused, use /play to continue");
        });

        handler.<Player>register("play", "Continue the game.", (args, player) -> {
            if (Vars.state.getState() == GameState.State.playing) {
                Log.info("Game is already playing");
                return;
            }
            Vars.state.set(GameState.State.playing);
            Log.info("Game continues");
        });

        handler.<Player>register("run-wave", "[on/off]", "Toggle wave run.", (arg, player) -> {
            if (arg.length == 0) {
                Log.info("[GameCTL] Wave Timer is '" + Vars.state.rules.waveTimer + "' currently, use /run-wave [on|off] to toggle");
                return;
            }

            boolean value = arg[0].equalsIgnoreCase("on");

            Vars.state.rules.waveTimer = value;
            Log.info("Wave Timer toggled to '" + value + "'.");
        });

        handler.<Player>register("s", "Start random survival map", (arg, player) -> {
            if (Vars.state.getState() == GameState.State.playing) {
                Log.info("Game is already playing");
                return;
            }

            Array<Map> maps = Vars.maps.all();

            Map selected = maps.random();

            Gamemode preset = Gamemode.survival;
            try {
                Vars.world.loadMap(selected, selected.applyRules(preset));
                Vars.state.rules = selected.applyRules(preset);
                Vars.logic.play();

                Log.info("Map loaded.");

                try {
                    Vars.net.host(Core.settings.getInt("port"));
                    Log.info("&lcOpened a server on port {0}.", Core.settings.getInt("port"));
                } catch (BindException e) {
                    Log.err("Unable to host: Port already in use! Make sure no other servers are running on the same port in your network.");
                    Vars.state.set(GameState.State.menu);
                } catch (IOException e) {
                    Log.err(e);
                    Vars.state.set(GameState.State.menu);
                }
            } catch (MapException e) {
                Log.err(e.map.name() + ": " + e.getMessage());
            }
        });
    }
}
