package com.yalcinkaya.ctf.util;

import com.yalcinkaya.core.ProjectM;
import com.yalcinkaya.core.redis.QueueType;
import com.yalcinkaya.core.redis.RedisDataService;
import com.yalcinkaya.ctf.team.Team;
import com.yalcinkaya.ctf.user.CTFUser;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class EloCalculator {

    private static final int changePerRankDiff = 5;

    private QueueType queueType;

    public int getEloWin(CTFUser user, Team other) {
        return (int) Math.min(30, Math.max(0, 15 - 0.01 * changePerRankDiff * getDiffToAverage(user, other)));
    }

    public int getEloLoss(CTFUser user, Team other) {
        return (int) Math.max(-30, Math.min(0, -15 - 0.01 * changePerRankDiff * getDiffToAverage(user, other)));
    }

    private double getDiffToAverage(CTFUser user, Team team) {
        RedisDataService redisDataService = ProjectM.getInstance().getRedisDataService();
        int userElo = redisDataService.getElo(user.getUuid().toString(), queueType);
        int avgElo = (1 / team.getMembers().size()) * team.getMembers().stream().mapToInt(opp -> redisDataService.getElo(opp.getUuid().toString(), queueType)).sum();
        return userElo - avgElo;
    }
}
