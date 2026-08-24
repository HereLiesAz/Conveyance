package com.hereliesaz.conveyance.demo

import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.unit.Density
import java.io.File
import javax.imageio.ImageIO
import javax.imageio.IIOImage
import javax.imageio.ImageWriteParam
import javax.imageio.metadata.IIOMetadataNode
import javax.imageio.stream.FileImageOutputStream
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Renders the framework's verbs as motion.
 *
 * Stills were the wrong medium and produced the wrong impression: a grammar's whole argument is what
 * happens between two states, so a photograph of the endpoints shows the one thing conveyance is not
 * about. These are driven by real pointer events against the real application, and every frame comes
 * from the framework's own animation rather than from anything staged here.
 */
class RenderMotion {

    private val out = File("build/motion").apply { mkdirs() }
    private val width = 900
    private val height = 1300
    private val density = 2f

    private fun dp(value: Float) = value * density

    /**
     * Drive the app and capture every frame.
     *
     * [script] is given the frame index and may send pointer events; the scene advances by one
     * display frame each step, so what is recorded is exactly what a person would have seen.
     */
    private fun film(
        name: String,
        frames: Int = 40,
        initial: Int = 4,
        script: (Int, ImageComposeScene) -> Unit,
    ) {
        val scene = ImageComposeScene(width = width, height = height, density = Density(density)) {
            Gallery(initial = initial)
        }
        val captured = mutableListOf<BufferedImage>()
        try {
            var nanos = 0L
            repeat(frames) { frame ->
                script(frame, scene)
                nanos += 33_000_000L
                val skia = scene.render(nanos)
                captured += ImageIO.read(skia.encodeToData()!!.bytes.inputStream())
            }
        } finally {
            scene.close()
        }
        writeGif(File(out, "$name.gif"), captured, delayMs = 33)
        assertTrue(File(out, "$name.gif").length() > 0, "$name produced no film")
    }

    private fun ImageComposeScene.tap(x: Float, y: Float) {
        sendPointerEvent(PointerEventType.Press, Offset(x, y))
        sendPointerEvent(PointerEventType.Release, Offset(x, y))
    }

    /** The Migration: an empty tray is its own invitation, and the shutter moves in once used. */
    @Test
    fun `the migration`() = film("01-migration", frames = 55, initial = 0) { frame, scene ->
        // An empty tray is the only state in which the Migration exists. The shutter sits in the
        // middle of the space it is inviting you to fill; taking one photograph sends it home.
        if (frame == 8) scene.tap(width / 2f, dp(400f))
        if (frame == 26) scene.tap(width - dp(70f), height - dp(70f))
    }

    /**
     * The Escort: touching a photograph with nobody chosen carries you to the people, and the person
     * you land on settles under your attention. No message, no greyed-out control, no explanation.
     */
    @Test
    fun `the escort`() = film("02-escort", frames = 45) { frame, scene ->
        if (frame == 6) scene.tap(width / 2f, dp(240f))
    }

    /**
     * Send: the photograph leaves your hands and arrives somewhere you can point at, and the person
     * it went to warms. This is the entire argument for a visible destination.
     */
    @Test
    fun `sending`() = film("03-send", frames = 55) { frame, scene ->
        if (frame == 4) scene.tap(dp(55f), dp(58f))
        if (frame == 12) scene.tap(width / 2f, dp(240f))
    }

    private fun writeGif(target: File, frames: List<BufferedImage>, delayMs: Int) {
        require(frames.isNotEmpty()) { "nothing to write" }
        val writer = ImageIO.getImageWritersByFormatName("gif").next()
        FileImageOutputStream(target).use { output ->
            writer.output = output
            val params = writer.defaultWriteParam
            val type = javax.imageio.ImageTypeSpecifier.createFromBufferedImageType(BufferedImage.TYPE_INT_RGB)
            val metadata = writer.getDefaultImageMetadata(type, params)
            val format = metadata.nativeMetadataFormatName

            val root = metadata.getAsTree(format) as IIOMetadataNode
            val control = IIOMetadataNode("GraphicControlExtension").apply {
                setAttribute("disposalMethod", "none")
                setAttribute("userInputFlag", "FALSE")
                setAttribute("transparentColorFlag", "FALSE")
                setAttribute("delayTime", (delayMs / 10).toString())
                setAttribute("transparentColorIndex", "0")
            }
            root.appendChild(control)
            val extensions = IIOMetadataNode("ApplicationExtensions")
            extensions.appendChild(
                IIOMetadataNode("ApplicationExtension").apply {
                    setAttribute("applicationID", "NETSCAPE")
                    setAttribute("authenticationCode", "2.0")
                    // Loop forever: a grammar is learned by seeing the same motion more than once.
                    userObject = byteArrayOf(0x1, 0x0, 0x0)
                },
            )
            root.appendChild(extensions)
            metadata.setFromTree(format, root)

            writer.prepareWriteSequence(null)
            frames.forEach { frame ->
                val rgb = BufferedImage(frame.width, frame.height, BufferedImage.TYPE_INT_RGB)
                rgb.createGraphics().apply { drawImage(frame, 0, 0, null); dispose() }
                writer.writeToSequence(IIOImage(rgb, null, metadata), params)
            }
            writer.endWriteSequence()
        }
        writer.dispose()
    }
}
