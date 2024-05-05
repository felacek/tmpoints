package eu.ptrk.trakman.tmpoints

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.security.Security

@SpringBootApplication
class PointsApplication

fun main(args: Array<String>) {
    val algos = Security.getProperty("jdk.tls.disabledAlgorithms").replace("TLSv1, ", "")
    Security.setProperty("jdk.tls.disabledAlgorithms", algos)
    runApplication<PointsApplication>(*args)
}
