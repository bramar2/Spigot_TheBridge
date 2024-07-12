package me.bramar.thebridge.model;

import lombok.Getter;

public enum GameState {
    REGENERATING("Regenerating", -1),
    IDLE("Idle", 0),
    STARTED("Started", 1, true),
    SCORING("Scoring", 2, true),
    IN_BOX("In Box", 3, true);

    final boolean start;
    @Getter final String name;
    @Getter final int id;
    GameState(String name, int i) {
        this(name, i, false);
    }
    GameState(String name, int i, boolean start) {
        this.id = i;
        this.name = name;
        this.start = start;
    }
    public boolean hasStarted() {
        return start;
    }
}
