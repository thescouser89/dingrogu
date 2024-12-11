package org.jboss.pnc.dingrogu.restworkflow.rest;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.dingrogu.common.Constants;

import java.time.ZonedDateTime;

/**
 * Version endpoint I wasn't sure whether to put it here or in the rest-adapter module so it just stays here for now!
 */
@Path("/version")
public class Version {

    @ConfigProperty(name = "quarkus.application.name")
    String name;

    @GET
    public ComponentVersion getVersion() {
        return ComponentVersion.builder()
                .name(name)
                .version(Constants.DINGROGU_VERSION)
                .commit(Constants.COMMIT_HASH)
                .builtOn(ZonedDateTime.parse(Constants.BUILD_TIME))
                .build();
    }

}
