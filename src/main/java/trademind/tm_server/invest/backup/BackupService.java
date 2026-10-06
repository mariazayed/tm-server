package trademind.tm_server.invest.backup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Clock;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

/** Writes the export to a timestamped JSON file and trims old ones. */
@Service
public class BackupService {

    private static final Logger log = LoggerFactory.getLogger(BackupService.class);
    private static final String PREFIX = "invest-backup-";
    private static final String SUFFIX = ".json";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss").withZone(ZoneOffset.UTC);

    public record Result(Path file, long bytes, int pruned) {
    }

    private final ExportService export;
    private final ObjectMapper mapper;
    private final BackupProperties properties;
    private final Clock clock;

    public BackupService(ExportService export, ObjectMapper mapper, BackupProperties properties, Clock clock) {
        this.export = export;
        this.mapper = mapper;
        this.properties = properties;
        this.clock = clock;
    }

    public Result backup() {
        Path dir = Path.of(properties.dir()).toAbsolutePath().normalize();
        String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(export.export());
        byte[] bytes = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Path target = dir.resolve(PREFIX + STAMP.format(clock.instant()) + SUFFIX);
        try {
            Files.createDirectories(dir);
            Path tmp = Files.createTempFile(dir, ".backup-", ".tmp");
            try {
                restrictToOwner(tmp);
                Files.write(tmp, bytes);
                try {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (AtomicMoveNotSupportedException e) {
                    Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(tmp); // only left over if something failed above
            }
            int pruned = prune(dir);
            log.info("Wrote backup {} ({} bytes), pruned {}", target.getFileName(), bytes.length, pruned);
            return new Result(target, bytes.length, pruned);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write the backup to " + dir, e);
        }
    }

    /** Backups hold your notes and reminder address: owner-only where the filesystem allows it. */
    private static void restrictToOwner(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // not a POSIX filesystem (e.g. Windows): rely on the directory's own permissions
        }
    }

    /** Keeps the newest {@code keep} backups. Names sort chronologically, so no timestamps are parsed. */
    private int prune(Path dir) throws IOException {
        List<Path> backups;
        try (Stream<Path> files = Files.list(dir)) {
            backups = files.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith(PREFIX)
                            && p.getFileName().toString().endsWith(SUFFIX))
                    .sorted().toList();
        }
        int excess = Math.max(0, backups.size() - properties.keep());
        for (int i = 0; i < excess; i++) {
            Files.deleteIfExists(backups.get(i));
        }
        return excess;
    }
}
