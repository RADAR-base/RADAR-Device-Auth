/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.webapp.resource;

import java.net.URI;
import java.net.URISyntaxException;
import javax.validation.Valid;

import org.radarbase.authorizer.service.RestSourceUserService;
import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.RestSourceUsers;
import org.radarbase.authorizer.service.dto.TokenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestSourceUserResource {

    private Logger logger = LoggerFactory.getLogger(RestSourceUserResource.class);

    @Autowired
    private RestSourceUserService restSourceUserService;

    @PostMapping("/users")
    public ResponseEntity addAuthorizedRestSourceUser(@RequestParam(value = "code") String code,
            @RequestParam(value = "state") String state) throws URISyntaxException {
        logger.debug("Add a rest-source user with code {} and state {}", code, state);
        RestSourceUserPropertiesDTO
                user = this.restSourceUserService.authorizeAndStoreDevice(code, state);
        return ResponseEntity
                .created(new URI("/user/" + user.getId())).body(user);
    }

    @GetMapping("/users")
    public ResponseEntity<RestSourceUsers> getAllRestSources(
            @RequestParam(value = "source-type", required = false) String sourceType) {
        if (sourceType != null && !sourceType.isEmpty()) {
            logger.debug("Get all rest source users by type {}", sourceType);
            return ResponseEntity
                    .ok(this.restSourceUserService.getAllUsersBySourceType(sourceType));
        }

        logger.debug("Get all rest source users");
        return ResponseEntity
                .ok(this.restSourceUserService.getAllRestSourceUsers());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<RestSourceUserPropertiesDTO> getRestSourceUserById(
            @PathVariable String id) {
        logger.debug("Get rest source user with id {}", id);
        return ResponseEntity
                .ok(this.restSourceUserService.getRestSourceUserById(Long.valueOf(id)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity updateDeviceUser(@Valid @PathVariable String id,
            @RequestBody RestSourceUserPropertiesDTO restSourceUser) {
        logger.debug("Requesting to update rest source user");
        return ResponseEntity
                .ok(this.restSourceUserService.updateRestSourceUser(Long.valueOf(id), restSourceUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteDeviceUser(@Valid @PathVariable String id) {
        logger.debug("Requesting to delete rest source user");
        this.restSourceUserService.revokeTokenAndDeleteUser(Long.valueOf(id));
        return ResponseEntity
                .ok().header("user-removed", id).build();
    }


    @GetMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> getUserToken(@PathVariable String id) {
        logger.debug("Get user token for rest source user id {}", id);
        return ResponseEntity
                .ok(this.restSourceUserService.getDeviceTokenByUserId(Long.valueOf(id)));
    }

    @PostMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> requestRefreshTokenForUser(@PathVariable String id) {
        logger.debug("Refreshing user token for rest source user id {}", id);
        return ResponseEntity
                .ok(this.restSourceUserService.refreshTokenForUser(Long.valueOf(id)));
    }
}