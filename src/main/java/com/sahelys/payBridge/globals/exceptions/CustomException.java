package com.sahelys.payBridge.globals.exceptions;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CustomException extends RuntimeException {

  private EExceptionCode code;

  public CustomException() {
    super();
  }

  public CustomException(EExceptionCode code) {
    this.code = code;
  }

  public CustomException(EExceptionCode code, String message) {
    super(message);
    this.code = code;
  }

  public CustomException(String message, Throwable cause) {
    super(message, cause);
  }

  public CustomException(EExceptionCode code, Throwable cause) {
    super(code.getLabel(), cause);
    this.code = code;
  }

  public CustomException(Throwable cause) {
    super(cause);
  }

  protected CustomException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
