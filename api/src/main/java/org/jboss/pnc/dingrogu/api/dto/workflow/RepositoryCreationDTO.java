package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepositoryCreationDTO {

    public String repourUrl;
    public String externalRepoUrl;
    public String ref;

}
