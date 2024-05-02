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
    val completedQuests: Set<QuestResponse>
)

data class TokenPlayersResponse(
    val token: String,
    val players: List<PlayerResponse>
)

class PlayerResponse(player: Player, quests: Set<PlayerQuest>) {
    val login: String = player.login
    val points: Int = player.points
    val streak: Int = player.streak
    val completableQuests = quests.filter {
        !it.completed() && it.expires.isAfter(LocalDate.now())
    }.map { QuestResponse(it.quest, it.remaining) }
}

class QuestResponse(quest: Quest, remaining: Int) {
    val id = quest.id
    val name = quest.name
    val description = quest.description
    val type = quest.type
    val goal = quest.goal
    val reward = quest.reward
    val amount = quest.amount
    val remaining = remaining
    val server = quest.server
}

data class AdminLoginRequest(
    val login: String,
    val password: String
)

data class ServerRegisterRequest(
    val login: String,
    val password: String
)

data class TMOAuthTokenRequest(
    val client_id: String,
    val client_secret: String,
    val redirect_uri: String,
    val code: String,
    val grant_type: String = "authorization_code"
)

data class TMOAuthTokenResponse(
    val access_token: String,
    val token_type: String,
    val expires: Long,
    val login: String
)

data class TMServersResponse(
    val login: String,
    val name: String,
    val path: String
)

data class TokenResponse(val token: String)
