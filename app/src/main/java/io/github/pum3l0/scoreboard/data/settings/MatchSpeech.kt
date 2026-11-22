package io.github.pum3l0.scoreboard.data.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.text.SpannableString
import android.text.SpannableStringBuilder
import io.github.pum3l0.scoreboard.R
import android.text.Spanned
import android.text.style.TtsSpan
import io.github.pum3l0.scoreboard.domain.match.Score
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SpeechStatus { OFF, STARTING, DOWNLOADING, READY, TUNED, UNAVAILABLE }

class MatchSpeech(context: Context, private val language: AppLanguage) {
    private val locale = language.locale
    private val context = context.createConfigurationContext(Configuration().apply { setLocale(language.locale) })
    private val main = Handler(Looper.getMainLooper())
    private val mutableStatus = MutableStateFlow(SpeechStatus.OFF)
    val status = mutableStatus.asStateFlow()

    var ratePercent: Int = DEFAULT_SPEECH_RATE

    private var engine: TextToSpeech? = null
    private var request = 0
    private var pendingSpeech: CharSequence? = null
    private var variant = VoiceVariant.MALE
    private var requestedVoice: String? = null
    private var downloadSince: Long? = null
    private val poll = Runnable { selectVoice() }

    fun start(voice: VoiceVariant) {
        if (engine != null) return
        variant = voice
        mutableStatus.value = SpeechStatus.STARTING
        val generation = ++request
        engine = try {
            TextToSpeech(context) { status ->
                main.post {
                    if (generation != request) return@post
                    if (status == TextToSpeech.SUCCESS) selectVoice() else mutableStatus.value = SpeechStatus.UNAVAILABLE
                }
            }
        } catch (_: RuntimeException) {
            mutableStatus.value = SpeechStatus.UNAVAILABLE
            null
        }
    }

    private fun selectVoice() {
        val speech = engine ?: return
        val local = speech.voices.orEmpty().filter {
            it.locale.language == locale.language && !it.isNetworkConnectionRequired
        }
        val preferred = local.filter { it.matches(language, variant) }.sortedWith(
            compareByDescending<Voice> { it.locale.country == locale.country }.thenByDescending { it.quality },
        )
        val installed = local.filter { it.isReady() }.maxWithOrNull(
            compareBy<Voice> { it.locale.country == locale.country }.thenBy { it.quality },
        )
        val exact = preferred.firstOrNull { it.isReady() }
        if (exact != null && speech.setVoice(exact) == TextToSpeech.SUCCESS) {
            complete(speech, tuned = false)
            return
        }
        val downloadable = preferred.firstOrNull() ?: if (installed == null) local.maxByOrNull { it.quality } else null
        val since = downloadSince
        if (downloadable != null && !downloadable.isReady() &&
            (since == null || SystemClock.elapsedRealtime() - since < DOWNLOAD_TIMEOUT_MS)
        ) {
            if (requestedVoice != downloadable.name) {
                requestedVoice = downloadable.name
                downloadSince = SystemClock.elapsedRealtime()
                speech.setVoice(downloadable)
            }
            mutableStatus.value = SpeechStatus.DOWNLOADING
            main.postDelayed(poll, 1_500)
            return
        }
        if (downloadable == null && installed == null && since == null &&
            speech.isLanguageAvailable(locale) == TextToSpeech.LANG_MISSING_DATA
        ) {
            downloadSince = SystemClock.elapsedRealtime()
            speech.setLanguage(locale)
            mutableStatus.value = SpeechStatus.DOWNLOADING
            main.postDelayed(poll, 1_500)
            return
        }
        if (installed == null && since != null && SystemClock.elapsedRealtime() - since < DOWNLOAD_TIMEOUT_MS) {
            main.postDelayed(poll, 1_500)
            return
        }
        if (installed != null && speech.setVoice(installed) == TextToSpeech.SUCCESS) {
            complete(speech, tuned = true)
        } else {
            mutableStatus.value = SpeechStatus.UNAVAILABLE
            pendingSpeech = null
        }
    }

    private fun complete(speech: TextToSpeech, tuned: Boolean) {
        speech.setPitch(if (tuned) variant.pitch else 1f)
        mutableStatus.value = if (tuned) SpeechStatus.TUNED else SpeechStatus.READY
        pendingSpeech?.let(::speakText)
        pendingSpeech = null
    }

    fun speak(score: Score?, serverName: String?) {
        val text = SpannableStringBuilder()
        score?.let { text.append(scoreText(it)) }
        serverName?.let {
            if (text.isNotEmpty()) text.append(". ")
            text.append(context.getString(R.string.voice_serving, it))
        }
        if (text.isNotEmpty()) speakText(text)
    }

    private fun speakText(text: CharSequence) {
        when (status.value) {
            SpeechStatus.STARTING, SpeechStatus.DOWNLOADING -> pendingSpeech = text
            SpeechStatus.READY, SpeechStatus.TUNED -> {
                val speech = engine ?: return
                speech.setSpeechRate(ratePercent / 100f)
                if (speech.speak(text, TextToSpeech.QUEUE_FLUSH, null, "score") != TextToSpeech.SUCCESS) {
                    mutableStatus.value = SpeechStatus.UNAVAILABLE
                }
            }
            else -> Unit
        }
    }

    private fun scoreText(score: Score): CharSequence {
        val first = score.first.toString()
        val second = score.second.toString()
        return SpannableString("$first — $second").apply {
            setSpan(cardinal(score.first), 0, first.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(cardinal(score.second), length - second.length, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    fun stop() {
        ++request
        main.removeCallbacks(poll)
        pendingSpeech = null
        requestedVoice = null
        downloadSince = null
        engine?.shutdown()
        engine = null
        mutableStatus.value = SpeechStatus.OFF
    }
}

private const val DOWNLOAD_TIMEOUT_MS = 90_000L

private fun cardinal(number: Int): TtsSpan = TtsSpan.CardinalBuilder(number.toLong()).build()

private fun Voice.isReady() = TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in features.orEmpty()

private fun Voice.matches(language: AppLanguage, variant: VoiceVariant): Boolean {
    val identifiers = when (language) {
        AppLanguage.RUSSIAN -> when (variant) {
            VoiceVariant.MALE -> setOf("ru-ru-x-rud-local", "ru-ru-x-ruf-local")
            VoiceVariant.FEMALE -> setOf("ru-ru-x-ruc-local", "ru-ru-x-rue-local", "ru-ru-x-dfc-local")
        }
        AppLanguage.ENGLISH -> when (variant) {
            VoiceVariant.MALE -> setOf("en-us-x-iol-local", "en-us-x-tpd-local", "en-us-x-iom-local")
            VoiceVariant.FEMALE -> setOf("en-us-x-tpc-local", "en-us-x-tpf-local", "en-us-x-iob-local")
        }
    }
    val id = name.lowercase()
    val genderTag = variant.name.lowercase()
    return id in identifiers || id.split('-', '_', ' ').any { it == genderTag }
}
