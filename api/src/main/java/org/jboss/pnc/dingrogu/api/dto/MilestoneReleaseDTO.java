package org.jboss.pnc.dingrogu.api.dto;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

@Data
@Jacksonized
@Builder
public class MilestoneReleaseDTO {

    public String milestoneId;
}
