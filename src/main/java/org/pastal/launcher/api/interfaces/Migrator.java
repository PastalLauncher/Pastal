package org.pastal.launcher.api.interfaces;

import java.util.List;

public interface Migrator {
    String getName();

    List<Importable> findData();

    boolean hasData();
}
