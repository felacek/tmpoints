package eu.ptrk.trakman.tmpoints.services

import eu.ptrk.trakman.tmpoints.Admin
import eu.ptrk.trakman.tmpoints.AdminRepository
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

@Service
class AdminService(private val adminRepository: AdminRepository, private val authenticationManager: AuthenticationManager, private val encoder: PasswordEncoder) {

    fun login(login: String, password: String): String {
        val admin = adminRepository.getAdminByLogin(login) ?: throw BadCredentialsException("Bad credentials")
        val authenticationRequest = UsernamePasswordAuthenticationToken.unauthenticated(login, password)
        authenticationManager.authenticate(authenticationRequest)
        return admin.login
    }

    fun register(login: String, password: String): String {
        val exists = adminRepository.getAdminByLogin(login)
        if (exists != null) return login(login, password)
        if (login.length > 20) throw IllegalArgumentException("Login cannot exceed 20 characters")
        return adminRepository.save(Admin(login, encoder.encode(password))).login
    }
}