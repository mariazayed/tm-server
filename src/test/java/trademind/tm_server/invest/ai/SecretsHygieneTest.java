package trademind.tm_server.invest.ai;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cheap static guards for the "the API key stays on the server" promise: no logging statement mentions a
 * secret, and no key-shaped string is committed outside the tests that use fake ones.
 */
class SecretsHygieneTest {

    private static final Pattern LOG_LINE = Pattern.compile(
            "(log|logger|LOG)\\.(trace|debug|info|warn|error)\\(.*(apiKey|api-key|api_key|password|passwordHash|sessionSecret|secret)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern KEY_SHAPE = Pattern.compile("sk-ant-[A-Za-z0-9_-]{10,}");

    private static List<Path> files(String root, String... extensions) throws IOException {
        List<Path> out = new ArrayList<>();
        Path dir = Path.of(root);
        if (!Files.exists(dir)) return out;
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).filter(p -> {
                String name = p.getFileName().toString();
                for (String ext : extensions) if (name.endsWith(ext)) return true;
                return false;
            }).forEach(out::add);
        }
        return out;
    }

    @Test
    void noLogStatementMentionsASecret() throws IOException {
        List<String> offenders = new ArrayList<>();
        for (Path file : files("src/main/java", ".java")) {
            List<String> lines = Files.readAllLines(file);
            for (int i = 0; i < lines.size(); i++) {
                if (LOG_LINE.matcher(lines.get(i)).find()) offenders.add(file + ":" + (i + 1) + " " + lines.get(i).strip());
            }
        }
        assertTrue(offenders.isEmpty(), "logging a secret: " + offenders);
    }

    @Test
    void noKeyShapedValueIsCommittedOutsideTests() throws IOException {
        List<String> offenders = new ArrayList<>();
        List<Path> candidates = new ArrayList<>();
        candidates.addAll(files("src/main", ".java", ".properties", ".example", ".sql", ".yml", ".yaml"));
        candidates.addAll(files("docs", ".md"));
        candidates.addAll(files("scripts", ".sh"));
        candidates.add(Path.of("pom.xml"));
        for (Path file : candidates) {
            if (!Files.exists(file)) continue;
            String text = Files.readString(file);
            if (KEY_SHAPE.matcher(text).find()) offenders.add(file.toString());
        }
        assertTrue(offenders.isEmpty(), "a real-looking API key is committed in: " + offenders);
    }

    @Test
    void theCommittedPropertiesNeverSetTheKey() throws IOException {
        for (Path file : files("src/main/resources", ".properties", ".example")) {
            for (String line : Files.readAllLines(file)) {
                String t = line.strip();
                if (t.startsWith("#") || t.isEmpty()) continue;
                assertTrue(!t.startsWith("ai.provider.api-key") || t.matches("ai\\.provider\\.api-key\\s*=\\s*"),
                        file + " sets the API key: " + t);
                assertTrue(!t.startsWith("app.auth.password-hash") && !t.startsWith("app.auth.session-secret"),
                        file + " sets an auth secret: " + t);
            }
        }
    }
}
