package trademind.tm_server.invest.backup;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BackupServiceTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** A clock the test can move so each backup gets a different name. */
    private static final class MovableClock extends Clock {
        Instant now = Instant.parse("2026-10-06T02:30:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    private static ExportData sample(String note) {
        return new ExportData(ExportData.SCHEMA_VERSION, Instant.parse("2026-10-06T02:30:00Z"),
                new ExportData.Settings(30000, "USD", 1, "VT", LocalDate.parse("2026-08-01"), "UTC", true,
                        "me@example.com", 3, 72),
                List.of(new ExportData.Contribution(1L, LocalDate.parse("2026-10-01"), 30000, new BigDecimal("2.55489022"),
                        new BigDecimal("117.4250"), 0, note, LocalDateTime.parse("2026-10-01T09:00:00"), "VT")),
                List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(new ExportData.Holding(1L, "VT", "Total world fund", "EQUITY", "Global", new BigDecimal("100"), null, null)));
    }

    private static BackupService service(Path dir, int keep, MovableClock clock, ExportData data) {
        ExportService export = mock(ExportService.class);
        when(export.export()).thenReturn(data);
        return new BackupService(export, MAPPER, new BackupProperties(true, dir.toString(), keep, "0 30 2 * * *"), clock);
    }

    private static List<String> names(Path dir) throws Exception {
        try (Stream<Path> files = Files.list(dir)) {
            return files.map(p -> p.getFileName().toString()).sorted().toList();
        }
    }

    @Test
    void writesAReadableJsonFileNamedByTime(@TempDir Path dir) throws Exception {
        BackupService.Result result = service(dir, 5, new MovableClock(), sample("first")).backup();

        assertEquals("invest-backup-20261006-023000.json", result.file().getFileName().toString());
        assertEquals(Files.size(result.file()), result.bytes());

        JsonNode json = MAPPER.readTree(Files.readString(result.file()));
        assertEquals(1, json.path("schemaVersion").asInt());
        assertEquals(30000, json.path("contributions").path(0).path("amountCents").asInt());
        assertEquals("2.55489022", json.path("contributions").path(0).path("shares").asString());
        assertEquals("first", json.path("contributions").path(0).path("notes").asString());
        assertEquals("VT", json.path("contributions").path(0).path("ticker").asString());
        assertEquals("VT", json.path("holdings").path(0).path("ticker").asString());
        assertEquals("me@example.com", json.path("settings").path("reminderEmail").asString());
    }

    @Test
    void theFileIsPrivateToTheOwnerWhereTheFilesystemSupportsIt(@TempDir Path dir) throws Exception {
        Path file = service(dir, 5, new MovableClock(), sample("x")).backup().file();
        if (Files.getFileStore(file).supportsFileAttributeView("posix")) {
            Set<PosixFilePermission> perms = Files.getPosixFilePermissions(file);
            assertEquals(Set.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE), perms);
        }
    }

    @Test
    void createsTheDirectoryAndLeavesNoTemporaryFiles(@TempDir Path root) throws Exception {
        Path dir = root.resolve("nested").resolve("backups");
        service(dir, 5, new MovableClock(), sample("x")).backup();
        List<String> files = names(dir);
        assertEquals(1, files.size());
        assertFalse(files.get(0).endsWith(".tmp"));
        assertFalse(files.get(0).startsWith(".backup-"));
    }

    @Test
    void keepsOnlyTheNewestBackupsAndNeverTouchesOtherFiles(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("notes.txt"), "mine");
        Files.writeString(dir.resolve("invest-backup-old.txt"), "not a backup file name"); // wrong suffix
        MovableClock clock = new MovableClock();
        BackupService service = service(dir, 3, clock, sample("x"));

        int lastPruned = 0;
        for (int day = 0; day < 5; day++) {
            clock.now = Instant.parse("2026-10-0" + (1 + day) + "T02:30:00Z");
            lastPruned = service.backup().pruned();
        }

        assertEquals(1, lastPruned);
        assertEquals(List.of("invest-backup-20261003-023000.json", "invest-backup-20261004-023000.json",
                "invest-backup-20261005-023000.json", "invest-backup-old.txt", "notes.txt"), names(dir));
    }

    @Test
    void twoBackupsInTheSameSecondDoNotCorruptEachOther(@TempDir Path dir) throws Exception {
        BackupService service = service(dir, 5, new MovableClock(), sample("x"));
        service.backup();
        service.backup(); // same timestamp: replaced atomically, still valid
        List<String> files = names(dir);
        assertEquals(1, files.size());
        assertTrue(MAPPER.readTree(Files.readString(dir.resolve(files.get(0)))).has("settings"));
    }

    @Test
    void anUnwritableLocationFailsLoudlyWithoutPartialFiles(@TempDir Path root) throws Exception {
        Path notADirectory = root.resolve("file.txt");
        Files.writeString(notADirectory, "I am a file");
        BackupService service = service(notADirectory, 3, new MovableClock(), sample("x"));
        assertThrows(UncheckedIOException.class, service::backup);
        assertEquals(List.of("file.txt"), names(root));
    }

    @Test
    void theRetentionSettingIsValidated() {
        assertThrows(IllegalArgumentException.class, () -> new BackupProperties(true, "backups", 0, "0 30 2 * * *"));
        assertThrows(IllegalArgumentException.class, () -> new BackupProperties(true, "backups", 5000, "0 30 2 * * *"));
    }
}
