package eu.ptrk.trakman.tmpoints.controllers

import eu.ptrk.trakman.tmpoints.*
import eu.ptrk.trakman.tmpoints.TokenService
import eu.ptrk.trakman.tmpoints.services.ServerService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.server.ResponseStatusException

@Controller
@RequestMapping("/server")
class ServerController(private val serverService: ServerService, private val tokenService: TokenService) {

    @PostMapping("/login")
    fun login(@RequestBody request: AddServerRequest): ResponseEntity<TokenPlayersResponse> {
        try {
            val players = serverService.addServer(request.login, request.password, request.currentMapUid, request.currentMapAuthorTime, request.playerLogins)
            return ResponseEntity.ok(TokenPlayersResponse(tokenService.createServerToken(request.login), players.map { PlayerResponse(it.key, it.value) }))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: DisabledException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Disabled")
        } catch (e: BadCredentialsException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
        }
    }

    @PostMapping("/map")
    @PreAuthorize("hasRole('SERVER')")
    fun newMap(authentication: Authentication, @RequestBody request: MapRequest): ResponseEntity<Unit> {
        try {
            return ResponseEntity.ok(
                serverService.newMap(
                    authentication.principal as String,
                    request.currentMapUid,
                    request.currentMapAuthorTime
                )
            )
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    @PostMapping("/join/{player}")
    @PreAuthorize("hasRole('SERVER')")
    fun playerJoin(authentication: Authentication, @PathVariable player: String): ResponseEntity<PlayerResponse> {
        try {
            val pair = serverService.playerJoin(authentication.principal as String, player)
            return ResponseEntity.ok(PlayerResponse(pair.first, pair.second))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    @PostMapping("/leave/{player}")
    @PreAuthorize("hasRole('SERVER')")
    fun playerLeave(authentication: Authentication, @PathVariable player: String): ResponseEntity<Unit> {
        if (serverService.playerLeave(authentication.principal as String, player)) {
            try {
                return ResponseEntity.ok(Unit)
            } catch (e: IllegalArgumentException) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
            } catch (e: Exception) {
                throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
            }
        }
        throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Player $player is not on this server")
    }

    @PostMapping("/finished/{player}")
    @PreAuthorize("hasRole('SERVER')")
    fun playerFinished(authentication: Authentication, @PathVariable player: String, @RequestBody request: FinishedRequest): ResponseEntity<FinishedResponse> {
        try {
            return ResponseEntity.ok(serverService.playerFinished(authentication.principal as String, player, request.time, request.local, request.dedi, request.improved))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }

    @PostMapping("/complete-misc/{player}/{quest}")
    @PreAuthorize("hasRole('SERVER')")
    fun completeMisc(authentication: Authentication, @PathVariable player: String, @PathVariable quest: Int): ResponseEntity<FinishedResponse> {
        try {
            return ResponseEntity.ok(serverService.playerCompletedMiscQuest(authentication.principal as String, player, quest))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: Exception) {
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}
