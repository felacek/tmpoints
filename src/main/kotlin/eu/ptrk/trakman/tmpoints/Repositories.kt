package eu.ptrk.trakman.tmpoints

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.LocalDate

interface QuestRepository: CrudRepository<Quest, Int> {
    fun getQuestById(id: Int): Quest?
    fun getQuestByName(name: String): Quest?
    fun getQuestsByMinPointsIsLessThanEqual(minPoints: Int): Set<Quest>
    fun getQuestsByIdIsNotInAndMinPointsIsLessThanEqual(ids: Set<Int>, minPoints: Int): Set<Quest>
}

interface PlayerQuestRepository: CrudRepository<PlayerQuest, Int> {
    @Query("select p.quest.id from PlayerQuest p where p.expires > ?2 and p.player.id = ?1")
    fun getInvalidQuestsByPlayer(id: Int, date: LocalDate = LocalDate.now()): Set<Int>

    fun getPlayerQuestsByPlayerId(id: Int): Set<PlayerQuest>
    fun getPlayerQuestsByPlayerLoginIn(logins: Set<String>): Set<PlayerQuest>
    fun getPlayerQuestByPlayerIdAndQuestId(player: Int, quest: Int): PlayerQuest?
}

interface PlayerRepository: CrudRepository<Player, Int> {
    fun getPlayerByLogin(login: String): Player?
    fun getPlayerById(id: Int): Player?
    //@Query("select p.login, p.points, q, p.id from Player p join PlayerQuest q on p.id = q.player.id where p.login in (?1)")
    fun getPlayersByLoginIn(logins: Set<String>): Set<Player>
}

interface ServerRepository: CrudRepository<Server, Int> {
    fun getServerByLogin(login: String): Server?
}

interface AdminRepository: CrudRepository<Admin, Int> {
    fun getAdminByLogin(login: String): Admin?
}