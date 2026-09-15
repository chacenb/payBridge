package com.sahelys.payBridge.domain.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.time.ZonedDateTime;
import java.util.Map;

@Getter @Setter @Builder
public class WsResponse<T> {
    protected ZonedDateTime       timeStamp;
    private   HttpStatus          status;
    private   String              message;
    private   T                   data;
    private   Map<String, Object> extra;
    private   Throwable           error;
    private   String              errorCode;
    private   Integer             total;
    private   Integer             page;
}
