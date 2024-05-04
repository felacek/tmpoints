package eu.ptrk.trakman.tmpoints.controllers

import eu.ptrk.trakman.tmpoints.*
import eu.ptrk.trakman.tmpoints.services.AdminService
import eu.ptrk.trakman.tmpoints.services.PlayerService
import eu.ptrk.trakman.tmpoints.services.QuestService
import eu.ptrk.trakman.tmpoints.services.ServerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/admin")
class AdminController(
    private val adminService: AdminService,
    private val questService: QuestService,
    private val tokenService: TokenService,
    private val playerService: PlayerService,
    private val serverService: ServerService,
) {

    @GetMapping(path = ["", "/"])
    fun loginPage(authentication: Authentication?, model: Model): String {
        if (authentication == null || !authentication.isAuthenticated ||
            !authentication.authorities.mapNotNull {it.authority}.contains("ADMIN")) return "adminlogin"
        return "admin"
    }

    @PostMapping("/login")
    fun login(@RequestBody request: AdminLoginRequest): ResponseEntity<TokenResponse> {
        try {
            val token = tokenService.createAdminToken(adminService.login(request.login, request.password))
            return ResponseEntity.ok(TokenResponse(token, "/admin"))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: DisabledException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Disabled")
        } catch (e: BadCredentialsException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
        }
    }

    @PostMapping("/register")
    fun register(@RequestBody request: AdminLoginRequest): ResponseEntity<TokenResponse> {
        try {
            val token = tokenService.createAdminToken(adminService.register(request.login, request.password))
            return ResponseEntity.ok(TokenResponse(token))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: DisabledException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Disabled")
        } catch (e: BadCredentialsException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
        }
    }

    @PostMapping("/daily")
    @PreAuthorize("hasRole('ADMIN')")
    fun daily(): ResponseEntity<Unit> {
        try {
            playerService.daily()
            return ResponseEntity.ok(Unit)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    /*@GetMapping("/")
    fun adminPage(model: Model): String {
        model["title"] = "Admin page"
        return "admin"
    }*/

    @GetMapping("/quest/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getQuest(authentication: Authentication, @PathVariable id: Int): ResponseEntity<Quest> {
        try {
        return ResponseEntity.ok(questService.getQuest(id))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    @PutMapping("/quest")
    @PreAuthorize("hasRole('ADMIN')")
    fun putQuest(authentication: Authentication, @RequestBody request: AddQuestRequest): ResponseEntity<Quest> {
        try {
            val quest = questService.addQuest(request.name, request.description, request.type, request.goal, request.reward, request.minPoints)
            val questId = quest.id ?:
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not get id of added quest")
            if (request.server != null) return ResponseEntity.ok(questService.setQuestServer(questId, serverService.getServer(request.server)))
            return ResponseEntity.ok(quest)
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        }
    }

    @GetMapping("/quest")
    @PreAuthorize("hasRole('ADMIN')")
    fun getQuests(authentication: Authentication): ResponseEntity<Set<Quest>> {
        return ResponseEntity.ok(questService.getAllQuests())
    }

    @GetMapping("/player")
    @PreAuthorize("hasRole('ADMIN')")
    fun getPlayers(authentication: Authentication): ResponseEntity<Set<Player>> {
        return ResponseEntity.ok(playerService.getPlayers())
    }

    @GetMapping("/server")
    @PreAuthorize("hasRole('ADMIN')")
    fun getServers(authentication: Authentication): ResponseEntity<Set<Server>> {
        return ResponseEntity.ok(serverService.getServers())
    }
}