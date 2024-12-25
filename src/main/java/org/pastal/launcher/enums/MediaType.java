package org.pastal.launcher.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MediaType {
    JSON("application/json"),
    HTML("text/html"),
    PLAIN_TEXT("text/plain"),
    XML("application/xml");


    private final String type;
}
