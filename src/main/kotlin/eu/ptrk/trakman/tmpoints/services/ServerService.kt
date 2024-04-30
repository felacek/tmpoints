package eu.ptrk.trakman.tmpoints.services

import eu.ptrk.trakman.tmpoints.*
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class ServerService(private val serverRepository: ServerRepository,
                    private val playerService: PlayerService,
                    private val questService: QuestService,
                    private val encoder: PasswordEncoder,
                    private val authenticationManager: AuthenticationManager) {

    fun addServer(login: String, password: String, currentMapUid: String,
                  currentMapAuthorTime: Int, playerLogins: Set<String>): Map<Player, Set<PlayerQuest>> {
        val exists = serverRepository.getServerByLogin(login)
        if (exists != null) {
            val authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(login, password)
            authenticationManager.authenticate(authenticationRequest)
            exists.online = true
            val ret = playerService.getOrAddPlayers(playerLogins)
            exists.players.addAll(ret.keys)
            newMap(exists, currentMapUid, currentMapAuthorTime)
            return ret
        }
        if (login.length > 40) throw IllegalArgumentException("Login cannot exceed 40 characters")
        val ret = playerService.getOrAddPlayers(playerLogins)
        serverRepository.save(Server(login, encoder.encode(password), true, currentMapUid, currentMapAuthorTime, ret.keys.toMutableSet()))
        return ret
    }

    fun newMap(server: Server, currentMapUid: String, currentMapAuthorTime: Int) {
        server.currentMapUid = currentMapUid
        server.currentMapAuthorTime = currentMapAuthorTime
        playerService.newMap(server.players)
        serverRepository.save(server)
    }

    fun newMap(login: String, currentMapUid: String, currentMapAuthorTime: Int) {
        newMap(getServer(login), currentMapUid, currentMapAuthorTime)
    }

    fun playerJoin(login: String, playerLogin: String): Pair<Player, Set<PlayerQuest>> {
        val server = getServer(login)
        val players = playerService.getOrAddPlayers(setOf(playerLogin))
        val player = players.keys.first()
        if (server.players.add(player)) {
            serverRepository.save(server)
            return Pair(player, players[player] ?: throw IllegalArgumentException("Player $playerLogin has no quest set"))
        } else throw IllegalArgumentException("Player $playerLogin is already on the server $login")
    }

    fun playerLeave(login: String, playerLogin: String): Boolean {
        val server = getServer(login)
        val player = getPlayer(server, playerLogin)
        if (server.players.remove(player)) {
            serverRepository.save(server)
            return true
        }
        return false
    }

    fun playerFinished(login: String, playerLogin: String, time: Int, local: Int, dedi: Int, improved: Boolean): FinishedResponse {
        val server = getServer(login)
        val player = getPlayer(server, playerLogin)
        player.currentMapFinishes++
        val impr = improved && (player.currentMapTime == 0 || time < player.currentMapTime)
        if (impr) player.currentMapTime = time
        val completed: MutableSet<Quest> = mutableSetOf()
        questService.getQuests(player.id ?: throw IllegalArgumentException("Player $login has no id")).forEach {
            if (it.completed) return@forEach
            if ((it.quest.server?.login ?: login) != login) return@forEach
            if (it.quest.type == QuestType.FINISHES && it.quest.goal <= player.currentMapFinishes) {
                try { completed.add(playerService.completeQuest(it)) } catch (_: IllegalArgumentException) {}
                return@forEach
            }
            if (!impr) return@forEach
            try {
                when (it.quest.type) {
                    QuestType.AUTHOR -> if (time <= server.currentMapAuthorTime) completed.add(playerService.completeQuest(it))
                    QuestType.LOCAL -> if (local > 0 && local <= it.quest.goal) completed.add(playerService.completeQuest(it))
                    QuestType.DEDI -> if (dedi > 0 && dedi <= it.quest.goal) completed.add(playerService.completeQuest(it))
                    else -> return@forEach
                }
            } catch (_: IllegalArgumentException) {}
        }
        // player completed all assigned quests
        // TODO: streaks
        if (completed.isNotEmpty() && completed.size == playerService.maxQuests) player.points += 100
        return FinishedResponse(PlayerResponse(playerService.save(player), questService.getQuests(player.id)), completed)
    }

    fun playerCompletedMiscQuest(login: String, playerLogin: String, id: Int): FinishedResponse {
        val server = getServer(login)
        val player = getPlayer(server, playerLogin)
        if (player.id == null) throw IllegalArgumentException("Player $login has no id")
        val completed = playerService.completeQuest(playerLogin, id, login, true)
        return FinishedResponse(PlayerResponse(playerService.save(player), questService.getQuests(player.id)), setOf(completed))
    }

    fun getServers(): Set<Server> = serverRepository.findAll().toSet()

    fun getServer(login: String): Server = serverRepository.getServerByLogin(login) ?:
    throw IllegalArgumentException("Server with login $login does not exist, I don't think this is supposed to happen...")

    private fun getPlayer(server: Server, login: String): Player = server.players.find { it.login == login } ?:
    throw IllegalArgumentException("Player $login is not on the server ${server.login}")
}