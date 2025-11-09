package com.yalcinkaya.ctf.user;

import lombok.Getter;

import java.util.HashMap;
import java.util.UUID;

@Getter
public class CTFUserManager {

    private HashMap<UUID, CTFUser> users = new HashMap<>();

    public CTFUser getUser(UUID uuid) {
        return users.get(uuid);
    }

    public void addUser(UUID uuid) {
        users.put(uuid, new CTFUser(uuid));
    }

    public boolean isRegistered(UUID uuid) {
        return users.containsKey(uuid);
    }

}
