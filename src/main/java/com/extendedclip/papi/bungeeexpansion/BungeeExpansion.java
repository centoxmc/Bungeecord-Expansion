package com.extendedclip.papi.bungeeexpansion;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import me.clip.placeholderapi.expansion.Configurable;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import me.clip.placeholderapi.expansion.Taskable;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.bukkit.scheduler.BukkitTask;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class BungeeExpansion extends PlaceholderExpansion implements PluginMessageListener, Taskable, Configurable {

    private static final String MESSAGE_CHANNEL = "BungeeCord";
    private static final String SERVERS_CHANNEL = "GetServers";
    private static final String PLAYERS_CHANNEL = "PlayerCount";
    private static final String ADDRESS_CHANNEL = "ServerIP";
    private static final String CONFIG_INTERVAL = "check_interval";

    private static final Splitter SPLITTER = Splitter.on(",").trimResults();


    private final Map<String, BungeeServer>   servers = new HashMap<>();
    private final AtomicReference<BukkitTask> cached = new AtomicReference<>();


    @Override
    public String getIdentifier() {
        return "bungee";
    }

    @Override
    public String getAuthor() {
        return "clip";
    }

    @Override
    public String getVersion() {
        return "2.0";
    }

    @Override
    public Map<String, Object> getDefaults() {
        return Collections.singletonMap(CONFIG_INTERVAL, 30);
    }


    @Override
    public String onRequest(final OfflinePlayer player, String identifier) {
        String[] arguments = identifier.split("_");
        if(arguments.length <= 1)
            return null;
        switch (arguments[0]){
            case "status":
                return String.valueOf(getServerStatus(arguments[1]));
            case "players":
                return String.valueOf(getPlayersOnline(arguments[1]));
            default:
                return null;
        }
    }

    public int getPlayersOnline(String server){
        if(server.equalsIgnoreCase("total"))
            return servers.values().stream().mapToInt(BungeeServer::getPlayers).sum();
        return servers.containsKey(server.toLowerCase()) ? servers.get(server.toLowerCase()).getPlayers() : 0;
    }

    public boolean getServerStatus(String server){
        return servers.containsKey(server.toLowerCase()) && servers.get(server.toLowerCase()).isOnline();
    }


    @Override
    public void start() {
        final BukkitTask task = Bukkit.getScheduler().runTaskTimer(getPlaceholderAPI(), () -> {

            if (servers.isEmpty()) sendServersChannelMessage();
            else {
                servers.forEach((key, value) -> {
                    sendPlayersChannelMessage(key);
                    value.checkStatus();
                });
            }


        }, 20L * 2L, 20L * getLong(CONFIG_INTERVAL, 30));


        final BukkitTask prev = cached.getAndSet(task);
        if (prev != null) {
            prev.cancel();
        } else {
            Bukkit.getMessenger().registerOutgoingPluginChannel(getPlaceholderAPI(), MESSAGE_CHANNEL);
            Bukkit.getMessenger().registerIncomingPluginChannel(getPlaceholderAPI(), MESSAGE_CHANNEL, this);
        }
    }

    @Override
    public void stop() {
        final BukkitTask prev = cached.getAndSet(null);
        if (prev == null) {
            return;
        }

        prev.cancel();
        servers.clear();

        Bukkit.getMessenger().unregisterOutgoingPluginChannel(getPlaceholderAPI(), MESSAGE_CHANNEL);
        Bukkit.getMessenger().unregisterIncomingPluginChannel(getPlaceholderAPI(), MESSAGE_CHANNEL, this);
    }


    @Override
    public void onPluginMessageReceived(final String channel, final Player player, final byte[] message) {
        if (!MESSAGE_CHANNEL.equals(channel)) {
            return;
        }

        //noinspection UnstableApiUsage
        final ByteArrayDataInput in = ByteStreams.newDataInput(message);
        switch (in.readUTF()) {
            case PLAYERS_CHANNEL:
                BungeeServer pc_server = servers.get(in.readUTF());
                if(pc_server != null)
                    pc_server.setPlayers(in.readInt());
                break;
            case SERVERS_CHANNEL:
                SPLITTER.split(in.readUTF()).forEach(serverName -> {
                    servers.putIfAbsent(serverName.toLowerCase(), new BungeeServer(serverName));
                    sendAddressChannelMessage(serverName);
                });
                break;
            case ADDRESS_CHANNEL:
                BungeeServer ac_server = servers.get(in.readUTF());
                if(ac_server != null){
                    ac_server.setIp(in.readUTF());
                    ac_server.setPort(in.readUnsignedShort());
                }
                break;
        }
    }


    private void sendServersChannelMessage() {
        sendMessage(SERVERS_CHANNEL, out -> { });
    }

    private void sendPlayersChannelMessage(final String serverName) {
        sendMessage(PLAYERS_CHANNEL, out -> out.writeUTF(serverName));
    }

    private void sendAddressChannelMessage(final String serverName){
        sendMessage(ADDRESS_CHANNEL, out -> out.writeUTF(serverName));
    }

    private void sendMessage(final String channel, final Consumer<ByteArrayDataOutput> consumer) {
        final Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null) {
            return;
        }

        //noinspection UnstableApiUsage
        final ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF(channel);

        consumer.accept(out);

        player.sendPluginMessage(getPlaceholderAPI(), MESSAGE_CHANNEL, out.toByteArray());
    }

}