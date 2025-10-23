package com.yalcinkaya.core.redis;

import lombok.AllArgsConstructor;
import lombok.Getter;
import net.kyori.adventure.text.format.NamedTextColor;

@Getter
@AllArgsConstructor
public enum Rank {

    OWNER(NamedTextColor.DARK_RED),
    ADMIN(NamedTextColor.RED),
    MODERATOR(NamedTextColor.DARK_PURPLE),
    CONTENT_CREATOR(NamedTextColor.AQUA),
    DEFAULT(NamedTextColor.GRAY);

    private final NamedTextColor color;
}
