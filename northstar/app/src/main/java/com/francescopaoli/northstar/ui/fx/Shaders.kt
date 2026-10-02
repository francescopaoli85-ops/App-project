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

    /**
     * Nebulosa/aurora che scorre lentamente: onde (seno/coseno) sovrapposte e deformate.
     * Niente funzioni "hash" con moltiplicazioni enormi: su molte GPU lo shader gira a
     * precisione ridotta e quelle si rompevano a blocchi. Le onde restano lisce comunque.
     */
    const val NEBULA = """
uniform float2 iResolution;
uniform float iTime;
uniform float2 iOffset;
uniform float3 cViolet;
uniform float3 cCyan;
uniform float3 cNight;

float fbm(float2 p, float t) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * (0.5 + 0.5 * sin(p.x * 3.1 + 1.7 * sin(p.y * 2.3 + t)) * cos(p.y * 2.7 - t * 0.7));
        // ruota e ingrandisce: ogni livello ha direzioni diverse, niente righe
        p = float2(0.8 * p.x - 0.6 * p.y, 0.6 * p.x + 0.8 * p.y) * 1.9 + float2(0.37, -0.21);
        a *= 0.5;
    }
    return v;
}

half4 main(float2 fragCoord) {
    float2 p = (fragCoord + iOffset) / iResolution.y;
    float t = iTime * 0.05;
    float qx = fbm(p + float2(0.0, t), t);
    float qy = fbm(p + float2(5.2, -t), t);
    float n = fbm(p + 1.6 * float2(qx, qy), t);
    float m = clamp((n - 0.25) / 0.5, 0.0, 1.0);

    float3 col = cNight;
    col = mix(col, cViolet, smoothstep(0.35, 1.0, m) * (0.35 + 0.3 * qx));
    col = mix(col, cCyan, smoothstep(0.6, 1.0, m * (0.5 + qy)) * 0.22);

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
