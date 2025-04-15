package org.jboss.pnc.dingrogu.restworkflow.rest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

import org.jboss.pnc.dingrogu.common.Constants;
import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
class VersionTest {

    @Test
    public void testVersionEndpoint() {
        given().when().get("/version").then().statusCode(200).body(containsString(Constants.DINGROGU_VERSION));
    }
}