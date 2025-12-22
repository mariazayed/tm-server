package trademind.tm_server.day;

import org.mapstruct.Mapper;
import trademind.tm_server.day.dto.DayResponse;
import trademind.tm_server.day.dto.NewDayRequest;
import trademind.tm_server.day.dto.UpdateDayRequest;

@Mapper(componentModel = "spring")
public interface DayMapper {
    DayResponse toResponse(Day day);

    Day toEntity(NewDayRequest request);

    Day toEntity(UpdateDayRequest request);
}

