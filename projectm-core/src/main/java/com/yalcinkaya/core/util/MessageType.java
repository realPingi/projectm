package com.yalcinkaya.core.util;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum MessageType {

    WARNING("<red>Warning >> ", "<gray>", "<red>"),
    SUCCESS("<green>Success >> ", "<gray>", "<green>"),
    INFO("<light_purple>Info >> ", "<gray>", "<light_purple>"),
    BROADCAST("<gold>ProjectM >> ", "<gray>", "<gold>");

    private String prefix;
    private String base;
    private String highlight;
}
