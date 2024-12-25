package org.pastal.launcher.api.objects;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@AllArgsConstructor
@Getter
@ToString
public class AddonVersion {
    private String name;
    private String url;
    private String altUrl;
}
