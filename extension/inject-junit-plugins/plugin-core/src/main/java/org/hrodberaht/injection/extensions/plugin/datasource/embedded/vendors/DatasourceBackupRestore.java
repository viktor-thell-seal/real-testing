package org.hrodberaht.injection.extensions.plugin.datasource.embedded.vendors;

public interface DatasourceBackupRestore {
    void createSnapshot(String name);
    void loadSnapshot(String name);

    String jdbcUrl();
}
