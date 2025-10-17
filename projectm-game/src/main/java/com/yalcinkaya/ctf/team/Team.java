package com.yalcinkaya.ctf.team;

import com.yalcinkaya.ctf.user.CTFUser;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Setter
@Getter
public class Team {

    private TeamColor color;
    private Set<CTFUser> members = new HashSet<>();

    public Team(TeamColor color) {
        this.color = color;
    }

}
