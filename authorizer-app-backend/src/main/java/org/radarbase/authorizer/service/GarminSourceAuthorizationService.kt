/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer.service

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.glassfish.jersey.process.internal.RequestScope
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestOauth1AccessToken
import org.radarbase.authorizer.api.RestOauth1UserId
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*
import javax.ws.rs.core.Context

class GarminSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val requestScope: RequestScope
): OAuth1RestSourceAuthorizationService(restSourceClients, httpClient, objectMapper) {
    val GARMIN_USER_ID_ENDPOINT = "https://healthapi.garmin.com/wellness-api/rest/user/id"
    val DEREGISTER_CHECK_PERIOD = 5000L

    init {
        // This schedules a task that periodically checks users with elapsed end dates and deregisters them.
        val task = object : TimerTask() {
            override fun run() = checkForUsersWithElapsedEndDateAndDeregister()
        }
        Timer().scheduleAtFixedRate(task, 0, DEREGISTER_CHECK_PERIOD)
    }

    override fun getExternalId(tokens: RestOauth1AccessToken, sourceType: String): String? {
        // Garmin does not provide the service/external id with the token payload, so an additional
        // request to pull the external id is needed.
        val req = createRequest("GET", GARMIN_USER_ID_ENDPOINT, tokens, sourceType)
        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> response.body?.byteStream()
                        ?.let {  objectMapper.readerFor(RestOauth1UserId::class.java).readValue<RestOauth1UserId>(it).userId }
                        ?: throw HttpBadGatewayException("Service did not provide a result")
                400, 401, 403 -> throw HttpBadGatewayException("Service was unable to fetch the external ID")
                else -> throw HttpBadGatewayException("Cannot connect to ${GARMIN_USER_ID_ENDPOINT}: HTTP status ${response.code}")
            }
        }
    }

    fun checkForUsersWithElapsedEndDateAndDeregister() {
        requestScope.runInScope(Runnable {
            val users = userRepository.queryAllWithElapsedEndDate(GARMIN_AUTH)
            users.forEach { user -> revokeToken(user) }
            logger.info(users.toString())
        })
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(GarminSourceAuthorizationService::class.java)
    }

}
