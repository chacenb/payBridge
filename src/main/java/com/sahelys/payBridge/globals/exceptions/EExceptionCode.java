package com.sahelys.payBridge.globals.exceptions;

import lombok.Getter;

import java.util.Arrays;

@Getter public enum EExceptionCode {

  DEBUGGING("DEBUGGING"),
  RUNTINE_EXCEPTION("RUNTINE_EXCEPTION"),
  MISSING_REQUIRED_FIELD("MISSING_REQUIRED_FIELD"),
  DATA_INCOHERENCE("DATA_INCOHERENCE"),
  TIMEOUT_ERROR("TIMEOUT_ERROR"),
  ENUM_MISMATCH("ENUM_MISMATCH"),
  WRONG_PWD("WRONG_PWD"),
  USER_DEACTIVATED("USER_DEACTIVATED"),
  ENTITY_NOT_FOUND("ENTITY_NOT_FOUND"),
  HAS_DEPENDENCIES("HAS_DEPENDENCIES"),
  WRONG_DELETED_ENTITY("WRONG_DELETED_ENTITY"),
  DTO_CONVERSION_ERROR("DTO_CONVERSION_ERROR"),
  DISABLED_FEATURE("DISABLED_FEATURE"),

  ;

  public final String label;

  EExceptionCode(String label) {
    this.label = label;
  }

  /**
   * Return an Enum EEtatStatut from a string value
   *
   * @param str a string value from which we want to retrieve the Enum value
   * @return An instance of the enum EEtatStatut
   * @throws CustomException if the provided string doexnt match any EEtatStatut entry
   */
  public static EExceptionCode from(String str) {
    return Arrays.stream(values()).filter(it -> it.label.equals(str)).findAny()
                 .orElseThrow(() -> new CustomException(EExceptionCode.ENUM_MISMATCH, "Aucune Enumeration [EExceptionCode] ne correspond à la chaine " + str));
  }

}
