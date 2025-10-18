package com.yalcinkaya.lobby.menu.menus;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.menu.Menu;
import com.yalcinkaya.lobby.util.LobbyUtil;
import com.yalcinkaya.lobby.util.QueueUtil;

public class QueueMenu extends Menu {

    public QueueMenu(int size, String name) {
        super(size, name);
    }

    @Override
    public void refresh() {
        QueueUtil.addQueueItem(this, LobbyUtil.slotFromRowCol(1, 4), Lobby.getInstance().getQueueManager().getSoloQueue5v5());
    }
}
