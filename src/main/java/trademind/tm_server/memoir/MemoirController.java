package trademind.tm_server.memoir;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.memoir.dto.MemoirResponse;
import trademind.tm_server.memoir.dto.NewMemoirRequest;
import trademind.tm_server.memoir.dto.UpdateMemoirRequest;

import java.util.List;

@CrossOrigin(origins = "http://localhost:4300")
@RestController
@RequestMapping(AppConstants.URL_MEMOIR)
public class MemoirController {
    @Autowired
    private MemoirService service;

    @PostMapping
    public MemoirResponse create(@RequestBody NewMemoirRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<MemoirResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public MemoirResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public MemoirResponse update(@PathVariable Long id, @RequestBody UpdateMemoirRequest request) {
        return service.update(id, request);
    }
}