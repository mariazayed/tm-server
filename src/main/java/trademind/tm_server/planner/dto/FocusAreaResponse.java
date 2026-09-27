package trademind.tm_server.planner.dto;

import lombok.Data;

@Data
public class FocusAreaResponse {
    private Long id;
    private String name;
    private String color;
    private String icon;
    private Double maxPercent;
    private Integer sortOrder;
    private boolean archived;
    private String notes;
}
