package org.pastal.launcher.http.implement.api.v1;

import org.pastal.launcher.http.annotations.RequestMapping;
import org.pastal.launcher.http.annotations.RestController;

@RestController("/api/v1")
public class StatusController {
    @RequestMapping(path = "/status")
    public String status(){
        return "ok";
    }
}
