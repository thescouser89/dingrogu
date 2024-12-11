package org.jboss.pnc.dingrogu.api.dto.workflow;

import lombok.Builder;
import lombok.Data;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Data
@Jacksonized
@Builder
public class BrewPushDTO {

    public List<String> buildIds;
}
