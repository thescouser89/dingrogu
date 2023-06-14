package org.jboss.pnc.dingrogu.workflows;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.pnc.api.repour.dto.RepourCloneCallback;
import org.jboss.pnc.api.repour.dto.RepourCloneRepositoryRequest;
import org.jboss.pnc.api.repour.dto.RepourCreateRepositoryRequest;
import org.jboss.pnc.dingrogu.common.GitUrlParser;

import java.util.Collections;

@ApplicationScoped
public class RepositoryCreation {

    public RepourCreateRepositoryRequest getRepourCreateInternalRepository(String externalUrl) {

        return RepourCreateRepositoryRequest.builder()
                .project(GitUrlParser.generateInternalGitRepoName(externalUrl))
                .ownerGroups(Collections.singletonList("ldap/jboss-prod"))
                .parentProject("jboss-prod-permissions")
                .build();
    }

    public RepourCloneRepositoryRequest getRepourCloneRepository(String externalUrl, String readWriteUrl, String ref) {
        // RepourCloneCallback callback = RepourCloneCallback
        // .builder()
        // .url(systemVariables.getCallbackUrl())
        // .method(systemVariables.getCallbackMethod())
        // .build();

        // TODO: the way rex does callback is different. handle this
        return RepourCloneRepositoryRequest.builder()
                .type("git")
                .originRepoUrl(externalUrl)
                .targetRepoUrl(readWriteUrl)
                .ref(ref)
                // .callback(callback)
                .build();
    }
}
