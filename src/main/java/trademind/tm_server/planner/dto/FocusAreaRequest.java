package trademind.tm_server.planner.dto;

import lombok.Data;

// Used for both create and (partial) update -- null fields are left
// unchanged on update. name/color/maxPercent are required on create.
@Data
public class FocusAreaRequest {
    private String name;
    private String color;
    private String icon;
    private Double maxPercent;
    private Integer sortOrder;
    private Boolean archived;
    private String notes;
}
