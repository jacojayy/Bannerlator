#version 450

// =============================================================================
//  Color grade (brightness / contrast / gamma) post effect.
//
//  Ported verbatim from the OpenGL ColorEffect.java shader. The three terms are
//  prepared CPU-side (planUpscaleFrame) exactly as the GL path does:
//      brightness = brightnessSlider / 100   clamped to [-1, 1]
//      contrast   = contrastSlider   / 100   clamped to [ 0, 2]
//      gamma      = gammaSlider               clamped to [0.1, 5]
//  (the clamps replicate ColorEffectMaterial.use()). Neutral = (0,0,1) -> no-op,
//  matching the GL "remove the effect" check; the chain skips Color when neutral.
//
//  Conventions match upscale.vert: combined-image-sampler at binding 0, the
//  push-constant block leads with vec4 ndc (offset 0), fragTexCoord in [0,1].
//  Runs after Toon, before CAS in the canonical chain (grade the clean image).
// =============================================================================

layout(binding = 0) uniform sampler2D InputTexture;

layout(push_constant) uniform PC {
    vec4  ndc;        // quad NDC rect, consumed by upscale.vert (offset 0)
    float brightness; // additive, [-1, 1]
    float contrast;   // [0, 2]; effective multiplier = clamp(contrast+1, 0.5, 2)
    float gamma;      // [0.1, 5]
} pc;

layout(location = 0) in  vec2 fragTexCoord;
layout(location = 0) out vec4 outColor;

void main() {
    vec4 texelColor = texture(InputTexture, fragTexCoord);
    vec3 color = texelColor.rgb;
    color = clamp(color + pc.brightness, 0.0, 1.0);
    color = (color - 0.5) * clamp(pc.contrast + 1.0, 0.5, 2.0) + 0.5;
    color = pow(color, vec3(1.0 / pc.gamma));
    outColor = vec4(color, texelColor.a);
}
