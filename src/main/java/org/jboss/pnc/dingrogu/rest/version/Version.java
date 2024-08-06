package org.jboss.pnc.dingrogu.rest.version;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.pnc.api.dto.ComponentVersion;
import org.jboss.pnc.dingrogu.Constants;

import java.time.ZonedDateTime;

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
