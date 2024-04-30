package eu.ptrk.trakman.tmpoints

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.ProviderManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.authentication.dao.DaoAuthenticationProvider
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken
import org.springframework.security.web.SecurityFilterChain
import org.springframework.stereotype.Service
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val tokenService: TokenService,
    private val serverRepository: ServerRepository,
    private val adminRepository: AdminRepository
) {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http.authorizeHttpRequests {
            it.requestMatchers(HttpMethod.POST, "/server/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/admin/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/admin/login").permitAll()
                .anyRequest().authenticated()
        }//.httpBasic(Customizer.withDefaults())

        http.oauth2ResourceServer {
            it.jwt { }
        }

        http.authenticationManager {
            val jwt = it as BearerTokenAuthenticationToken
            val server = tokenService.parseServerToken(jwt.token)
            if (server != null)
                UsernamePasswordAuthenticationToken(server, "", listOf(SimpleGrantedAuthority("SERVER")))
            else {
                val admin = tokenService.parseAdminToken(jwt.token) ?: throw InvalidBearerTokenException("Invalid token")
                UsernamePasswordAuthenticationToken(admin, "", listOf(SimpleGrantedAuthority("ADMIN")))
            }
        }

        http.cors {}

        http.sessionManagement {
            it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        }

        http.csrf {
            it.disable()
        }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = listOf("http://localhost")
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "DELETE")
        configuration.allowedHeaders = listOf("authorization", "content-type")

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun authenticationManager(passwordEncoder: PasswordEncoder): AuthenticationManager {
        val authenticationProvider = DaoAuthenticationProvider()
        authenticationProvider.setUserDetailsService(PointsUserDetailsService(serverRepository, adminRepository))
        authenticationProvider.setPasswordEncoder(passwordEncoder)

        return ProviderManager(authenticationProvider)
    }


    @Bean
    fun passwordEncoder(): PasswordEncoder {
        return BCryptPasswordEncoder(4)
    }
}

class PointsUserDetailsService(private val serverRepository: ServerRepository, private val adminRepository: AdminRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String): UserDetails {
        val server = serverRepository.getServerByLogin(username)
        if (server != null) {
            return User(server.login, server.password, listOf(SimpleGrantedAuthority("SERVER")))
        }
        val admin = adminRepository.getAdminByLogin(username) ?: throw UsernameNotFoundException("No such username")
        return User(admin.login, admin.password, listOf(SimpleGrantedAuthority("ADMIN")))
    }
}

@Configuration
class JwtEncodingConfig(@Value("\${security.key}") private val jwtKey: String) {
    private val secretKey = SecretKeySpec(jwtKey.toByteArray(), "HmacSHA256")

    @Bean
    fun jwtDecoder(): JwtDecoder {
        return NimbusJwtDecoder.withSecretKey(secretKey).build()
    }

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val secret = ImmutableSecret<SecurityContext>(secretKey)
        return NimbusJwtEncoder(secret)
    }
}

@Service
class TokenService(
    private val jwtDecoder: JwtDecoder,
    private val jwtEncoder: JwtEncoder
) {

    fun createServerToken(server: String): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(10L, ChronoUnit.DAYS))
            .subject(server)
            .claim("server", server)
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    fun parseServerToken(token: String): String? {
        try {
            val jwt = jwtDecoder.decode(token)
            return jwt.claims["server"] as String
        } catch (e: Exception) {
            return null
        }
    }

    fun createAdminToken(admin: String): String {
        val jwsHeader = JwsHeader.with { "HS256" }.build()
        val claims = JwtClaimsSet.builder()
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plus(10L, ChronoUnit.DAYS))
            .subject(admin)
            .claim("admin", admin)
            .build()
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader, claims)).tokenValue
    }

    fun parseAdminToken(token: String): String? {
        try {
            val jwt = jwtDecoder.decode(token)
            return jwt.claims["admin"] as String
        } catch (e: Exception) {
            return null
        }
    }
}