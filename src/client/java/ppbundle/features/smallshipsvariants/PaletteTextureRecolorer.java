package ppbundle.features.smallshipsvariants;

import com.mojang.blaze3d.platform.NativeImage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * This utility performs palette-based recoloring of oak-based item textures.
 *
 * The algorithm extracts a luminance-ordered palette from the oak planks texture
 * and from the target planks texture, then remaps only pixels that are close to
 * the oak wood palette. Pixels such as sails, metal details, outlines and highlights
 * are preserved when they are not part of the oak wood palette range.
 */
public final class PaletteTextureRecolorer {
    private static final int DEFAULT_PALETTE_SIZE = 6;
    private static final int PALETTE_MERGE_DISTANCE_SQ = 16 * 16;
    private static final int WOOD_MATCH_DISTANCE_SQ = 72 * 72;
    private static final int OVERLAY_DIFFERENCE_DISTANCE_SQ = 18 * 18;

    private PaletteTextureRecolorer() {
    }

    public static NativeImage recolorFromOakPlanks(NativeImage oakItemTexture, NativeImage oakPlanks, NativeImage targetPlanks) {
        List<RgbColor> oakPalette = extractPalette(oakPlanks, DEFAULT_PALETTE_SIZE);
        List<RgbColor> targetPalette = extractPalette(targetPlanks, DEFAULT_PALETTE_SIZE);

        if (oakPalette.isEmpty() || targetPalette.isEmpty()) {
            return copy(oakItemTexture);
        }

        NativeImage output = new NativeImage(NativeImage.Format.RGBA, oakItemTexture.getWidth(), oakItemTexture.getHeight(), false);

        for (int y = 0; y < oakItemTexture.getHeight(); y++) {
            for (int x = 0; x < oakItemTexture.getWidth(); x++) {
                int pixel = oakItemTexture.getPixelRGBA(x, y);
                output.setPixelRGBA(x, y, recolorPixel(pixel, oakPalette, targetPalette));
            }
        }

        return output;
    }

    /**
     * This recolors the wooden hull but preserves pixels that belong to the extra
     * chest overlay by comparing the chest texture against the plain boat texture.
     */
    public static NativeImage recolorFromOakPlanksPreservingOverlay(NativeImage oakCompositeTexture,
                                                                    NativeImage oakReferenceTexture,
                                                                    NativeImage oakPlanks,
                                                                    NativeImage targetPlanks) {
        List<RgbColor> oakPalette = extractPalette(oakPlanks, DEFAULT_PALETTE_SIZE);
        List<RgbColor> targetPalette = extractPalette(targetPlanks, DEFAULT_PALETTE_SIZE);

        if (oakPalette.isEmpty() || targetPalette.isEmpty()) {
            return copy(oakCompositeTexture);
        }

        NativeImage output = new NativeImage(
                NativeImage.Format.RGBA,
                oakCompositeTexture.getWidth(),
                oakCompositeTexture.getHeight(),
                false
        );

        for (int y = 0; y < oakCompositeTexture.getHeight(); y++) {
            for (int x = 0; x < oakCompositeTexture.getWidth(); x++) {
                int compositePixel = oakCompositeTexture.getPixelRGBA(x, y);

                if (shouldPreserveOverlayPixel(oakCompositeTexture, oakReferenceTexture, x, y)) {
                    output.setPixelRGBA(x, y, compositePixel);
                    continue;
                }

                output.setPixelRGBA(x, y, recolorPixel(compositePixel, oakPalette, targetPalette));
            }
        }

        return output;
    }

    private static int recolorPixel(int pixel, List<RgbColor> oakPalette, List<RgbColor> targetPalette) {
        int a = alpha(pixel);

        if (a <= 0) {
            return pixel;
        }

        RgbColor source = RgbColor.fromNative(pixel);
        int nearestIndex = nearestPaletteIndex(source, oakPalette);
        RgbColor nearestOak = oakPalette.get(nearestIndex);

        if (source.distanceSq(nearestOak) > WOOD_MATCH_DISTANCE_SQ) {
            return pixel;
        }

        float woodShade = palettePosition(source, oakPalette, nearestIndex);
        RgbColor mapped = samplePalette(targetPalette, woodShade);

        RgbColor preservedLightness = mapped.withLightnessFrom(source);
        return preservedLightness.toNative(a);
    }

    /**
     * This preserves pixels that belong to the chest-specific overlay.
     *
     * The chest overlay is detected when:
     * - the composite pixel is visible but the plain boat pixel is transparent
     * - or both are visible but their colors differ enough to represent the chest
     */
    private static boolean shouldPreserveOverlayPixel(NativeImage composite, NativeImage reference, int x, int y) {
        if (x >= reference.getWidth() || y >= reference.getHeight()) {
            return true;
        }

        int compositePixel = composite.getPixelRGBA(x, y);
        int referencePixel = reference.getPixelRGBA(x, y);

        int compositeAlpha = alpha(compositePixel);
        int referenceAlpha = alpha(referencePixel);

        if (compositeAlpha <= 0) {
            return false;
        }

        if (referenceAlpha <= 0) {
            return true;
        }

        RgbColor compositeColor = RgbColor.fromNative(compositePixel);
        RgbColor referenceColor = RgbColor.fromNative(referencePixel);

        return compositeColor.distanceSq(referenceColor) > OVERLAY_DIFFERENCE_DISTANCE_SQ;
    }

    private static NativeImage copy(NativeImage source) {
        NativeImage copy = new NativeImage(NativeImage.Format.RGBA, source.getWidth(), source.getHeight(), false);
        for (int y = 0; y < source.getHeight(); y++) {
            for (int x = 0; x < source.getWidth(); x++) {
                copy.setPixelRGBA(x, y, source.getPixelRGBA(x, y));
            }
        }
        return copy;
    }

    private static List<RgbColor> extractPalette(NativeImage image, int targetSize) {
        List<PaletteBucket> buckets = new ArrayList<>();

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                int pixel = image.getPixelRGBA(x, y);
                int a = alpha(pixel);
                if (a < 12) {
                    continue;
                }

                RgbColor color = RgbColor.fromNative(pixel);

                PaletteBucket existing = null;
                for (PaletteBucket bucket : buckets) {
                    if (bucket.color.distanceSq(color) <= PALETTE_MERGE_DISTANCE_SQ) {
                        existing = bucket;
                        break;
                    }
                }

                if (existing == null) {
                    buckets.add(new PaletteBucket(color));
                } else {
                    existing.add(color);
                }
            }
        }

        buckets.sort(Comparator
                .comparingInt(PaletteBucket::weight).reversed()
                .thenComparingDouble(bucket -> bucket.color.luminance()));

        if (buckets.isEmpty()) {
            return List.of();
        }

        List<RgbColor> colors = new ArrayList<>();
        for (PaletteBucket bucket : buckets) {
            colors.add(bucket.average());
        }

        colors.sort(Comparator.comparingDouble(RgbColor::luminance));

        if (colors.size() <= targetSize) {
            return colors;
        }

        List<RgbColor> resampled = new ArrayList<>();
        for (int i = 0; i < targetSize; i++) {
            float t = targetSize == 1 ? 0.0f : (float) i / (float) (targetSize - 1);
            resampled.add(samplePalette(colors, t));
        }
        return resampled;
    }

    private static int nearestPaletteIndex(RgbColor color, List<RgbColor> palette) {
        int bestIndex = 0;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = 0; i < palette.size(); i++) {
            int distance = color.distanceSq(palette.get(i));
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }

        return bestIndex;
    }

    private static float palettePosition(RgbColor color, List<RgbColor> palette, int nearestIndex) {
        if (palette.size() == 1) {
            return 0.0f;
        }

        RgbColor nearest = palette.get(nearestIndex);
        float nearestL = (float) nearest.luminance();

        float lowL = nearestIndex > 0 ? (float) palette.get(nearestIndex - 1).luminance() : nearestL;
        float highL = nearestIndex < palette.size() - 1 ? (float) palette.get(nearestIndex + 1).luminance() : nearestL;
        float currentL = (float) color.luminance();

        float localT;
        if (currentL < nearestL && nearestIndex > 0 && nearestL > lowL) {
            localT = (currentL - lowL) / (nearestL - lowL);
            localT = clamp(localT, 0.0f, 1.0f);
            return ((nearestIndex - 1) + localT) / (palette.size() - 1.0f);
        }

        if (currentL > nearestL && nearestIndex < palette.size() - 1 && highL > nearestL) {
            localT = (currentL - nearestL) / (highL - nearestL);
            localT = clamp(localT, 0.0f, 1.0f);
            return (nearestIndex + localT) / (palette.size() - 1.0f);
        }

        return nearestIndex / (palette.size() - 1.0f);
    }

    private static RgbColor samplePalette(List<RgbColor> palette, float t) {
        if (palette.size() == 1) {
            return palette.get(0);
        }

        float scaled = clamp(t, 0.0f, 1.0f) * (palette.size() - 1);
        int low = (int) Math.floor(scaled);
        int high = Math.min(palette.size() - 1, low + 1);
        float localT = scaled - low;

        return RgbColor.lerp(palette.get(low), palette.get(high), localT);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int alpha(int nativePixel) {
        return (nativePixel >>> 24) & 0xFF;
    }

    private static final class PaletteBucket {
        private int totalR;
        private int totalG;
        private int totalB;
        private int count;
        private RgbColor color;

        private PaletteBucket(RgbColor initial) {
            this.color = initial;
            this.totalR = initial.r;
            this.totalG = initial.g;
            this.totalB = initial.b;
            this.count = 1;
        }

        private void add(RgbColor value) {
            this.totalR += value.r;
            this.totalG += value.g;
            this.totalB += value.b;
            this.count++;
            this.color = average();
        }

        private int weight() {
            return count;
        }

        private RgbColor average() {
            return new RgbColor(
                    totalR / count,
                    totalG / count,
                    totalB / count
            );
        }
    }

    private static final class RgbColor {
        private final int r;
        private final int g;
        private final int b;

        private RgbColor(int r, int g, int b) {
            this.r = clampChannel(r);
            this.g = clampChannel(g);
            this.b = clampChannel(b);
        }

        private static RgbColor fromNative(int nativePixel) {
            int r = nativePixel & 0xFF;
            int g = (nativePixel >>> 8) & 0xFF;
            int b = (nativePixel >>> 16) & 0xFF;
            return new RgbColor(r, g, b);
        }

        private int toNative(int alpha) {
            return ((alpha & 0xFF) << 24)
                    | ((b & 0xFF) << 16)
                    | ((g & 0xFF) << 8)
                    | (r & 0xFF);
        }

        private int distanceSq(RgbColor other) {
            int dr = this.r - other.r;
            int dg = this.g - other.g;
            int db = this.b - other.b;
            return dr * dr + dg * dg + db * db;
        }

        private double luminance() {
            return 0.2126D * r + 0.7152D * g + 0.0722D * b;
        }

        private RgbColor withLightnessFrom(RgbColor source) {
            float sourceBrightness = (thisChannelMax(source) <= 0) ? 0.0f : (float) source.luminance() / thisChannelMax(source);
            int nr = clampChannel(Math.round(this.r * sourceBrightness));
            int ng = clampChannel(Math.round(this.g * sourceBrightness));
            int nb = clampChannel(Math.round(this.b * sourceBrightness));
            return new RgbColor(nr, ng, nb);
        }

        private static int thisChannelMax(RgbColor color) {
            return Math.max(1, Math.max(color.r, Math.max(color.g, color.b)));
        }

        private static RgbColor lerp(RgbColor a, RgbColor b, float t) {
            int nr = Math.round(a.r + (b.r - a.r) * t);
            int ng = Math.round(a.g + (b.g - a.g) * t);
            int nb = Math.round(a.b + (b.b - a.b) * t);
            return new RgbColor(nr, ng, nb);
        }

        private static int clampChannel(int value) {
            return Math.max(0, Math.min(255, value));
        }
    }
}