package com.github.pacificengine.migration.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@Service
@Slf4j
public class UpgradeService {
    @Autowired
    private DataSource dataSource;

    @Value("${migration.sql.table}")
    private String migrationTable;

    @Value("${migration.sql.directory}")
    private String migrationDirectory;

    @PostConstruct
    public void init() throws IOException, SQLException {
        var scripts = getScripts();
        var currentVersion = getCurrentVersion();
        var migrationVersion = getMigratedVersion(scripts);
        if (currentVersion < migrationVersion) {
            log.info("Upgrading from " + currentVersion + " to " + migrationVersion);
            performUpgrade(scripts, currentVersion, migrationVersion);
        }
    }

    private void performUpgrade(List<File> scripts, int fromVersion, int toVersion) throws SQLException {
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            connection.beginRequest();
            scripts = scripts.stream()
                    .filter(file -> {
                        var version = Integer.parseInt(file.getName().split("_")[0]);
                        return fromVersion < version && version <= toVersion;
                    }).collect(Collectors.toList());

            for (File file : scripts) {
                try (Statement statement = connection.createStatement()) {
                    var script = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8)
                            .replaceAll("\r", "");
                    statement.execute(script);
                } catch (IOException | SQLException | RuntimeException e) {
                    connection.rollback();
                    throw new RuntimeException("Failed to complete script: " + file.getName(), e);
                }
            }
            var sql = "INSERT INTO " + migrationTable + " (version) VALUES (" + toVersion + ");";
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw new RuntimeException("Failed to complete script: " + sql, e);
            }
            connection.commit();
        }
    }

    private List<File> getScripts() {
        if (ObjectUtils.isEmpty(migrationDirectory)) {
            return Collections.EMPTY_LIST;
        }
        return Stream.of(new File(migrationDirectory).listFiles())
                .filter(Objects::nonNull)
                .filter(file -> !file.isDirectory())
                .filter(file -> file.getName().endsWith(".sql"))
                .filter(file -> file.getName().split("_")[0].matches("[0-9]+"))
                .sorted(Comparator.comparingInt(file -> Integer.parseInt(file.getName().split("_")[0])))
                .collect(Collectors.toList());
    }

    private int getMigratedVersion(List<File> scripts) {
        try {
            if (!ObjectUtils.isEmpty(scripts)) {
                return Integer.parseInt(scripts.get(scripts.size() - 1).getName().split("_")[0]);
            }
            return 0;
        } catch (IndexOutOfBoundsException | NumberFormatException e) {
            return 0;
        }
    }

    public int getCurrentVersion() {
        var sql = "SELECT version FROM " + migrationTable + " ORDER BY version DESC LIMIT 1;";
        try(var connection = dataSource.getConnection()) {
            try (Statement statement = connection.createStatement()) {
                var results = statement.executeQuery(sql);
                if (!results.next()) {
                    return -1;
                }

                return results.getInt(1);
            } catch (SQLException | RuntimeException e) {
                return -1;
            }
        } catch (Exception e) {
            return -1;
        }
    }
}
