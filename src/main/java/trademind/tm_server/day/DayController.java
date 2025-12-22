package trademind.tm_server.day;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import trademind.tm_server.constants.AppConstants;
import trademind.tm_server.day.dto.DayResponse;
import trademind.tm_server.day.dto.NewDayRequest;
import trademind.tm_server.day.dto.UpdateDayRequest;

import java.util.List;

@RestController
@RequestMapping(AppConstants.URL_DAY)
public class DayController {
    @Autowired
    private DayService service;

    @PostMapping
    public DayResponse create(@RequestBody NewDayRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<DayResponse> getAll() {
        return service.getAll();
    }

    @GetMapping(AppConstants.SUB_URL_ID_PARAM)
    public DayResponse getById(@PathVariable Long id) {
        return service.getOneById(id);
    }

    @PutMapping(AppConstants.SUB_URL_ID_PARAM)
    public DayResponse update(@PathVariable Long id, @RequestBody UpdateDayRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping(AppConstants.SUB_URL_ID_PARAM)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(AppConstants.SUB_URL_UNDO_DELETE)
    public DayResponse undoDelete(@PathVariable Long id) {
        return service.undoDelete(id);
    }
}