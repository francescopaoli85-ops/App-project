package com.francescopaoli.northstar.ui.fx

import android.app.Application
import android.graphics.RuntimeShader
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Gli shader AGSL devono compilare: un errore qui, sul telefono, farebbe sparire l'effetto. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], application = Application::class)
class ShaderCompileTest {
    @Test fun nebulaCompiles() { RuntimeShader(Shaders.NEBULA) }
    /**
     * Sul telefono (Android 13+) si legge il contenuto con `content.eval(p)`.
     * Il motore dentro Robolectric è più vecchio e conosce solo `sample(content, p)`:
     * se fallisce proprio su quello, ricontrollo il resto dello shader con la sintassi vecchia.
     */
    @Test fun shockwaveCompiles() {
        try {
            RuntimeShader(Shaders.SHOCKWAVE)
        } catch (e: IllegalArgumentException) {
            if (e.message?.contains("cannot swizzle value of type 'shader'") != true) throw e
            val legacy = Shaders.SHOCKWAVE.replace(Regex("""content\.eval\(([^;]*)\);"""), "sample(content, $1);")
            RuntimeShader(legacy)
        }
    }
}

