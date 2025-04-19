package de.c3kay.beacon;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Beacon;
import org.bukkit.block.BlockState;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;


public class BeaconPlugin extends JavaPlugin {

    // When changing please also check config.yml
    final static String DISTANCE_KEY = "distance";
    final static String TIER_KEY = "tier";
    final static String MEASURE_KEY = "measure";
    final static long DELAY_TICKS = 200;
    final static long INTERVAL_TICKS = 100;

    /**
     * Plugin startup event.
     */
    @Override
    public void onEnable() {
        // Copy embedded config to disk if needed
        saveDefaultConfig();

        getLogger().info(
                "Distance: " + getConfig().getInt(DISTANCE_KEY)
                        + " - Tier: " + getConfig().getInt(TIER_KEY)
        );

        getServer().getScheduler().scheduleSyncRepeatingTask(
                this, this::beaconTask, DELAY_TICKS, INTERVAL_TICKS
        );

        getLogger().info("Beacon plugin enabled");
    }

    /**
     * Plugin shutdown event.
     */
    @Override
    public void onDisable() {
        getLogger().info("Beacon plugin disabled");
    }

    /**
     * Plugin command event.
     *
     * @param sender  Command sender
     * @param command Executed command
     * @param label   Alias for command
     * @param args    Arguments of command
     * @return true if command syntax correct, otherwise false
     */
    @Override
    public boolean onCommand(@NotNull CommandSender sender, Command command, @NotNull String label, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("beacon") && sender.hasPermission("beacon")) {
            // /beacon <distance|tier> [value]
            // => Config of distance and tier
            if (
                    args.length == 2
                            && (args[0].equalsIgnoreCase(DISTANCE_KEY)
                            || args[0].equalsIgnoreCase(TIER_KEY))
            ) {
                try {
                    int value = Integer.parseInt(args[1]);

                    getConfig().set(args[0].toLowerCase(), value);
                    saveConfig();

                    sender.sendMessage("New value for " + args[0].toLowerCase() + ": " + value);
                } catch (NumberFormatException e) {
                    sender.sendMessage("Invalid value!");
                }

                return true;
            }

            // /beacon <measure>
            // => Measure distance to nearest beacon
            if (args.length == 1 && args[0].equalsIgnoreCase(MEASURE_KEY)) {
                if (sender instanceof Player player) {
                    // Get the closest beacon in world of player
                    Optional<Double> minDistance = getBeacons(player.getWorld())
                            .map(BlockState::getLocation)
                            .map(location -> location.distanceSquared(player.getLocation()))
                            .min(Double::compareTo);

                    if (minDistance.isPresent()) {
                        player.sendMessage(
                                String.format("Closest beacon: %.1f", Math.sqrt(minDistance.get()))
                        );
                    } else {
                        player.sendMessage("No beacon found in this world!");
                    }
                } else {
                    sender.sendMessage("You need to be a Player for this command!");
                }

                return true;
            }
        }
        return false;
    }

    /**
     * Plugin command autocomplete event.
     *
     * @param sender  Command sender
     * @param command Executed command
     * @param label   Alias for command
     * @param args    Arguments of command
     * @return List of possible arguments or null if none
     */
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, String[] args) {
        if (args.length == 1) {
            return List.of(DISTANCE_KEY, TIER_KEY, MEASURE_KEY);
        }

        return null;
    }

    /**
     * Repeating task function (Runnable).
     * For each world which has players, apply beacon effects to players in range.
     */
    public void beaconTask() {
        getServer().getWorlds().stream()
                .filter(world -> !world.getPlayers().isEmpty())
                .flatMap(this::getBeacons)
                .forEach(beacon -> applyBeaconEffectsToPlayers(beacon, getPlayersInRange(beacon)));
    }

    /**
     * Get all beacons of a given world.
     *
     * @param world World where beacons should be searched.
     * @return Stream of beacons. Empty stream if no beacons found.
     */
    private Stream<Beacon> getBeacons(World world) {
        return Arrays.stream(world.getLoadedChunks())
                .flatMap(chunk -> Arrays.stream(chunk.getTileEntities()))
                .filter(tileEntity -> tileEntity.getType().equals(Material.BEACON))
                .map(tileEntity -> (Beacon) tileEntity);
    }

    /**
     * Get all players that are in range of given beacon.
     * Only players in the same world as the beacon are considered.
     *
     * @param beacon The beacon from which to measure the distance.
     * @return List of players. Empty list if none in range.
     */
    private List<Player> getPlayersInRange(Beacon beacon) {
        final int distance = getConfig().getInt(DISTANCE_KEY);
        return beacon.getWorld().getPlayers().stream()
                // Using squared distance to avoid costly root function
                .filter(player -> player.getLocation().distanceSquared(beacon.getLocation()) <= distance * distance)
                .toList();
    }

    /**
     * Apply primary and secondary potion effects of a beacon to given players.
     * The beacon must be at least MIN_TIER (height of pyramid).
     *
     * @param beacon  The beacon from which the effects should apply.
     * @param players The players to which the effects should be applied.
     */
    private void applyBeaconEffectsToPlayers(Beacon beacon, Iterable<Player> players) {
        if (beacon.getTier() >= getConfig().getInt(TIER_KEY)) {
            for (Player player : players) {
                if (beacon.getPrimaryEffect() != null) {
                    player.addPotionEffect(beacon.getPrimaryEffect());
                }

                if (beacon.getSecondaryEffect() != null) {
                    player.addPotionEffect(beacon.getSecondaryEffect());
                }
            }
        }
    }
}
