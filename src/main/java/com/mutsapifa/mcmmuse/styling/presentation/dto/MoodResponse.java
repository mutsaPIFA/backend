package com.mutsapifa.mcmmuse.styling.presentation.dto;

import com.mutsapifa.mcmmuse.styling.domain.Mood;

/** 계약 §4-3 — {id, label, labelEn, iconKey} */
public record MoodResponse(Long id, String label, String labelEn, String iconKey) {

  public static MoodResponse from(Mood mood) {
    return new MoodResponse(mood.getId(), mood.getLabel(), mood.getLabelEn(), mood.getIconKey());
  }
}
