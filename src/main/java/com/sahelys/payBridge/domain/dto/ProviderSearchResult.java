package com.sahelys.payBridge.domain.dto;

import lombok.*;

/**
 * What {@code SearchTransactionByExtID}'s real captured response actually carries -- see
 * {@code ProviderXmlParser#parseMoovSearchTransactionResult}. Deliberately a plain relay of the
 * raw fields (no success/failure field, no derived "found" flag): the one captured sample only
 * confirms a result code/description for the search itself and when the transaction completed,
 * never the underlying transaction's outcome. Do not add an interpreted field to this without a
 * real captured sample to back it -- see that method's own comment.
 */
@Getter @Setter @ToString @Builder @NoArgsConstructor @AllArgsConstructor
public class ProviderSearchResult {
    private String resultCode;
    private String resultDesc;
    private String completedAt;
}
