package trademind.tm_server.features;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = FeaturesControllerTest.TestApp.class)
@Import({FeaturesController.class, FeatureFlags.class})
class FeaturesControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;

    @Test
    void tradingIsOffByDefault() throws Exception {
        mvc.perform(get("/api/v1/features"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trading").value(false))
                .andExpect(jsonPath("$.investing").value(true));
    }
}
