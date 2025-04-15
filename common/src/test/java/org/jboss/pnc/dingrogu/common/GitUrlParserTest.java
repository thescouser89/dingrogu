package org.jboss.pnc.dingrogu.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class GitUrlParserTest {

    @Test
    void generateInternalGitRepoName() {
        String gitRepository = "git@github.com:project-ncl/dingrogu";
        assertThat(GitUrlParser.generateInternalGitRepoName(gitRepository)).isEqualTo("project-ncl/dingrogu");

        String gitRepository2 = "https://github.com/project-ncl/dingrogu.git";
        assertThat(GitUrlParser.generateInternalGitRepoName(gitRepository2)).isEqualTo("project-ncl/dingrogu");

        String badGitRepository = "hellohub";
        assertThat(GitUrlParser.generateInternalGitRepoName(badGitRepository)).isNull();
    }

    @Test
    void scmRepoURLReadOnly() {
        String readwriteUrlGitlab = "git@gitlab.com:project-ncl/bpm.git";
        String readonlyUrlGitlab = "https://gitlab.com/project-ncl/bpm.git";
        assertThat(GitUrlParser.scmRepoURLReadOnly(readwriteUrlGitlab)).isEqualTo(readonlyUrlGitlab);

        String badReadwriteUrlGitlab = "safdltgitlacomproject-ncl";
        assertThat(GitUrlParser.scmRepoURLReadOnly(badReadwriteUrlGitlab)).isNull();
    }
}