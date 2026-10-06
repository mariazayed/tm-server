package trademind.tm_server.invest.journal;

import org.junit.jupiter.api.Test;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.invest.TagService;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalTagNormalizationTest {

    @Test
    void trimsStripsHashLowercasesAndDedupes() {
        assertEquals(List.of("emotion", "market event", "learning"),
                TagService.normalize(List.of(" #Emotion ", "##Market   Event", "EMOTION", "learning", "  ", "#")));
    }

    @Test
    void nullIsEmpty() {
        assertEquals(List.of(), TagService.normalize(null));
    }

    @Test
    void rejectsMoreThanTenDistinctTags() {
        List<String> eleven = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();
        assertThrows(BadRequestException.class, () -> TagService.normalize(eleven));
        assertEquals(10, TagService.normalize(eleven.subList(0, 10)).size());
    }
}
