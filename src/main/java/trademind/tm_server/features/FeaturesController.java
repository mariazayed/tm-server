package trademind.tm_server.features;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;

/** Lets clients ask what is switched on so they can hide the rest. Read-only and always available. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_FEATURES)
public class FeaturesController {

    private final FeatureFlags flags;

    public FeaturesController(FeatureFlags flags) {
        this.flags = flags;
    }

    @GetMapping
    public FeaturesResponse features() {
        return new FeaturesResponse(flags.trading(), flags.investing());
    }
}
