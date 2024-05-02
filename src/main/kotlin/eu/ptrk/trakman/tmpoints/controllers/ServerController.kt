package eu.ptrk.trakman.tmpoints.controllers

import eu.ptrk.trakman.tmpoints.*
import eu.ptrk.trakman.tmpoints.services.ServerService
import jakarta.servlet.http.HttpServletResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.DisabledException
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.*
import org.springframework.web.reactive.function.BodyInserters
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodilessEntity
import org.springframework.web.reactive.function.client.awaitBody
import org.springframework.web.server.ResponseStatusException
import org.springframework.web.servlet.view.RedirectView
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*

@Controller
@RequestMapping("/points")
class ServerController(
    private val serverService: ServerService,
    private val tokenService: TokenService,
    @Value("\${eu.ptrk.trakman.ws.name}") private val wsName: String,
    @Value("\${eu.ptrk.trakman.ws.password}") private val wsPass: String,
    @Value("\${spring.application.url}") private val hostUrl: String
) {

    private val client = WebClient.create()
    private val states: MutableMap<String, ServerRegisterRequest> = mutableMapOf()
    private val authorizeUrl = "https://ws.trackmania.com/oauth2/authorize/"
    private val tokenUrl = "https://ws.trackmania.com/oauth2/token/"
    private val responseUrl = "https://players.trackmaniaforever.com"

    @GetMapping("/foobar")
    fun default(): ResponseEntity<TokenResponse> {
        return ResponseEntity.ok(TokenResponse("fak u"))
    }

    @GetMapping("/register")
    fun registerPage(model: Model, response: HttpServletResponse): String {
        response.addHeader("Access-Control-Allow-Origin", responseUrl)
        return "register"
    }

    @CrossOrigin
    @PostMapping("/register")
    suspend fun getTMLogin(@RequestBody request: ServerRegisterRequest): ResponseEntity<TokenResponse> {
        val state = UUID.randomUUID().toString().take(20)
        states[state] = request
        val url = authorizeUrl +
                "?client_id=$wsName" +
                "&redirect_uri=$hostUrl/points/register/finish" +
                "&response_type=code" +
                "&state=$state"
        val response = client.get().uri(url).retrieve().awaitBodilessEntity() ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "$authorizeUrl did not respond")
        if (response.statusCode != HttpStatus.FOUND || response.headers.location == null)
            throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "$authorizeUrl returned code ${response.statusCode}")
        return ResponseEntity.ok(TokenResponse(response.headers.location.toString()))
    }

    @GetMapping("/register/finish")
    suspend fun register(@RequestParam code: String, @RequestParam state: String): RedirectView {
        val finalUrl = "$hostUrl/points/register?registered="
        val request = states[state] ?: return RedirectView("${finalUrl}false&err=nostate&msg=")
        val body = TMOAuthTokenRequest(wsName, wsPass, "$hostUrl/points/register/finish", code)
        val req = client.post().uri(tokenUrl).contentType(MediaType.APPLICATION_JSON).accept(MediaType.APPLICATION_JSON)
            .body(BodyInserters.fromValue(body))
        println(req.toString())
        val response = try { req.retrieve().awaitBody<TMOAuthTokenResponse>() } catch (e: Exception) {
            return RedirectView("${finalUrl}false&err=gateway&msg=${
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(e.message, StandardCharsets.UTF_8.toString())
                }
            }")
        }
        if (request.login != response.login)
            return RedirectView("${finalUrl}false&err=mismatch&msg=")
        try {
            serverService.register(request.login, request.password)
        } catch (e: IllegalArgumentException) {
            return RedirectView("${finalUrl}false&err=badreq&msg=${
                withContext(Dispatchers.IO) {
                    URLEncoder.encode(e.message, StandardCharsets.UTF_8.toString())
                }
            }")
        }
        return RedirectView("${finalUrl}true")
    }

    @PostMapping("/login")
    suspend fun login(@RequestBody request: AddServerRequest): ResponseEntity<TokenPlayersResponse> {
        try {
            val players = serverService.addServer(request.login, request.password, request.currentMapUid, request.currentMapAuthorTime, request.playerLogins)
            return ResponseEntity.ok(TokenPlayersResponse(tokenService.createServerToken(request.login), players.map { PlayerResponse(it.key, it.value) }))
        } catch (e: IllegalArgumentException) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, e.message)
        } catch (e: DisabledException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Disabled")
        } catch (e: BadCredentialsException) {
            throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Bad credentials")
        } catch (e: Exception) {
            println(e.message)
            throw ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
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
