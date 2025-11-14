package com.yalcinkaya.ctf.team;

import com.yalcinkaya.ctf.user.CTFUser;
import lombok.Getter;

import java.util.HashSet;

@Getter
public class Team {

    private final TeamColor color;
    private final HashSet<CTFUser> members = new HashSet<>();

    public Team(TeamColor color) {
        this.color = color;
    }

    public void addMember(CTFUser user) {
        members.add(user);
    }

    public void removeMember(CTFUser user) {
        members.remove(user);
    }

    public boolean isMember(CTFUser user) {
        return members.contains(user);
    }

}
