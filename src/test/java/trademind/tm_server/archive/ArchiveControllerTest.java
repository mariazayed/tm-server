package trademind.tm_server.archive;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.features.FeatureFlags;
import trademind.tm_server.features.TradingGateFilter;
import trademind.tm_server.trade.TradeService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = ArchiveControllerTest.TestApp.class)
@Import({ArchiveController.class, FeatureFlags.class, TradingGateFilter.class})
class ArchiveControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean TradeService trades;

    /** The point of the archive: it works with trading switched off (the test default). */
    @Test
    void exportWorksWhileTradingIsHidden() throws Exception {
        when(trades.getAll()).thenReturn(List.of());
        mvc.perform(get("/api/v1/archive/trades.csv"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"trades.csv\""))
                .andExpect(content().string(org.hamcrest.Matchers.startsWith("id,external_id,symbol")));
    }
}
