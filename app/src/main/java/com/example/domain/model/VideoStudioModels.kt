package com.example.domain.model

enum class VideoStudioMode(val title: String, val badge: String) {
    TEXT_TO_VIDEO("Text to Video", "T2V"),
    IMAGE_TO_VIDEO("Image to Video", "I2V"),
    EXTEND_VIDEO("Extend Video", "EXTEND")
}

enum class VideoRatio(
    val label: String,
    val subtitle: String,
    val width: Int,
    val height: Int
) {
    // Exact 16:9 and 9:16 dimensions divisible by 16 for 100% H.264 MediaCodec compatibility
    LANDSCAPE_16_9("16:9", "Landscape", 768, 432),
    PORTRAIT_9_16("9:16", "Portrait", 432, 768)
}

enum class FreeVideoModel(
    val displayName: String,
    val tag: String,
    val engineSummary: String,
    val styleDirective: String,
    val primaryImageModel: String,
    val fallbackImageModel: String
) {
    VEO3_FAST(
        displayName = "Google Veo 3 Fast",
        tag = "FREE · REALISM",
        engineSummary = "Photorealistic cinema via Built-in Gemini Director + Nano Banana 2.1",
        styleDirective = "photorealistic 35mm cinema film, crisp natural lighting, realistic physics and fluid motion, shallow depth of field, ultra sharp",
        primaryImageModel = "gemini-nano-banana-2.1",
        fallbackImageModel = "gemini-3.1-flash-lite-image"
    ),
    SORA_2(
        displayName = "Sora 2",
        tag = "FREE · CINEMA",
        engineSummary = "Sweeping world storytelling via Built-in Gemini Director + Nano Banana 2.1",
        styleDirective = "sweeping blockbuster cinematography, rich atmospheric depth, dynamic volumetric light, coherent world motion, IMAX 70mm film look",
        primaryImageModel = "gemini-nano-banana-2.1",
        fallbackImageModel = "gemini-3.1-flash-lite-image"
    ),
    GEMINI_4_NEON(
        displayName = "Gemini 4 Neon",
        tag = "FREE · TURBO",
        engineSummary = "High-speed luminous framing via Built-in Gemini Director + Flash-Lite Image",
        styleDirective = "modern vibrant neon-lit hyper-crisp cinematography, luminous highlights, energetic fluid framing, ultra clean composition",
        primaryImageModel = "gemini-3.1-flash-lite-image",
        fallbackImageModel = "gemini-nano-banana-2.1"
    ),
    QWEN38_MAX(
        displayName = "Qwen3.8-Max",
        tag = "FREE · VIVID",
        engineSummary = "High-contrast crisp detail via Built-in Gemini Director + Nano Banana 2.1",
        styleDirective = "ultra-detailed studio visual clarity, balanced dramatic lighting, sharp foreground texture, polished commercial film color grading",
        primaryImageModel = "gemini-nano-banana-2.1",
        fallbackImageModel = "gemini-3.1-flash-lite-image"
    ),
    MUSE_AI(
        displayName = "Muse AI",
        tag = "FREE · ARTISTIC",
        engineSummary = "Expressive artistic motion via Built-in Gemini Director + Nano Banana 2.1",
        styleDirective = "artistic dreamlike cinematic concept art, expressive painterly brushwork blended with atmospheric glow, rich color harmony",
        primaryImageModel = "gemini-nano-banana-2.1",
        fallbackImageModel = "gemini-3.1-flash-lite-image"
    )
}

enum class VideoCameraMotion(val displayName: String) {
    AUTO("Auto Director"),
    ZOOM_IN("Smooth Zoom In"),
    ZOOM_OUT("Slow Pull Back"),
    PAN_RIGHT("Cinema Pan Right"),
    PAN_LEFT("Cinema Pan Left"),
    ORBIT_DRIFT("Orbit Drift")
}

data class VideoClipRecord(
    val id: String,
    val prompt: String,
    val mode: VideoStudioMode,
    val model: FreeVideoModel,
    val ratio: VideoRatio,
    val segmentDurationSec: Int,
    val totalDurationSec: Int,
    val videoPath: String,
    val thumbPath: String,
    val lastFramePath: String,
    val parentClipId: String? = null,
    val extendStep: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

data class VideoRenderProgress(
    val stageTitle: String,
    val detail: String,
    val progress: Float
)
