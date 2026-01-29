package trademind.tm_server.capital;

import org.mapstruct.Mapper;
import trademind.tm_server.asset.AssetMapper;
import trademind.tm_server.capital.dto.NewCapitalRequest;
import trademind.tm_server.capital.dto.CapitalResponse;

@Mapper(componentModel = "spring", uses = AssetMapper.class)
public interface CapitalMapper {
    CapitalResponse toResponse(Capital capital);

    Capital toEntity(NewCapitalRequest request);
}