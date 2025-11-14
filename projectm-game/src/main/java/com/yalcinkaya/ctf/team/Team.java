package com.yalcinkaya.ctf.team;

import com.yalcinkaya.ctf.user.CTFUser;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
public class Team {

    private final TeamColor color;
    private final Set<CTFUser> members = new HashSet<>();

    public Team(TeamColor color) {
        this.color = color;
    }

    public void addMember(CTFUser user) {
        if (!isMember(user)) {
            members.add(user);
        }
    }

    public void removeMember(CTFUser user) {
        if (isMember(user)) {
            members.remove(getMember(user));
        }
    }

    public boolean isMember(CTFUser user) {
        return getMember(user) != null;
    }

    private CTFUser getMember(CTFUser user) {
        if (user == null) return null;
        return members.stream().filter(member -> member.getUuid().equals(user.getUuid())).findFirst().orElse(null);
    }

}
