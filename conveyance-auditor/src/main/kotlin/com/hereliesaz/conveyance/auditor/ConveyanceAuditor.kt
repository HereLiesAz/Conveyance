package com.hereliesaz.conveyance.auditor

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.anthropic.models.messages.Base64ImageSource
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.ImageBlockParam
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.Model
import com.anthropic.models.messages.TextBlockParam
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.hereliesaz.conveyance.AuditFrame
import com.hereliesaz.conveyance.AuditReport
import com.hereliesaz.conveyance.ElementId
import com.hereliesaz.conveyance.Grade
import com.hereliesaz.conveyance.Prediction
import com.hereliesaz.conveyance.Verdict
import java.util.Base64

/**
 * Runs the two rules no structural check can settle.
 *
 * *The reader can tell beforehand what each will do* needs a reader, and *not omit necessary
 * details* needs someone to notice what is missing. Both were written off as human-only work. They
 * are not — they are work for a viewer who has never seen the product, and that is something this
 * framework can arrange on demand.
 *
 * **The viewer is kept naive, deliberately and structurally.** The first pass receives the rendered
 * image and nothing else: no element names, no act identifiers, no source, no [AuditFrame]. The
 * moment it can read the code it stops predicting and starts reciting, and the measurement becomes
 * worthless. The truth is held back and used only in the second pass, to grade what the first pass
 * already committed to.
 *
 * That separation is the whole design. Everything else here is plumbing.
 */
class ConveyanceAuditor(
    private val client: AnthropicClient = AnthropicOkHttpClient.fromEnv(),
    private val model: Model = Model.of("claude-opus-5"),
) {
    private val json = ObjectMapper().registerKotlinModule()

    /**
     * Show [png] to a viewer who has never seen it, then grade what they said against [frame].
     *
     * @param png the rendered surface, exactly as a person would see it.
     * @param frame what is actually true, which the viewer never sees.
     */
    fun audit(png: ByteArray, frame: AuditFrame): AuditReport {
        val predictions = predict(png)
        return grade(predictions, frame)
    }

    /** Pass one. Pixels in, expectations out. Nothing else crosses this boundary. */
    private fun predict(png: ByteArray): List<Prediction> {
        val response = client.messages().create(
            MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessageOfBlockParams(
                    listOf(
                        ContentBlockParam.ofImage(
                            ImageBlockParam.builder()
                                .source(
                                    Base64ImageSource.builder()
                                        .data(Base64.getEncoder().encodeToString(png))
                                        // Required on the wire. The builder accepts its absence
                                        // and the API rejects it, so omitting this compiles and
                                        // then fails in CI with a 400.
                                        .mediaType(Base64ImageSource.MediaType.IMAGE_PNG)
                                        .build(),
                                )
                                .build(),
                        ),
                        ContentBlockParam.ofText(TextBlockParam.builder().text(PREDICT).build()),
                    ),
                )
                .build(),
        )
        return parse(response.content().mapNotNull { it.text().orElse(null) }.joinToString("\n") { it.text() })
    }

    /** Pass two. The truth arrives only now, and only to judge an answer already given. */
    private fun grade(predictions: List<Prediction>, frame: AuditFrame): AuditReport {
        val truth = json.writeValueAsString(frame)
        val guesses = json.writeValueAsString(predictions)
        val response = client.messages().create(
            MessageCreateParams.builder()
                .model(model)
                .maxTokens(MAX_TOKENS)
                .addUserMessage("$GRADE\n\nWHAT THEY SAID:\n$guesses\n\nWHAT IS TRUE:\n$truth")
                .build(),
        )
        val text = response.content().mapNotNull { it.text().orElse(null) }.joinToString("\n") { it.text() }
        val parsed = json.readTree(strip(text))
        val verdicts = parsed.path("verdicts").map { node ->
            Verdict(
                element = node.path("element").asText("").takeIf { it.isNotBlank() }?.let(::ElementId),
                grade = runCatching { Grade.valueOf(node.path("grade").asText()) }.getOrDefault(Grade.NoIdea),
                predicted = node.path("predicted").asText(""),
                actual = node.path("actual").asText(""),
                note = node.path("note").asText(""),
            )
        }
        val omissions = parsed.path("omissions").map { it.asText() }
        return AuditReport(frame.surface, verdicts, omissions)
    }

    private fun parse(text: String): List<Prediction> =
        json.readTree(strip(text)).path("predictions").map { node ->
            Prediction(
                where = node.path("where").asText(""),
                expectation = node.path("expectation").asText(""),
                certain = node.path("certain").asBoolean(false),
            )
        }

    /** Models fence JSON more often than not, and a fence is not a parse error worth failing on. */
    private fun strip(text: String): String =
        text.substringAfter("```json", text).substringAfter("```", text).substringBeforeLast("```")
            .trim()
            .ifBlank { "{}" }

    private companion object {
        const val MAX_TOKENS = 8_000L

        val PREDICT = """
            You have never seen this application before. You are looking at one screen of it.

            Point at everything you believe you can interact with. For each one, say where it is in
            your own words, what you expect will happen if you touch it, and whether you are
            actually sure or just guessing.

            Then, separately: is there anything you would want to know before touching any of these
            that this screen does not tell you? Cost, permanence, whether it can be undone, what it
            will affect. Only list what is genuinely missing from what you can see.

            Guess honestly. If you cannot tell what something does, say so -- that is a useful
            answer and pretending otherwise is not.

            Reply with JSON only:
            {"predictions":[{"where":"...","expectation":"...","certain":true|false}],
             "missing":["..."]}
        """.trimIndent()

        val GRADE = """
            Below are one person's predictions about a screen they had never seen, and then the
            truth about what that screen actually does. They did not have the truth when they
            answered.

            Grade each prediction against the truth:
              Right  - they expected substantially what actually happens.
              Wrong  - they expected something that does not happen. Confident and wrong is the
                       worst case: the interface actively misled them.
              NoIdea - they said they could not tell.

            Match predictions to elements by position and description. An element in the truth that
            nobody predicted at all is a Verdict with grade NoIdea and a note saying it went
            unmentioned.

            Then list omissions: anything in the truth that a person would need before acting and
            that the screen does not convey. An act that is Heavy or not reversible, where nothing
            visible says so, is always an omission.

            Reply with JSON only:
            {"verdicts":[{"element":"...","grade":"Right|Wrong|NoIdea","predicted":"...",
                          "actual":"...","note":"..."}],
             "omissions":["..."]}
        """.trimIndent()
    }
}
