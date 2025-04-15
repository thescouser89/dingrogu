package org.jboss.pnc.dingrogu.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ConstantsTest {

    @Test
    void checkIfAllVariablesSubstituted() {
        assertThat(Constants.BUILD_TIME).doesNotContain("@");
        assertThat(Constants.COMMIT_HASH).doesNotContain("@");
        assertThat(Constants.DINGROGU_VERSION).doesNotContain("@");
    }

}