package com.yalcinkaya.lobby.hotbar;

import com.yalcinkaya.lobby.Lobby;
import com.yalcinkaya.lobby.queue.queues.SoloQueue5v5;

public class QueueHotbarGUI implements HotbarGUI {

    private final InteractiveItem[] items = new InteractiveItem[9];

    public QueueHotbarGUI() {
        SoloQueue5v5 soloQueue5v5 = Lobby.getInstance().getQueueManager().getSoloQueue5v5();
        items[4] = new InteractiveItem(soloQueue5v5.getIcon(), soloQueue5v5::accept);
    }

    @Override
    public InteractiveItem[] getItems() {
        return items;
    }

}
