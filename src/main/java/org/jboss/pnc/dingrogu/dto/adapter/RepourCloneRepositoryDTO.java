package org.jboss.pnc.dingrogu.dto.adapter;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class RepourCloneRepositoryDTO {

    String repourUrl;
    String externalUrl;
    String ref;
}
