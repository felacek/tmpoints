package eu.ptrk.trakman.tmpoints.services

import eu.ptrk.trakman.tmpoints.*
import org.springframework.stereotype.Service

@Service
class QuestService(
    private val questRepository: QuestRepository,
    private val playerQuestRepository: PlayerQuestRepository
) {

    fun getQuest(id: Int): Quest {
        val quest = questRepository.getQuestById(id)
        if (quest != null) return quest
        throw IllegalArgumentException("Quest with id $id does not exist")
    }

    fun addQuest(name: String, description: String, type: String, goal: Int, reward: Int, minPoints: Int): Quest {
        val exists = questRepository.getQuestByName(name)
        if (exists != null) return exists
        if (name.length > 30) throw IllegalArgumentException("Name must not exceed 30 characters")
        if (description.length > 255) throw IllegalArgumentException("Description must not exceed 256 characters")
        return questRepository.save(Quest(name, description, enumValueOf(type), goal, reward, minPoints))
    }

    fun setQuestServer(id: Int, server: Server): Quest {
        val quest = getQuest(id)
        quest.server = server
        return questRepository.save(quest)
    }

    fun generateQuests(player: Int, amount: Int, points: Int = 0): List<Quest> {
        val invalid = playerQuestRepository.getInvalidQuestsByPlayer(player)
        val quests = if (invalid.isEmpty()) questRepository.getQuestsByMinPointsIsLessThanEqual(points)
        else questRepository.getQuestsByIdIsNotInAndMinPointsIsLessThanEqual(invalid, points)
        return quests.shuffled().take(amount)
    }

    fun getQuests(player: Int): Set<PlayerQuest> {
        return playerQuestRepository.getPlayerQuestsByPlayerId(player)
    }

    fun getAllQuests(): Set<Quest> {
        return questRepository.findAll().toSet()
    }
}