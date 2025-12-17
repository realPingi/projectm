package com.yalcinkaya.ctf.user;

import lombok.Getter;

import java.util.HashMap;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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

    public Set<CTFUser> getSpecs() {
        return users.values().stream().filter(CTFUser::isSpectating).collect(Collectors.toSet());
    }

    public Set<CTFUser> getIngame() {
        return users.values().stream().filter(user -> !user.isSpectating()).collect(Collectors.toSet());
    }

}
