package com.asledgehammer.candle.yamldoc;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class YamlConstructor extends YamlExecutable {

    final String name;
    final String[] modifiers;
    final String returnType;
    final String notes;

    YamlConstructor(@NotNull Map<String, Object> raw, String name) {
        super(raw);

        this.name = name;
        this.returnType = readString("returnType");
        this.modifiers = readModifiers();
        this.notes = readString("notes");
    }
}