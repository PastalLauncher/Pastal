package org.pastal.launcher.managers;

import org.pastal.launcher.migrators.HMCLMigrator;
import org.pastal.launcher.migrators.MCLMigrator;
import org.pastal.launcher.migrators.PCLMigrator;
import org.pastal.launcher.migrators.UMCLMigrator;
import org.pastal.launcher.api.ImmutableManager;
import org.pastal.launcher.api.interfaces.Migrator;

import java.util.ArrayList;
import java.util.List;

public class MigratManager extends ImmutableManager<Migrator> {
    public MigratManager() {
        registerElements(HMCLMigrator.class, MCLMigrator.class, PCLMigrator.class, UMCLMigrator.class);
    }

    public List<Migrator> getAvailableAdapters() {
        List<Migrator> migrators = new ArrayList<>(getElements());
        migrators.removeIf(migrator -> !migrator.hasData());
        return migrators;
    }
}
