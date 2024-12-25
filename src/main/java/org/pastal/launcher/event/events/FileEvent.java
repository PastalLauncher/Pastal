package org.pastal.launcher.event.events;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.pastal.launcher.enums.FileAction;
import org.pastal.launcher.event.impl.Event;

import java.io.File;

@AllArgsConstructor
@Getter
public class FileEvent implements Event {
    private final FileAction fileAction;
    private final File file;
}
