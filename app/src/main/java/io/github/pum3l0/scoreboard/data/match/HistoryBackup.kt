package io.github.pum3l0.scoreboard.data.match

import android.content.ContentResolver
import android.net.Uri
import io.github.pum3l0.scoreboard.domain.match.MatchConfig
import io.github.pum3l0.scoreboard.domain.match.MatchFormat
import io.github.pum3l0.scoreboard.domain.match.MatchReducer
import io.github.pum3l0.scoreboard.domain.match.MatchSnapshot
import io.github.pum3l0.scoreboard.domain.match.PlayerSide
import java.io.ByteArrayOutputStream
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener

object HistoryBackup {
    private const val format = "scoreboard-history"
    private const val version = 1
    private const val maxBytes = 16 * 1024 * 1024

    suspend fun write(resolver: ContentResolver, uri: Uri, matches: List<StoredMatch>) =
        withContext(Dispatchers.IO) {
            val bytes = encode(matches)
            checkNotNull(resolver.openOutputStream(uri, "wt")).use { it.write(bytes) }
        }

    suspend fun read(resolver: ContentResolver, uri: Uri): List<StoredMatch> =
        withContext(Dispatchers.IO) {
            val bytes = checkNotNull(resolver.openInputStream(uri)).use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8192)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    require(output.size() + count <= maxBytes)
                    output.write(buffer, 0, count)
                }
                output.toByteArray()
            }
            decode(bytes)
        }

    internal fun encode(matches: List<StoredMatch>): ByteArray {
        val rows = JSONArray()
        matches.forEach { item ->
            val snapshot = item.match.snapshot()
            rows.put(
                JSONObject()
                    .put("id", item.id)
                    .put("firstName", snapshot.config.firstPlayerName)
                    .put("secondName", snapshot.config.secondPlayerName)
                    .put("gamesToWin", snapshot.config.format.gamesToWin)
                    .put("firstServer", snapshot.config.firstServer.storageCode)
                    .put("sidesSwapped", item.sidesSwapped)
                    .put("timing", item.timing?.encode())
                    .put("points", snapshot.encodedPoints())
                    .put("startedAt", item.startedAt)
                    .put("finishedAt", checkNotNull(item.finishedAt)),
            )
        }
        val json = JSONObject().put("format", format).put("version", version).put("matches", rows)
        return json.toString().toByteArray(Charsets.UTF_8).also { require(it.size <= maxBytes) }
    }

    internal fun decode(bytes: ByteArray): List<StoredMatch> {
        require(bytes.size <= maxBytes)
        val source = JSONTokener(bytes.decodeToString(throwOnInvalidSequence = true))
        val json = source.nextValue() as JSONObject
        require(source.nextClean() == '\u0000')
        require(json.getString("format") == format && json.getInt("version") == version)
        val rows = json.getJSONArray("matches")
        return List(rows.length()) { index -> decodeMatch(rows.getJSONObject(index)) }
    }

    private fun decodeMatch(row: JSONObject): StoredMatch {
        val id = row.getString("id")
        require(UUID.fromString(id).toString() == id)
        val config = MatchConfig(
            firstPlayerName = row.getString("firstName"),
            secondPlayerName = row.getString("secondName"),
            format = MatchFormat.entries.first { it.gamesToWin == row.getInt("gamesToWin") },
            firstServer = playerFromCode(row.getInt("firstServer")),
        )
        val match = MatchReducer.restore(MatchSnapshot(config, decodePoints(row.getString("points"))))
        require(match.winner != null)
        return StoredMatch(
            id, match, row.getLong("startedAt"), row.getLong("finishedAt"), row.optBoolean("sidesSwapped"),
            if (row.isNull("timing")) null else decodeTiming(row.getString("timing"), match.completedGames.size),
        )
    }
}

internal val PlayerSide.storageCode: Int
    get() = when (this) {
        PlayerSide.FIRST -> 1
        PlayerSide.SECOND -> 2
    }

internal fun playerFromCode(code: Int): PlayerSide = when (code) {
    1 -> PlayerSide.FIRST
    2 -> PlayerSide.SECOND
    else -> error("Unknown player code: $code")
}

internal fun MatchSnapshot.encodedPoints() = buildString(pointWinners.size) {
    pointWinners.forEach { append(it.storageCode) }
}

internal fun decodePoints(points: String): List<PlayerSide> = points.map {
    when (it) {
        '1' -> PlayerSide.FIRST
        '2' -> PlayerSide.SECOND
        else -> error("Unknown player code: $it")
    }
}
