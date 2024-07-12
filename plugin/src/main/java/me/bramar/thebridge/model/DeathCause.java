package me.bramar.thebridge.model;

public enum DeathCause {
    SHOT,
    VOID,
    MELEE,
    SHOT_VOID,
    THORNS, // because it can also be caused by other players
    OTHER;
}
