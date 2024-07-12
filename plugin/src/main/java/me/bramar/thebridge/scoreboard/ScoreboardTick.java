package me.bramar.thebridge.scoreboard;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor @Getter
public class ScoreboardTick {
    @Setter private String title;
    private List<String> content;
    private boolean sameContent;
}
