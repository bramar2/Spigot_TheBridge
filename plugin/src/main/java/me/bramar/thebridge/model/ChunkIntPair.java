package me.bramar.thebridge.model;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter @AllArgsConstructor @EqualsAndHashCode
public class ChunkIntPair {
    private final int x, z;
}
