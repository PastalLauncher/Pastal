package org.pastal.launcher.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ClientIdentification {
    STYLES("2289e44b-73ce-4577-bbd0-bb7f3c1a27cb", "XboxLive.signin offline_access"),
    OFFICIAL("00000000402b5328", "service::user.auth.xboxlive.com::MBI_SSL"),
    HMCL("6a3728d6-27a3-4180-99bb-479895b8f88e", "XboxLive.signin offline_access"),
    PCL("fe72edc2-3a6f-4280-90e8-e2beb64ce7e1", "XboxLive.signin offline_access");

    private final String clientId;
    private final String scope;
}
