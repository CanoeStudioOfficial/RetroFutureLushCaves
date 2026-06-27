package com.canoestudio.retrofuturemc.contents.world.gen.noise;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class RetroFutureNormalNoise {
    private static final double INPUT_FACTOR = 1.0181268882175227D;
    private final double valueFactor;
    private final PerlinNoise first;
    private final PerlinNoise second;
    private final double maxValue;

    public static RetroFutureNormalNoise create(long worldSeed, String key, int firstOctave, double firstAmplitude, double... amplitudes) {
        double[] allAmplitudes = new double[amplitudes.length + 1];
        allAmplitudes[0] = firstAmplitude;
        System.arraycopy(amplitudes, 0, allAmplitudes, 1, amplitudes.length);
        return new RetroFutureNormalNoise(new XoroshiroRandomSource(RandomSupport.upgradeSeedTo128bit(worldSeed).xor(RandomSupport.seedFromHashOf(key))), firstOctave, allAmplitudes);
    }

    private RetroFutureNormalNoise(XoroshiroRandomSource random, int firstOctave, double[] amplitudes) {
        this.first = new PerlinNoise(random, firstOctave, amplitudes);
        this.second = new PerlinNoise(random, firstOctave, amplitudes);

        int minOctave = Integer.MAX_VALUE;
        int maxOctave = Integer.MIN_VALUE;

        for (int i = 0; i < amplitudes.length; i++) {
            if (amplitudes[i] != 0.0D) {
                minOctave = Math.min(minOctave, i);
                maxOctave = Math.max(maxOctave, i);
            }
        }

        this.valueFactor = 0.16666666666666666D / expectedDeviation(maxOctave - minOctave);
        this.maxValue = (this.first.maxValue() + this.second.maxValue()) * this.valueFactor;
    }

    public double getValue(double x, double y, double z) {
        double x2 = x * INPUT_FACTOR;
        double y2 = y * INPUT_FACTOR;
        double z2 = z * INPUT_FACTOR;
        return (this.first.getValue(x, y, z) + this.second.getValue(x2, y2, z2)) * this.valueFactor;
    }

    public double maxValue() {
        return this.maxValue;
    }

    private static double expectedDeviation(int octaveSpan) {
        return 0.1D * (1.0D + 1.0D / (double)(octaveSpan + 1));
    }

    private static final class PerlinNoise {
        private final ImprovedNoise[] noiseLevels;
        private final int firstOctave;
        private final double[] amplitudes;
        private final double lowestFreqValueFactor;
        private final double lowestFreqInputFactor;
        private final double maxValue;

        private PerlinNoise(XoroshiroRandomSource random, int firstOctave, double[] amplitudes) {
            this.firstOctave = firstOctave;
            this.amplitudes = amplitudes.clone();
            this.noiseLevels = new ImprovedNoise[amplitudes.length];
            PositionalRandomFactory positional = random.forkPositional();

            for (int i = 0; i < amplitudes.length; i++) {
                if (amplitudes[i] != 0.0D) {
                    int octave = firstOctave + i;
                    this.noiseLevels[i] = new ImprovedNoise(positional.fromHashOf("octave_" + octave));
                }
            }

            int zeroOctaveIndex = -firstOctave;
            this.lowestFreqInputFactor = Math.pow(2.0D, (double)(-zeroOctaveIndex));
            this.lowestFreqValueFactor = Math.pow(2.0D, (double)(amplitudes.length - 1)) / (Math.pow(2.0D, (double)amplitudes.length) - 1.0D);
            this.maxValue = this.edgeValue(2.0D);
        }

        private double getValue(double x, double y, double z) {
            double value = 0.0D;
            double factor = this.lowestFreqInputFactor;
            double valueFactor = this.lowestFreqValueFactor;

            for (int i = 0; i < this.noiseLevels.length; i++) {
                ImprovedNoise noise = this.noiseLevels[i];

                if (noise != null) {
                    value += this.amplitudes[i] * noise.noise(wrap(x * factor), wrap(y * factor), wrap(z * factor)) * valueFactor;
                }

                factor *= 2.0D;
                valueFactor /= 2.0D;
            }

            return value;
        }

        private double maxValue() {
            return this.maxValue;
        }

        private double edgeValue(double noiseValue) {
            double value = 0.0D;
            double valueFactor = this.lowestFreqValueFactor;

            for (int i = 0; i < this.noiseLevels.length; i++) {
                if (this.noiseLevels[i] != null) {
                    value += this.amplitudes[i] * noiseValue * valueFactor;
                }

                valueFactor /= 2.0D;
            }

            return value;
        }

        private static double wrap(double x) {
            return x - (double)floor(x / 3.3554432E7D + 0.5D) * 3.3554432E7D;
        }
    }

    private static final class ImprovedNoise {
        private static final int[][] GRADIENT = new int[][] {
                {1, 1, 0}, {-1, 1, 0}, {1, -1, 0}, {-1, -1, 0},
                {1, 0, 1}, {-1, 0, 1}, {1, 0, -1}, {-1, 0, -1},
                {0, 1, 1}, {0, -1, 1}, {0, 1, -1}, {0, -1, -1},
                {1, 1, 0}, {0, -1, 1}, {-1, 1, 0}, {0, -1, -1}
        };
        private final byte[] p = new byte[256];
        private final double xo;
        private final double yo;
        private final double zo;

        private ImprovedNoise(XoroshiroRandomSource random) {
            this.xo = random.nextDouble() * 256.0D;
            this.yo = random.nextDouble() * 256.0D;
            this.zo = random.nextDouble() * 256.0D;

            for (int i = 0; i < 256; i++) {
                this.p[i] = (byte)i;
            }

            for (int i = 0; i < 256; i++) {
                int offset = random.nextInt(256 - i);
                byte tmp = this.p[i];
                this.p[i] = this.p[i + offset];
                this.p[i + offset] = tmp;
            }
        }

        private double noise(double x, double y, double z) {
            double shiftedX = x + this.xo;
            double shiftedY = y + this.yo;
            double shiftedZ = z + this.zo;
            int xf = floor(shiftedX);
            int yf = floor(shiftedY);
            int zf = floor(shiftedZ);
            double xr = shiftedX - (double)xf;
            double yr = shiftedY - (double)yf;
            double zr = shiftedZ - (double)zf;
            return this.sampleAndLerp(xf, yf, zf, xr, yr, zr);
        }

        private int p(int x) {
            return this.p[x & 255] & 255;
        }

        private double sampleAndLerp(int x, int y, int z, double xr, double yr, double zr) {
            int x0 = this.p(x);
            int x1 = this.p(x + 1);
            int xy00 = this.p(x0 + y);
            int xy01 = this.p(x0 + y + 1);
            int xy10 = this.p(x1 + y);
            int xy11 = this.p(x1 + y + 1);
            double d000 = gradDot(this.p(xy00 + z), xr, yr, zr);
            double d100 = gradDot(this.p(xy10 + z), xr - 1.0D, yr, zr);
            double d010 = gradDot(this.p(xy01 + z), xr, yr - 1.0D, zr);
            double d110 = gradDot(this.p(xy11 + z), xr - 1.0D, yr - 1.0D, zr);
            double d001 = gradDot(this.p(xy00 + z + 1), xr, yr, zr - 1.0D);
            double d101 = gradDot(this.p(xy10 + z + 1), xr - 1.0D, yr, zr - 1.0D);
            double d011 = gradDot(this.p(xy01 + z + 1), xr, yr - 1.0D, zr - 1.0D);
            double d111 = gradDot(this.p(xy11 + z + 1), xr - 1.0D, yr - 1.0D, zr - 1.0D);
            double xAlpha = smoothstep(xr);
            double yAlpha = smoothstep(yr);
            double zAlpha = smoothstep(zr);
            return lerp3(xAlpha, yAlpha, zAlpha, d000, d100, d010, d110, d001, d101, d011, d111);
        }

        private static double gradDot(int hash, double x, double y, double z) {
            int[] gradient = GRADIENT[hash & 15];
            return (double)gradient[0] * x + (double)gradient[1] * y + (double)gradient[2] * z;
        }
    }

    private interface PositionalRandomFactory {
        XoroshiroRandomSource fromHashOf(String name);
    }

    private static final class XoroshiroRandomSource {
        private static final double DOUBLE_UNIT = 1.1102230246251565E-16D;
        private Xoroshiro128PlusPlus randomNumberGenerator;

        private XoroshiroRandomSource(RandomSupport.Seed128bit seed) {
            this.randomNumberGenerator = new Xoroshiro128PlusPlus(seed.seedLo, seed.seedHi);
        }

        private XoroshiroRandomSource(long seedLo, long seedHi) {
            this.randomNumberGenerator = new Xoroshiro128PlusPlus(seedLo, seedHi);
        }

        private PositionalRandomFactory forkPositional() {
            long seedLo = this.randomNumberGenerator.nextLong();
            long seedHi = this.randomNumberGenerator.nextLong();
            return new XoroshiroPositionalRandomFactory(seedLo, seedHi);
        }

        private int nextInt() {
            return (int)this.randomNumberGenerator.nextLong();
        }

        private int nextInt(int bound) {
            if (bound <= 0) {
                throw new IllegalArgumentException("Bound must be positive");
            }

            long randomBits = Integer.toUnsignedLong(this.nextInt());
            long multipliedRandomBits = randomBits * (long)bound;
            long fractionalPart = multipliedRandomBits & 4294967295L;

            if (fractionalPart < (long)bound) {
                for (int unbiasedBucketsStartIndex = Integer.remainderUnsigned(~bound + 1, bound); fractionalPart < (long)unbiasedBucketsStartIndex; fractionalPart = multipliedRandomBits & 4294967295L) {
                    randomBits = Integer.toUnsignedLong(this.nextInt());
                    multipliedRandomBits = randomBits * (long)bound;
                }
            }

            return (int)(multipliedRandomBits >>> 32);
        }

        private double nextDouble() {
            return (double)(this.randomNumberGenerator.nextLong() >>> 11) * DOUBLE_UNIT;
        }
    }

    private static final class XoroshiroPositionalRandomFactory implements PositionalRandomFactory {
        private final long seedLo;
        private final long seedHi;

        private XoroshiroPositionalRandomFactory(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;
        }

        @Override
        public XoroshiroRandomSource fromHashOf(String name) {
            return new XoroshiroRandomSource(RandomSupport.seedFromHashOf(name).xor(this.seedLo, this.seedHi));
        }
    }

    private static final class Xoroshiro128PlusPlus {
        private long seedLo;
        private long seedHi;

        private Xoroshiro128PlusPlus(long seedLo, long seedHi) {
            this.seedLo = seedLo;
            this.seedHi = seedHi;

            if ((this.seedLo | this.seedHi) == 0L) {
                this.seedLo = -7046029254386353131L;
                this.seedHi = 7640891576956012809L;
            }
        }

        private long nextLong() {
            long s0 = this.seedLo;
            long s1 = this.seedHi;
            long result = Long.rotateLeft(s0 + s1, 17) + s0;

            s1 ^= s0;
            this.seedLo = Long.rotateLeft(s0, 49) ^ s1 ^ s1 << 21;
            this.seedHi = Long.rotateLeft(s1, 28);
            return result;
        }
    }

    private static final class RandomSupport {
        private static final long GOLDEN_RATIO_64 = -7046029254386353131L;
        private static final long SILVER_RATIO_64 = 7640891576956012809L;

        private static Seed128bit upgradeSeedTo128bit(long seed) {
            long lowBits = seed ^ SILVER_RATIO_64;
            long highBits = lowBits + GOLDEN_RATIO_64;
            return new Seed128bit(mixStafford13(lowBits), mixStafford13(highBits));
        }

        private static Seed128bit seedFromHashOf(String input) {
            try {
                MessageDigest digest = MessageDigest.getInstance("MD5");
                byte[] hash = digest.digest(input.getBytes("UTF-8"));
                return new Seed128bit(bytesToLong(hash, 0), bytesToLong(hash, 8));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("MD5 digest is required for Mojang-style noise seeds", e);
            } catch (java.io.UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 encoding is required for Mojang-style noise seeds", e);
            }
        }

        private static long bytesToLong(byte[] bytes, int start) {
            return ((long)bytes[start] & 255L) << 56
                    | ((long)bytes[start + 1] & 255L) << 48
                    | ((long)bytes[start + 2] & 255L) << 40
                    | ((long)bytes[start + 3] & 255L) << 32
                    | ((long)bytes[start + 4] & 255L) << 24
                    | ((long)bytes[start + 5] & 255L) << 16
                    | ((long)bytes[start + 6] & 255L) << 8
                    | ((long)bytes[start + 7] & 255L);
        }

        private static long mixStafford13(long value) {
            value = (value ^ value >>> 30) * -4658895280553007687L;
            value = (value ^ value >>> 27) * -7723592293110705685L;
            return value ^ value >>> 31;
        }

        private static final class Seed128bit {
            private final long seedLo;
            private final long seedHi;

            private Seed128bit(long seedLo, long seedHi) {
                this.seedLo = seedLo;
                this.seedHi = seedHi;
            }

            private Seed128bit xor(long lo, long hi) {
                return new Seed128bit(this.seedLo ^ lo, this.seedHi ^ hi);
            }

            private Seed128bit xor(Seed128bit other) {
                return this.xor(other.seedLo, other.seedHi);
            }
        }
    }

    private static int floor(double value) {
        return (int)Math.floor(value);
    }

    private static double smoothstep(double value) {
        return value * value * value * (value * (value * 6.0D - 15.0D) + 10.0D);
    }

    private static double lerp(double alpha, double start, double end) {
        return start + alpha * (end - start);
    }

    private static double lerp2(double alpha1, double alpha2, double x00, double x10, double x01, double x11) {
        return lerp(alpha2, lerp(alpha1, x00, x10), lerp(alpha1, x01, x11));
    }

    private static double lerp3(double alpha1, double alpha2, double alpha3, double x000, double x100, double x010, double x110, double x001, double x101, double x011, double x111) {
        return lerp(alpha3, lerp2(alpha1, alpha2, x000, x100, x010, x110), lerp2(alpha1, alpha2, x001, x101, x011, x111));
    }
}
