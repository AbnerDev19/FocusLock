package com.focuslock.services

import android.content.Context
import android.graphics.Bitmap
import com.focuslock.domain.ContentLevel
import com.focuslock.domain.ImageClassifier
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter

/**
 * Detector aproximado, sem modelo: estima a proporção de pixels com cor de pele na tela.
 * Erra para os dois lados (retratos grandes podem disparar, imagens explícitas escuras podem passar).
 */
class SkinHeuristicClassifier : ImageClassifier {
    override val available = true
    override val description = "Detector de pele (aproximado)"

    override fun classify(bitmap: Bitmap): ContentLevel {
        val w = 90
        val h = 160
        val small = Bitmap.createScaledBitmap(bitmap, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== bitmap) small.recycle()
        var skin = 0
        for (p in px) {
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val mx = maxOf(r, g, b)
            val mn = minOf(r, g, b)
            if (r > 95 && g > 40 && b > 20 && mx - mn > 15 && Math.abs(r - g) > 15 && r > g && r > b) skin++
        }
        val ratio = skin.toFloat() / px.size
        return when {
            ratio >= 0.55f -> ContentLevel.EXPLICIT
            ratio >= 0.42f -> ContentLevel.SEXUAL
            ratio >= 0.32f -> ContentLevel.SUGGESTIVE
            else -> ContentLevel.SAFE
        }
    }
}

/**
 * Usa app/src/main/assets/nsfw.tflite quando existir. Espera o formato do modelo
 * GantMan/nsfw_model: entrada float32 RGB em [0,1] e 5 saídas (drawings, hentai, neutral, porn, sexy).
 */
class TfliteClassifier private constructor(
    private val interpreter: Interpreter,
    private val width: Int,
    private val height: Int
) : ImageClassifier {
    override val available = true
    override val description = "Modelo TensorFlow Lite (nsfw.tflite)"

    override fun classify(bitmap: Bitmap): ContentLevel {
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        val px = IntArray(width * height)
        scaled.getPixels(px, 0, width, 0, 0, width, height)
        if (scaled !== bitmap) scaled.recycle()
        val buf = ByteBuffer.allocateDirect(4 * width * height * 3).order(ByteOrder.nativeOrder())
        for (p in px) {
            buf.putFloat(((p shr 16) and 0xFF) / 255f)
            buf.putFloat(((p shr 8) and 0xFF) / 255f)
            buf.putFloat((p and 0xFF) / 255f)
        }
        buf.rewind()
        val out = Array(1) { FloatArray(5) }
        interpreter.run(buf, out)
        val o = out[0]
        val explicit = o[1] + o[3]
        val sexy = o[4]
        return when {
            explicit >= 0.6f -> ContentLevel.EXPLICIT
            explicit >= 0.3f || sexy >= 0.7f -> ContentLevel.SEXUAL
            sexy >= 0.4f -> ContentLevel.SUGGESTIVE
            else -> ContentLevel.SAFE
        }
    }

    companion object {
        fun load(ctx: Context): TfliteClassifier? = try {
            val bytes = ctx.assets.open("nsfw.tflite").use { it.readBytes() }
            val model = ByteBuffer.allocateDirect(bytes.size).order(ByteOrder.nativeOrder())
            model.put(bytes)
            model.rewind()
            val interp = Interpreter(model)
            val inShape = interp.getInputTensor(0).shape()
            val ok = interp.getInputTensor(0).dataType() == DataType.FLOAT32 &&
                inShape.size == 4 && interp.getOutputTensor(0).shape().last() == 5
            if (ok) TfliteClassifier(interp, inShape[2], inShape[1]) else null
        } catch (e: Exception) {
            null
        }
    }
}

object ImageClassifierFactory {
    fun create(ctx: Context): ImageClassifier = TfliteClassifier.load(ctx) ?: SkinHeuristicClassifier()
}
