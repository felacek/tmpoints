package eu.ptrk.trakman.tmpoints

import java.time.LocalDate

data class AddQuestRequest(
    val name: String,
    val description: String,
    val type: String,
    val goal: Int,
    val reward: Int,
    val minPoints: Int,
    val server: String? = null
)

data class AddServerRequest(
    val login: String,
    val password: String,
    val currentMapUid: String,
    val currentMapAuthorTime: Int,
    val playerLogins: Set<String>
)

data class MapRequest(
    val currentMapUid: String,
    val currentMapAuthorTime: Int
)

data class FinishedRequest(
    val time: Int,
    val local: Int = 0,
    val dedi: Int = 0,
    val improved: Boolean = true
)

data class FinishedResponse(
    val player: PlayerResponse,
    val completedQuests: Set<Quest>
)

data class TokenPlayersResponse(
    val token: String,
    val players: List<PlayerResponse>
)

class PlayerResponse(player: Player, quests: Set<PlayerQuest>) {
    val login: String = player.login
    val points: Int = player.points
    val completableQuests = quests.filter {
        !it.completed && it.expires.isAfter(LocalDate.now())
    }.map { it.quest }
}

data class AdminLoginRequest(
    val login: String,
    val password: String
)

data class TokenResponse(val token: String)
