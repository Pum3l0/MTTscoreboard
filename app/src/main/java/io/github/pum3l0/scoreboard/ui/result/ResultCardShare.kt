package io.github.pum3l0.scoreboard.ui.result

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import io.github.pum3l0.scoreboard.R
import io.github.pum3l0.scoreboard.data.match.MatchTiming
import io.github.pum3l0.scoreboard.domain.match.MatchState
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import io.github.pum3l0.scoreboard.ui.match.elapsedTime
import java.io.File

internal data class ShareColors(
    val background: Int,
    val surface: Int,
    val onSurface: Int,
    val accent: Int,
    val onAccent: Int,
)

internal fun createResultShare(context: Context, match: MatchState, timing: MatchTiming?, colors: ShareColors): Intent {
    val width = 1080
    val height = 515 + match.completedGames.size * 70
    val image = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(image)
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG)
    val decoration = Paint(Paint.ANTI_ALIAS_FLAG)
    val config = match.config
    fun rounded(left: Float, top: Float, right: Float, bottom: Float, radius: Float, color: Int) {
        decoration.color = color
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, decoration)
    }
    fun label(value: String, x: Float, baseline: Float, size: Float, color: Int, bold: Boolean = false, maxWidth: Float = 900f) {
        paint.textSize = size
        paint.color = color
        paint.typeface = Typeface.create("sans-serif", if (bold) Typeface.BOLD else Typeface.NORMAL)
        val text = TextUtils.ellipsize(value, paint, maxWidth, TextUtils.TruncateAt.END)
        canvas.drawText(text.toString(), x, baseline, paint)
    }
    val winner = if (match.winner == PlayerSide.FIRST) config.firstPlayerName else config.secondPlayerName
    canvas.drawColor(colors.background)
    rounded(52f, 50f, 1028f, 354f, 52f, colors.accent)
    label(context.getString(R.string.app_name), 94f, 114f, 40f, colors.onAccent, bold = true)
    label(context.getString(R.string.score_pair, match.gamesWon.first, match.gamesWon.second), 90f, 241f, 112f, colors.onAccent, bold = true)
    label(context.getString(R.string.player_order, config.firstPlayerName, config.secondPlayerName), 94f, 313f, 38f, colors.onAccent)
    rounded(52f, 376f, 1028f, height - 42f, 44f, colors.surface)
    label(context.getString(R.string.match_winner, winner), 94f, 432f, 36f, colors.onSurface, bold = true)
    match.completedGames.forEachIndexed { index, game ->
        val y = 492f + index * 70f
        label(context.getString(R.string.game_number, index + 1), 94f, y, 32f, colors.onSurface, maxWidth = 400f)
        label(context.getString(R.string.score_pair, game.score.first, game.score.second), 534f, y, 32f, colors.onSurface, bold = true, maxWidth = 216f)
        timing?.gamesMillis?.getOrNull(index)?.let {
            label(elapsedTime(it), 804f, y, 30f, colors.onSurface, maxWidth = 156f)
        }
    }
    timing?.let {
        label(context.getString(R.string.match_duration, elapsedTime(it.totalMillis)),
            94f, height - 61f, 31f, colors.onSurface, bold = true)
    }

    val directory = File(context.cacheDir, "result-cards")
    directory.mkdirs()
    val oldest = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1_000L
    directory.listFiles()?.filter { it.lastModified() < oldest }?.forEach(File::delete)
    val file = File.createTempFile("scoreboard-", ".png", directory)
    try {
        file.outputStream().use { check(image.compress(Bitmap.CompressFormat.PNG, 100, it)) }
    } catch (failure: Exception) {
        file.delete()
        throw failure
    } finally {
        image.recycle()
    }
    val uri = FileProvider.getUriForFile(context, "${context.packageName}.resultcards", file)
    return Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        clipData = ClipData.newRawUri("ScoreBoard", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}
