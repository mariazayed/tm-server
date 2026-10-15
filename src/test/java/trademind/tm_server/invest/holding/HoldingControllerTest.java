package trademind.tm_server.invest.holding;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import trademind.tm_server.exceptions.BadRequestException;
import trademind.tm_server.exceptions.GlobalExceptionHandler;
import trademind.tm_server.invest.Holding;
import trademind.tm_server.invest.InvestExceptionHandler;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = HoldingControllerTest.TestApp.class)
@Import({HoldingController.class, GlobalExceptionHandler.class, InvestExceptionHandler.class})
class HoldingControllerTest {

    @org.springframework.boot.SpringBootConfiguration
    static class TestApp {
    }

    @Autowired MockMvc mvc;
    @MockitoBean HoldingService holdings;

    private static final String VALID = """
            {"ticker":"agg","name":"US bond fund","assetClass":"BONDS","region":"United States",
             "targetPct":25,"expenseRatio":0.03}""";

    private static HoldingResponse response() {
        return new HoldingResponse(2L, "AGG", "US bond fund", Holding.AssetClass.BONDS, "United States",
                new BigDecimal("25"), new BigDecimal("0.030"), null);
    }

    @Test
    void listsAndCreatesFunds() throws Exception {
        when(holdings.list()).thenReturn(List.of(response()));
        mvc.perform(get("/api/v1/invest/holdings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ticker").value("AGG"))
                .andExpect(jsonPath("$[0].assetClass").value("BONDS"))
                .andExpect(jsonPath("$[0].targetPct").value(25));

        when(holdings.create(any())).thenReturn(response());
        mvc.perform(post("/api/v1/invest/holdings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void updatesAndDeletes() throws Exception {
        when(holdings.update(eq(2L), any())).thenReturn(response());
        mvc.perform(put("/api/v1/invest/holdings/2").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/v1/invest/holdings/2")).andExpect(status().isNoContent());
        verify(holdings).delete(2L);
    }

    @Test
    void badInputNeverReachesTheService() throws Exception {
        for (String body : List.of(
                VALID.replace("\"agg\"", "\"bad ticker!\""),
                VALID.replace("\"agg\"", "\"\""),
                VALID.replace("\"targetPct\":25", "\"targetPct\":101"),
                VALID.replace("\"targetPct\":25", "\"targetPct\":-1"),
                VALID.replace("\"expenseRatio\":0.03", "\"expenseRatio\":9"),
                VALID.replace("\"BONDS\"", "\"MEMECOINS\""),
                VALID.replace("\"United States\"", "\"\""))) {
            mvc.perform(post("/api/v1/invest/holdings").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        verify(holdings, never()).create(any());
    }

    @Test
    void aRefusalFromTheServiceIsAClearBadRequest() throws Exception {
        when(holdings.create(any())).thenThrow(new BadRequestException("Tracking more than one fund is not switched on yet"));
        mvc.perform(post("/api/v1/invest/holdings").contentType(MediaType.APPLICATION_JSON).content(VALID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tracking more than one fund is not switched on yet"));
    }
}
