package org.gabooj.players.chat;

import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class ChatSettings implements ConfigurationSerializable {

    // Prefix
    public String prefix = "";
    public TextColor prefixHexColor = TextColor.color(0xFFFFFF);
    public Set<TextDecoration> prefixDecorations = new HashSet<>();

    // Nickname
    public String nickname = "";
    public TextColor nicknameHexColor = TextColor.color(0xFFFFFF);
    public Set<TextDecoration> nicknameDecorations = new HashSet<>();

    // Suffix
    public String suffix = "";
    public TextColor suffixHexColor = TextColor.color(0xFFFFFF);
    public Set<TextDecoration> suffixDecorations = new HashSet<>();

    // Message
    public TextColor messageHexColor = TextColor.color(0xFFFFFF);
    public Set<TextDecoration> messageDecorations = new HashSet<>();

    public ChatSettings() {}

    public ChatSettings(Map<String, Object> map) {
        this.prefix = (String) map.getOrDefault("prefix", "");
        this.prefixHexColor = parseHex((String) map.getOrDefault("prefixColor", "#FFFFFF"));
        this.prefixDecorations = deserializeDecorations(map.get("prefixDecorations"));

        this.nickname = (String) map.getOrDefault("nickname", "");
        this.nicknameHexColor = parseHex((String) map.getOrDefault("nicknameColor", "#FFFFFF"));
        this.nicknameDecorations = deserializeDecorations(map.get("nicknameDecorations"));

        this.suffix = (String) map.getOrDefault("suffix", "");
        this.suffixHexColor = parseHex((String) map.getOrDefault("suffixColor", "#FFFFFF"));
        this.suffixDecorations = deserializeDecorations(map.get("suffixDecorations"));

        this.messageHexColor = parseHex((String) map.getOrDefault("messageColor", "#FFFFFF"));
        this.messageDecorations = deserializeDecorations(map.get("messageDecorations"));
    }

    @Override
    public @NotNull Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();

        map.put("prefix", prefix);
        map.put("prefixColor", prefixHexColor.asHexString());
        map.put("prefixDecorations", serializeDecorations(prefixDecorations));

        map.put("nickname", nickname);
        map.put("nicknameColor", nicknameHexColor.asHexString());
        map.put("nicknameDecorations", serializeDecorations(nicknameDecorations));

        map.put("suffix", suffix);
        map.put("suffixColor", suffixHexColor.asHexString());
        map.put("suffixDecorations", serializeDecorations(suffixDecorations));

        map.put("messageColor", messageHexColor.asHexString());
        map.put("messageDecorations", serializeDecorations(messageDecorations));

        return map;
    }

    private static TextColor parseHex(String hex) {
        if (hex == null || hex.isBlank()) return TextColor.color(0xFFFFFF);

        if (hex.startsWith("0x") || hex.startsWith("0X")) {
            hex = "#" + hex.substring(2);
        } else if (!hex.startsWith("#")) {
            hex = "#" + hex;
        }

        return TextColor.fromHexString(hex);
    }

    private static List<String> serializeDecorations(Set<TextDecoration> set) {
        return set.stream()
                .map(dec -> dec.name().toLowerCase())
                .toList();
    }

    private static Set<TextDecoration> deserializeDecorations(Object obj) {
        if (!(obj instanceof List<?> list)) return new HashSet<>();

        Set<TextDecoration> result = new HashSet<>();
        for (Object o : list) {
            if (o instanceof String s) {
                TextDecoration dec = TextDecoration.NAMES.value(s.toLowerCase());
                if (dec != null) result.add(dec);
            }
        }
        return result;
    }

}
