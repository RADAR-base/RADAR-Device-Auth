package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.doa.RestSourceUserRepository
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

@Path("health")
@Resource
@Singleton
class HealthCheckResource(
    @Context private var userRepository: RestSourceUserRepository
) {
    @GET
    fun check(): Response {
        val status = try {
            userRepository.query(Page(0, 1))
            HealthStatus("UP")
        } catch (ex: Throwable) {
            HealthStatus("DOWN")
        }
        return Response.ok(status).build()
    }
    data class HealthStatus(val status: String)
}
