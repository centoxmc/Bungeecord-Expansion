package com.extendedclip.papi.bungeeexpansion;

import java.net.InetSocketAddress;
import java.net.Socket;

public class BungeeServer {

    private final String name;

    private String ip;
    private int port;

    private boolean online = false;
    private int players = 0;

    public BungeeServer(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public boolean isOnline() {
        return online;
    }

    public int getPlayers() {
        return players;
    }

    public int getPort() {
        return port;
    }

    public String getIp() {
        return ip;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setPlayers(int players) {
        this.players = players;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void checkStatus(){
        try{
            Socket s = new Socket();
            s.connect(new InetSocketAddress(getIp(), getPort()), 10);
            s.close();
            setOnline(true);
        } catch (Exception ignored){
            setOnline(false);
        }
    }

    @Override
    public String toString() {
        return "BungeeServer{" +
                "name='" + name + '\'' +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                ", online=" + online +
                ", players=" + players +
                '}';
    }
}
