package trademind.tm_server.invest.rules;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.invest.dto.InvestSettingsResponse;
import trademind.tm_server.invest.reminder.ReminderService;
import trademind.tm_server.invest.rules.RulesService.OrderRequest;
import trademind.tm_server.invest.rules.RulesService.RuleRequest;
import trademind.tm_server.invest.rules.RulesService.RuleResponse;
import trademind.tm_server.invest.settings.SettingsRequest;
import trademind.tm_server.invest.settings.SettingsChangeResponse;
import trademind.tm_server.invest.settings.SettingsService;

import java.util.List;

/** Rules, settings changes and reminders; all under /api/v1/invest so a login token is required. */
@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_INVEST)
public class RulesController {

    private final RulesService rules;
    private final SettingsService settings;
    private final ReminderService reminders;

    public RulesController(RulesService rules, SettingsService settings, ReminderService reminders) {
        this.rules = rules;
        this.settings = settings;
        this.reminders = reminders;
    }

    // rules
    @GetMapping("/rules")
    public List<RuleResponse> rules() {
        return rules.list();
    }

    @PostMapping("/rules")
    @ResponseStatus(HttpStatus.CREATED)
    public RuleResponse createRule(@Valid @RequestBody RuleRequest request) {
        return rules.create(request);
    }

    @PutMapping("/rules/order")
    public List<RuleResponse> reorder(@Valid @RequestBody OrderRequest request) {
        return rules.reorder(request);
    }

    @PutMapping("/rules/{id}")
    public RuleResponse updateRule(@PathVariable Long id, @Valid @RequestBody RuleRequest request) {
        return rules.update(id, request);
    }

    @DeleteMapping("/rules/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable Long id) {
        rules.delete(id);
    }

    // settings (GET lives with the contributions controller)
    @PutMapping("/settings")
    public InvestSettingsResponse updateSettings(@Valid @RequestBody SettingsRequest request) {
        return settings.update(request);
    }

    /** History of plan changes (amount, ticker, buy day), oldest first. */
    @GetMapping("/settings/changes")
    public List<SettingsChangeResponse> settingsChanges() {
        return settings.changes();
    }

    // reminders
    @GetMapping("/reminders")
    public ReminderService.Status reminderStatus() {
        return reminders.status();
    }

    /** Sends a test message now; a delivery problem is reported in the body, not as an HTTP error. */
    @PostMapping("/reminders/test")
    public ReminderService.RunResult testReminder() {
        return reminders.sendTest();
    }
}
