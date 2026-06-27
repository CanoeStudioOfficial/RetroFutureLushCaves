package com.canoestudio.retrofuturemc.contents.world.gen;

final class RetroFutureCaveNoise {
    private final long seed;
    private final int octaves;

    RetroFutureCaveNoise(long worldSeed, String salt, int octaves) {
        this.seed = mix64(worldSeed ^ hashString(salt));
        this.octaves = Math.max(1, octaves);
    }

    double getValue(double x, double y, double z) {
        double total = 0.0D;
        double max = 0.0D;
        double amplitude = 1.0D;
        double frequency = 1.0D;

        for (int octave = 0; octave < octaves; octave++) {
            total += sample(x * frequency, y * frequency, z * frequency, octave) * amplitude;
            max += amplitude;
            amplitude *= 0.5D;
            frequency *= 2.0D;
        }

        return total / max;
    }

    private double sample(double x, double y, double z, int octave) {
        int x0 = fastFloor(x);
        int y0 = fastFloor(y);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int y1 = y0 + 1;
        int z1 = z0 + 1;
        double tx = smooth(x - x0);
        double ty = smooth(y - y0);
        double tz = smooth(z - z0);
        long octaveSeed = seed + octave * 0x9E3779B97F4A7C15L;

        double x00 = lerp(tx, value(octaveSeed, x0, y0, z0), value(octaveSeed, x1, y0, z0));
        double x10 = lerp(tx, value(octaveSeed, x0, y1, z0), value(octaveSeed, x1, y1, z0));
        double x01 = lerp(tx, value(octaveSeed, x0, y0, z1), value(octaveSeed, x1, y0, z1));
        double x11 = lerp(tx, value(octaveSeed, x0, y1, z1), value(octaveSeed, x1, y1, z1));
        double y0v = lerp(ty, x00, x10);
        double y1v = lerp(ty, x01, x11);

        return lerp(tz, y0v, y1v);
    }

    private static int fastFloor(double value) {
        int i = (int)value;
        return value < i ? i - 1 : i;
    }

    private static double smooth(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double delta, double start, double end) {
        return start + delta * (end - start);
    }

    private static double value(long seed, int x, int y, int z) {
        long hash = seed;
        hash ^= x * 0x632BE59BD9B4E019L;
        hash ^= y * 0x9E3779B97F4A7C15L;
        hash ^= z * 0xC2B2AE3D27D4EB4FL;
        hash = mix64(hash);
        return ((hash >>> 11) * (1.0D / (1L << 53))) * 2.0D - 1.0D;
    }

    private static long hashString(String value) {
        long hash = 1125899906842597L;

        for (int i = 0; i < value.length(); i++) {
            hash = 31L * hash + value.charAt(i);
        }

        return mix64(hash);
    }

    private static long mix64(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }
}
