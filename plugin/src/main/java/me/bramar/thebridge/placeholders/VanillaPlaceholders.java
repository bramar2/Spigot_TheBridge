package me.bramar.thebridge.placeholders;

import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

public class VanillaPlaceholders {
    private final BridgePlaceholders placeholders;
    private final char head = '%';
    private final char tail = '%';
    public VanillaPlaceholders(BridgePlaceholders placeholders) {
        this.placeholders = placeholders;
    }

    // GPL-3.0 states that patent use, private use, commercial use and distribution of Software is allowed:
    // https://github.com/PlaceholderAPI/PlaceholderAPI/blob/master/LICENSE
    // CREDIT: github.com/PlaceholderAPI/PlaceholderAPI at me.clip.placeholderapi.replacer.CharsReplacer
    public String applyPlaceholders(@Nullable OfflinePlayer player, String text) {
        char[] chars = text.toCharArray();
        StringBuilder builder = new StringBuilder(text.length());
        StringBuilder identifier = new StringBuilder();
        StringBuilder parameters = new StringBuilder();

        label152:
        for(int i = 0; i < chars.length; ++i) {
            char l = chars[i];
            if (l == '&') {
                ++i;
                if (i < chars.length) {
                    char c = Character.toLowerCase(chars[i]);
                    if (c != '0' && c != '1' && c != '2' && c != '3' && c != '4' && c != '5' && c != '6' && c != '7' && c != '8' && c != '9' && c != 'a' && c != 'b' && c != 'c' && c != 'd' && c != 'e' && c != 'f' && c != 'k' && c != 'l' && c != 'm' && c != 'n' && c != 'o' && c != 'r' && c != 'x') {
                        builder.append(l).append(chars[i]);
                        continue;
                    }

                    builder.append('§');
                    if (c != 'x') {
                        builder.append(chars[i]);
                        continue;
                    }

                    if (i > 1 && chars[i - 2] == '\\') {
                        builder.setLength(builder.length() - 2);
                        builder.append('&').append(chars[i]);
                        continue;
                    }

                    builder.append(c);
                    int j = 0;

                    while(true) {
                        ++j;
                        if (j > 6 || i + j >= chars.length) {
                            if (j == 7) {
                                i += 6;
                            } else {
                                builder.setLength(builder.length() - j * 2);
                            }
                            continue label152;
                        }

                        char x = chars[i + j];
                        builder.append('§').append(x);
                    }
                }
            }

            if (l == head && i + 1 < chars.length) {
                boolean identified = false;
                boolean oopsitsbad = true;
                boolean hadSpace = false;

                while(true) {
                    ++i;
                    if (i >= chars.length) {
                        break;
                    }

                    char p = chars[i];
                    if (p == ' ' && !identified) {
                        hadSpace = true;
                        break;
                    }

                    if (p == tail) {
                        oopsitsbad = false;
                        break;
                    }

                    if (p == '_' && !identified) {
                        identified = true;
                    } else if (identified) {
                        parameters.append(p);
                    } else {
                        identifier.append(p);
                    }
                }

                String identifierString = identifier.toString().toLowerCase();
                String parametersString = parameters.toString();
                identifier.setLength(0);
                parameters.setLength(0);
                if (oopsitsbad) {
                    builder.append(head).append(identifierString);
                    if (identified) {
                        builder.append('_').append(parametersString);
                    }

                    if (hadSpace) {
                        builder.append(' ');
                    }
                } else {
                    boolean correctIdentifier = identifierString.equalsIgnoreCase("tb") ||
                            identifierString.equalsIgnoreCase("thebridge");
                    if (!correctIdentifier) {
                        builder.append(head).append(identifierString);
                        if (identified) {
                            builder.append('_');
                        }

                        builder.append(parametersString).append(tail);
                    } else {
                        // PlaceholderAPI's replaced with BridgePlaceholderse
                        String replacement = this.placeholders.onRequest(player, parametersString);
                        if (replacement == null) {
                            builder.append(head).append(identifierString);
                            if (identified) {
                                builder.append('_');
                            }

                            builder.append(parametersString).append(tail);
                        } else {
                            builder.append(ChatColor.translateAlternateColorCodes('&', replacement));
                        }
                    }
                }
            } else {
                builder.append(l);
            }
        }

        return builder.toString();
    }
}
