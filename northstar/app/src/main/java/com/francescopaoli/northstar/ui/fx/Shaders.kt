package com.francescopaoli.northstar.ui.fx

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi

/**
 * Shader AGSL (girano sulla scheda grafica, Android 13+).
 * Sotto Android 13, o se la compilazione fallisce, si usano gli effetti Canvas di riserva.
 */
object Shaders {

    val supported: Boolean get() = Build.VERSION.SDK_INT >= 33

    /** Nebulosa/aurora che scorre lentamente: rumore frattale deformato (domain warping). */
    const val NEBULA = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iOffset;
uniform float3 cViolet;
uniform float3 cCyan;
uniform float3 cNight;

float hash(float2 p) {
    p = fract(p * float2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    float2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(float2 p) {
    float v = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        v += amp * noise(p);
        p = p * 2.03 + float2(1.7, 9.2);
        amp *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 uv = (fragCoord + iOffset) / iResolution.y;
    float t = iTime * 0.045;
    float2 q = float2(fbm(uv * 1.6 + float2(0.0, t)), fbm(uv * 1.6 + float2(5.2, -t)));
    // il rumore esce tra ~0.2 e ~0.46: lo riporto su 0..1
    float n = clamp((fbm(uv * 1.3 + 2.4 * q + float2(t * 0.7, t * 0.3)) - 0.2) / 0.26, 0.0, 1.0);

    float3 col = cNight;
    float glowV = smoothstep(0.25, 0.95, n);
    col = mix(col, cViolet, glowV * (0.55 + 0.35 * q.x));
    float glowC = smoothstep(0.55, 1.0, n * (0.55 + q.y));
    col = mix(col, cCyan, glowC * 0.4);
    // nucleo luminoso dove le due correnti si incontrano
    float core = max(n - 0.38, 0.0) * 2.4;
    col += cViolet * core * core * 0.5;

    float2 c = fragCoord / iResolution - 0.5;
    col *= 1.0 - dot(c, c) * 0.9;
    return half4(half3(col), 1.0);
}
"""

    /** Onda d'urto: deforma il contenuto lungo un anello che si allarga. */
    const val SHOCKWAVE = """
uniform shader content;
uniform float2 center;
uniform float radius;
uniform float width;
uniform float strength;

half4 main(float2 p) {
    float2 d = p - center;
    float dist = length(d);
    float k = (dist - radius) / width;
    float w = exp(-k * k * 4.0);
    float2 dir = dist > 0.001 ? d / dist : float2(0.0, 0.0);
    half4 col = content.eval(p - dir * w * strength);
    float glow = w * 0.3 * clamp(strength / 40.0, 0.0, 1.0);
    col.rgb += half3(0.45, 0.30, 0.95) * half(glow);
    return col;
}
"""

    @RequiresApi(33)
    fun create(source: String): RuntimeShader? = runCatching { RuntimeShader(source) }.getOrNull()
}
