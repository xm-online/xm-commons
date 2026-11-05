package com.icthh.xm.commons.config.client.applied;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.Aggregate;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.FetchScope;
import com.icthh.xm.commons.config.client.applied.AppliedConfigService.PathHash;

import java.util.List;
import java.util.Map;

/**
 * Response of {@code GET /api/applied-config}. {@code total} is for the management role only,
 * {@code configurationsHashSum} only with {@code details=true}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AppliedConfigResponse(AppliedConfigVersion commit,
                                    FetchScope fetchScope,
                                    Map<String, Aggregate> hashes,
                                    Aggregate total,
                                    List<PathHash> configurationsHashSum) {
}
