package org.pastal.launcher.components.file;

import org.pastal.launcher.Launcher;
import org.pastal.launcher.api.interfaces.Component;
import org.pastal.launcher.enums.FileAction;
import org.pastal.launcher.event.annotations.EventTarget;
import org.pastal.launcher.event.events.FileEvent;
import org.pastal.launcher.event.events.ShutdownEvent;
import org.pastal.launcher.managers.ComponentManager;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


public class FileListenerComponent implements Component {
    private final Timer timer = new Timer();

    private final ConcurrentHashMap<File, FileListenerEntry> entries = new ConcurrentHashMap<>();

    public FileListenerComponent() {
        TimerTask task = new TimerTask() {
            @Override
            public void run() {
                onUpdate();
            }
        };
        timer.scheduleAtFixedRate(task, 0, 1000);
        Launcher.getInstance().getEventManager().register(this);
    }

    @EventTarget
    public void onShutdown(ShutdownEvent event) {
        timer.cancel();
    }

    public boolean addListener(File file) {
        return getEntry(file) != null;
    }

    public boolean removeListener(File file) {
        return this.entries.remove(file) != null;
    }

    public void onUpdate() {
        Iterator<Map.Entry<File, FileListenerEntry>> iter = this.entries.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<File, FileListenerEntry> entry = iter.next();
            File file = entry.getKey();
            FileListenerEntry listenerEntry = entry.getValue();

            if (file.isDirectory()) {
                monitorDirectory(file, listenerEntry);
            } else {
                monitorFile(file, listenerEntry);
            }
        }
    }

    private void monitorFile(File file, FileListenerEntry listenerEntry) {
        if (file.exists() && file.exists() ^ listenerEntry.exists) {
            Launcher.getInstance().getEventManager().call(new FileEvent(FileAction.CREATED, file));
            listenerEntry.exists = true;
            listenerEntry.timestamp = file.lastModified();
        } else if (file.exists() && file.lastModified() != listenerEntry.timestamp) {
            Launcher.getInstance().getEventManager().call(new FileEvent(FileAction.CHANGED, file));
            listenerEntry.timestamp = file.lastModified();
        } else if (!file.exists() && listenerEntry.exists) {
            Launcher.getInstance().getEventManager().call(new FileEvent(FileAction.DELETED, file));
            listenerEntry.exists = false;
        }
    }

    private void monitorDirectory(File directory, FileListenerEntry listenerEntry) {
        for (File childFile : Objects.requireNonNull(directory.listFiles())) {
            if (!listenerEntry.children.containsKey(childFile.getAbsolutePath())) {
                listenerEntry.children.put(childFile.getAbsolutePath(), new FileListenerEntry(childFile));
                Launcher.getInstance().getEventManager().call(new FileEvent(FileAction.CREATED, childFile));
            }
        }

        Iterator<Map.Entry<String, FileListenerEntry>> childIter = listenerEntry.children.entrySet().iterator();
        while (childIter.hasNext()) {
            Map.Entry<String, FileListenerEntry> childEntry = childIter.next();
            File childFile = new File(childEntry.getKey());
            if (!childFile.exists()) {
                Launcher.getInstance().getEventManager().call(new FileEvent(FileAction.DELETED, childFile));
                childIter.remove();
            }
        }

        for (Map.Entry<String, FileListenerEntry> childEntry : listenerEntry.children.entrySet()) {
            monitorFile(new File(childEntry.getKey()), childEntry.getValue());
        }
    }

    private FileListenerEntry getEntry(File file) {
        return this.entries.computeIfAbsent(file, FileListenerEntry::new);
    }

    @Override
    public void setup(ComponentManager componentManager) {

    }

    static class FileListenerEntry {
        final File file;
        final Map<String, FileListenerEntry> children = new HashMap<>();
        long timestamp = 0L;
        boolean exists;

        FileListenerEntry(File file) {
            this.file = file;
            this.exists = file.exists();
            if (this.exists) {
                this.timestamp = file.lastModified();
            }
        }
    }
}
