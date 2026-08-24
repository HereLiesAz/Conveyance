package com.hereliesaz.conveyance.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.hereliesaz.conveyance.Act
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Practice
import com.hereliesaz.conveyance.SubjectId
import java.lang.ref.WeakReference
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * A leak that no positional or count assertion can see: whether an object the collection no
 * longer needs is still being held somewhere. JVM-only, because [WeakReference] is not part of
 * the common stdlib -- this is exactly why the test lives in `desktopTest` rather than beside the
 * rest of [CollectionTest].
 *
 * `items` is a [mutableStateListOf] mutated structurally (`removeAll`), not a `mutableStateOf`
 * reassigned wholesale. The two look equivalent from the outside, but reassigning a
 * `mutableStateOf`'s value leaves the old snapshot state record briefly reachable within a single
 * test's snapshot -- an artifact of Compose's own snapshot system, unrelated to anything this
 * framework does, and it was producing a false leak signal even for a plain list with nothing
 * else going on. A structural removal from a [androidx.compose.runtime.snapshots.SnapshotStateList]
 * does not carry that artifact, confirmed against the identical harness with no [Collection]
 * involved at all before this test was trusted to say anything about [Collection] itself.
 */
@OptIn(ExperimentalTestApi::class)
class CollectionLeakTest {

    private val list = ElementId("documents")

    /** A distinct instance per document, so a [WeakReference] to one specific instance means something. */
    private class Doc(val id: String)

    /**
     * `likeness` used to grow for the life of the `Collection`, because nothing ever removed a
     * subject from it once added -- unlike `order`, which was actively pruned. A subject dropped
     * outright (no destroy act, no ghost, nothing recoverable) should take its likeness with it.
     */
    @Test
    fun `a subject's likeness is released once its slot closes, not held for the life of the collection`() =
        runComposeUiTest {
            val ghosts = Ghosts()
            var gone: Doc? = Doc("gone")
            val weak = WeakReference(gone)
            val items = mutableStateListOf(Doc("keep"), gone!!)

            setContent {
                CompositionLocalProvider(
                    LocalElements provides ElementRegistry(),
                    LocalPractice provides Practice(),
                    LocalGhosts provides ghosts,
                ) {
                    Collection(
                        items = items,
                        creator = Act.create("doc.new", SubjectId("doc.new"), into = list),
                        key = { SubjectId(it.id) },
                        modifier = Modifier.size(400.dp),
                        creatorContent = { Box(Modifier.size(48.dp)) },
                        item = { Box(Modifier.size(40.dp)) },
                    )
                }
            }
            waitForIdle()

            items.removeAll { it.id == "gone" }
            gone = null
            waitForIdle()

            repeat(10) {
                System.gc()
                Thread.sleep(30)
            }

            assertNull(weak.get(), "The collection's likeness cache should not outlive the subject it copied.")
        }
}
