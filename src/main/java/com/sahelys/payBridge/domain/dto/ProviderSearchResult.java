package com.sahelys.payBridge.domain.dto;

import lombok.*;

/**
 * What {@code SearchTransactionByExtID}'s real captured response actually carries -- see
 * {@code ProviderXmlParser#parseMoovSearchTransactionResult}. Deliberately has no
 * success/failure field: the one captured sample only confirms the search found a match and
 * when it completed, never the underlying transaction's outcome. Do not add one without a real
 * captured sample to back it -- see that method's own comment.
 */
@Getter @Setter @ToString @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderSearchResult {
    private boolean found;
    private String  completedAt;
    private String  message;
}
