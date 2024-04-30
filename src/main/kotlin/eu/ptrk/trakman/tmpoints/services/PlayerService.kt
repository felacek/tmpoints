package eu.ptrk.trakman.tmpoints.services

import eu.ptrk.trakman.tmpoints.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PlayerService(
    private val playerRepository: PlayerRepository,
    private val questService: QuestService,
    private val playerQuestRepository: PlayerQuestRepository,
    @Value("\${eu.ptrk.trakman.quests.max}") val maxQuests: Int,
    @Value("\${eu.ptrk.trakman.quests.expire}") private val expireInDays: Long
) {

    fun getPlayer(login: String): Player {
        val player = playerRepository.getPlayerByLogin(login) ?: throw IllegalArgumentException("Player with login $login does not exist")
        return player
    }

    fun getPlayer(id: Int): Player {
        val player = playerRepository.getPlayerById(id) ?: throw IllegalArgumentException("Player with id $id does not exist")
        return player
    }

    fun addPlayer(login: String): Pair<Player, Set<PlayerQuest>> {
        val exists = playerRepository.getPlayerByLogin(login)
        if (exists?.id != null) return Pair(exists, questService.getQuests(exists.id))
        if (login.length > 40) throw IllegalArgumentException("Login cannot exceed 40 characters")
        val player = Player(login, 0, mutableSetOf(), 0, 0)
        playerRepository.save(player)
        val quests = fillQuests(player)
        return Pair(if (quests.isNotEmpty()) playerRepository.save(player)
        else player, quests.toSet())
    }

    fun getOrAddPlayers(logins: Set<String>): Map<Player, Set<PlayerQuest>> {
        val playerQuests = playerQuestRepository.getPlayerQuestsByPlayerLoginIn(logins)
        val inDB = logins.mapNotNull {
            val quests = playerQuests.filter { q -> (q.player?.login ?: "") == it }
            if (quests.isEmpty()) null
            else {
                val player = quests.first().player
                    ?: throw NoSuchElementException("Somehow the quest ${quests.first().quest.name} doesn't have a player, impossible error")
                val questSet = quests.toSet()
                Pair(player, questSet)
            }
        }.toMap()
        val notInDB = logins subtract (inDB.map { it.key.login }).toSet()
        return inDB + (notInDB.associate { addPlayer(it) })
    }

    fun completeQuest(login: String, questId: Int, server: String? = null, misc: Boolean = false): Quest {
        val player = getPlayer(login).id ?: throw IllegalArgumentException("Player with login $login does not have an id???")
        val playerQuest = playerQuestRepository.getPlayerQuestByPlayerIdAndQuestId(player, questId) ?:
        throw IllegalArgumentException("Player $login is not assigned quest $questId")
        if (misc && playerQuest.quest.type != QuestType.MISC)
            throw IllegalArgumentException("Quest ${playerQuest.quest.name} is not a miscellaneous quest, you must provide proof it has been completed")
        return completeQuest(playerQuest, server)
    }

    fun completeQuest(playerQuest: PlayerQuest, server: String? = null): Quest {
        if (playerQuest.player == null)
            throw IllegalArgumentException("Quest ${playerQuest.quest.name} has no player assigned")
        if ((playerQuest.quest.server?.login ?: server) != server)
            throw IllegalArgumentException("Quest ${playerQuest.quest.name} is assigned to a different server")
        if (playerQuest.completed)
            throw IllegalArgumentException("Player ${playerQuest.player.id} has already completed quest ${playerQuest.quest.name}")
        if (playerQuest.expires.isBefore(LocalDate.now()))
            throw IllegalArgumentException("Quest ${playerQuest.quest.name} has expired for player ${playerQuest.player.id}")
        playerQuest.completed = true
        playerQuest.player.points += playerQuest.quest.reward
        playerQuestRepository.save(playerQuest)
        // TODO: add three quests daily
        fillQuests(playerQuest.player)
        return playerQuest.quest
    }

    fun newMap(players: Set<Player>) {
        players.forEach {
            it.currentMapTime = 0
            it.currentMapFinishes = 0
        }
        playerRepository.saveAll(players)
    }

    fun getPlayers(): Set<Player> {
        return playerRepository.findAll().toSet()
    }

    fun save(player: Player): Player {
        return playerRepository.save(player)
    }

    private fun fillQuests(player: Player): List<PlayerQuest> {
        if (player.id == null) throw throw IllegalArgumentException("Player with login ${player.login} does not have an id")
        val toAdd = questService.generateQuests(player.id, maxQuests, player.points)
        if (toAdd.isEmpty()) return listOf()
        val playerQuests = toAdd.map {
            PlayerQuest(it, false, LocalDate.now().plusDays(expireInDays), player)
        }
        playerQuestRepository.saveAll(playerQuests)
        return playerQuests
    }
}