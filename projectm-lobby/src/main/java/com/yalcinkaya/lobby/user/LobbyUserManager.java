package com.yalcinkaya.lobby.user;

import java.util.HashMap;
import java.util.UUID;

public class LobbyUserManager {
    private HashMap<UUID, LobbyUser> users = new HashMap<>();

    public LobbyUser getUser(UUID uuid) {
        return users.get(uuid);
    }

    public void addUser(UUID uuid) {
        users.put(uuid, new LobbyUser(uuid));
    }

    public void removeUser(UUID uuid) {
        users.remove(uuid);
    }

    public boolean isRegistered(UUID uuid) {
        return users.containsKey(uuid);
    }

}
