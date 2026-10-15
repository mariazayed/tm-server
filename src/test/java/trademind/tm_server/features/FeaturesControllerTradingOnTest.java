package trademind.tm_server.features;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = FeaturesControllerTradingOnTest.TestApp.class)
@Import({FeaturesController.class, FeatureFlags.class})
@TestPropertySource(properties = "features.trading.enabled=true")
class FeaturesControllerTradingOnTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;

    @Test
    void tradingCanBeSwitchedOn() throws Exception {
        mvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trading").value(true))
                .andExpect(jsonPath("$.investing").value(true));
    }
}
