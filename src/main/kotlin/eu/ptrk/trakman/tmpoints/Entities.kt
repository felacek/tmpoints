package eu.ptrk.trakman.tmpoints

import jakarta.persistence.*
import java.time.LocalDate

enum class QuestType {
    FINISHES, AUTHOR, LOCAL, DEDI, MISC // TODO: time on a specific map
}

@Entity
@Table(name = "quests")
data class Quest(
    @Column(nullable = false, length = 30, unique = true) val name: String,
    @Column(length = 255) val description: String = "",
    val type: QuestType,
    val goal: Int = 0,
    val reward: Int,
    val minPoints: Int = 0,
    @OneToOne var server: Server? = null,
    @Id @GeneratedValue val id: Int? = null
)

@Entity
@Table(name = "playerquests")
data class PlayerQuest(
    @ManyToOne val quest: Quest,
    var completed: Boolean,
    val expires: LocalDate,
    @ManyToOne(fetch = FetchType.LAZY) val player: Player? = null,
    @Id @GeneratedValue val id: Int? = null
)

@Entity
@Table(name = "players")
data class Player(
    @Column(nullable = false, length = 40, unique = true) val login: String,
    var points: Int,
    @OneToMany @JoinColumn(name = "player_id", referencedColumnName = "id", unique = true) var quests: MutableSet<PlayerQuest>,
    var currentMapFinishes: Int = 0,
    var currentMapTime: Int = 0,
    @Id @GeneratedValue val id: Int? = null
)

@Entity
@Table(name = "servers")
data class Server(
    @Column(nullable = false, length = 40, unique = true) val login: String,
    val password: String,
    var online: Boolean,
    var currentMapUid: String,
    var currentMapAuthorTime: Int,
    @OneToMany var players: MutableSet<Player>,
    @Id @GeneratedValue val id: Int? = null
)

@Entity
@Table(name = "admins")
data class Admin(
    @Column(nullable = false, length = 20, unique = true) val login: String,
    val password: String,
    @Id @GeneratedValue val id: Int? = null
)