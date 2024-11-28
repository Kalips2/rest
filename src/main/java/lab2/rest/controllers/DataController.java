package lab2.rest.controllers;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lab2.rest.migrated.database.Element;
import lab2.rest.migrated.database.Row;
import lombok.RequiredArgsConstructor;
import lab2.rest.migrated.database.Result;
import lab2.rest.service.DatabaseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DataController {
    private final DatabaseService databaseService;

    @GetMapping(value = "/data")
    public ResponseEntity<List<Map<String, Element>>> select(@RequestParam String tableName) {
        final var database = databaseService.getDatabase();
        Result query = database.query(String.format("select * from %s", tableName));
        if (query.getStatus() != Result.Status.OK) {
            throw new RuntimeException(query.getReport());
        }
        return ResponseEntity.ok(query.getRows().stream().map(Row::getElementsAll).collect(Collectors.toList()));
    }

    @PostMapping(value = "/data/sort")
    public ResponseEntity<List<Map<String, Element>>> sort(@RequestParam String tableName,
                                                           @RequestParam String column,
                                                           @RequestParam String order) {
        final var database = databaseService.getDatabase();
        Result query = database.query(String.format("sort from %s by %s %s", tableName, column, order));
        if (query.getStatus() != Result.Status.OK) {
            throw new RuntimeException(query.getReport());
        }
        return this.select(tableName);
    }

    @PostMapping(value = "/data/query")
    public ResponseEntity<Void> sort(@RequestParam String query) {
        final var database = databaseService.getDatabase();
        Result result = database.query(query);
        if (result.getStatus() != Result.Status.OK) {
            throw new RuntimeException(result.getReport());
        }
        return ResponseEntity.ok().build();
    }

    @PostMapping(value = "/data/insert")
    public ResponseEntity<String> insert(@RequestParam String columns,
                                         @RequestParam String tableName,
                                         @RequestParam String values) {
        final var database = databaseService.getDatabase();
      Result query = database.query(
          String.format("insert into %s (%s) values (%s)", tableName, columns, values));
      if (query.getStatus() != Result.Status.OK) {
        throw new RuntimeException(query.getReport());
      }
      return ResponseEntity.ok(query.getReport());
    }

    @DeleteMapping(value = "/data/delete")
    public ResponseEntity<String> delete(@RequestParam String tableName,
                                         @RequestParam String condition) {
        final var database = databaseService.getDatabase();
      Result query = database.query(String.format("delete from %s where %s", tableName, condition));
      if (query.getStatus() != Result.Status.OK) {
        throw new RuntimeException(query.getReport());
      }
      return ResponseEntity.ok(query.getReport());
    }
}
