package com.tivit.snap_api.core;

import java.util.*;

public class SnapRegistry {
    private static final Map<String, SnapResourceMeta> registry = new HashMap<>();

    public static void register(String path, SnapResourceMeta meta) {
        registry.put(normalizePath(path), meta);
    }

    public static SnapResourceMeta getMetaFor(String path) {
        return registry.get(normalizePath(path));
    }

    public static Collection<SnapResourceMeta> getAll() {
        return registry.values();
    }

    private static String normalizePath(String path) {
        return path.startsWith("/") ? path.substring(1) : path;
    }
}