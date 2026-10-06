package trademind.tm_server.invest.journal;

import org.junit.jupiter.api.Test;
import trademind.tm_server.exceptions.BadRequestException;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JournalTagNormalizationTest {

    @Test
    void trimsStripsHashLowercasesAndDedupes() {
        assertEquals(List.of("emotion", "market event", "learning"),
                JournalService.normalizeTags(List.of(" #Emotion ", "##Market   Event", "EMOTION", "learning", "  ", "#")));
    }

    @Test
    void nullIsEmpty() {
        assertEquals(List.of(), JournalService.normalizeTags(null));
    }

    @Test
    void rejectsMoreThanTenDistinctTags() {
        List<String> eleven = IntStream.range(0, 11).mapToObj(i -> "tag" + i).toList();
        assertThrows(BadRequestException.class, () -> JournalService.normalizeTags(eleven));
        assertEquals(10, JournalService.normalizeTags(eleven.subList(0, 10)).size());
    }
}
