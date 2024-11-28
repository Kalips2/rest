package lab2.rest.controllers;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lab2.rest.migrated.database.Result;
import lab2.rest.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class TableController {
    private final DatabaseService databaseService;

    @GetMapping(value = "/tables")
    public ResponseEntity<List<String >> tables() {
        final var database = databaseService.getDatabase();
        Result result = database.query("list tables");
        return ResponseEntity.ok(Arrays.stream(result.getReport().split("; ")).toList());
    }

    @DeleteMapping(value = "/tables/delete")
    public ResponseEntity<String> dropTable(@RequestParam String tableName) {
        final var database = databaseService.getDatabase();
        Result query = database.query(String.format("delete table %s", tableName));
        if (query.getStatus() != Result.Status.OK) {
            throw new RuntimeException(query.getReport());
        }
        return ResponseEntity.ok(query.getReport());
    }

    @PostMapping(value = "/tables/create")
    public ResponseEntity<String> createTable(@RequestParam String columns,
                                              @RequestParam String tableName) {
        final var database = databaseService.getDatabase();
        Result query = database.query(String.format("create table %s (%s)", tableName, columns));
        if (query.getStatus() != Result.Status.OK) {
            throw new RuntimeException(query.getReport());
        }
        return ResponseEntity.ok(query.getReport());
    }
}
