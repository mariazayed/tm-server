package trademind.tm_server.invest.settings;

import trademind.tm_server.invest.SettingsChange;

import java.time.LocalDateTime;

/** One recorded change to the plan: which field, the value before and after, and when. */
public record SettingsChangeResponse(String field, String oldValue, String newValue, LocalDateTime changedAt) {

    public static SettingsChangeResponse from(SettingsChange c) {
        return new SettingsChangeResponse(c.getFieldName(), c.getOldValue(), c.getNewValue(), c.getCreatedAt());
    }
}
