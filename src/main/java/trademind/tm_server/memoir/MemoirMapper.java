package trademind.tm_server.memoir;

import org.mapstruct.Mapper;
import trademind.tm_server.memoir.dto.MemoirResponse;
import trademind.tm_server.memoir.dto.NewMemoirRequest;
import trademind.tm_server.memoir.dto.UpdateMemoirRequest;

@Mapper(componentModel = "spring")
public interface MemoirMapper {
    MemoirResponse toResponse(Memoir memoir);

    Memoir toEntity(NewMemoirRequest request);

    Memoir toEntity(UpdateMemoirRequest request);
}

