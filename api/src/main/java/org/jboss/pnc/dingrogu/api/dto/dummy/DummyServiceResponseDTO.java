package org.jboss.pnc.dingrogu.api.dto.dummy;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Jacksonized
@Data
@Builder
public class DummyServiceResponseDTO {
    public String status;
}
