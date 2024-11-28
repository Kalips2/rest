package lab2.rest.controllers;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import lab2.rest.migrated.database.Database;
import lab2.rest.migrated.database.DatabaseReader;
import lab2.rest.service.DatabaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@Slf4j
@RestController
@RequiredArgsConstructor
public class DatabaseController {
  public static final String DOWNLOAD_DEFAULT_FILENAME = "DB.json";
  private Database database;
  private final DatabaseService databaseService;

  @PostMapping("/database/create")
  public ResponseEntity<Void> createDatabase(@RequestParam String path) {
    if (path == null) {
      throw new RuntimeException("Filepath not found");
    }

    if (!path.endsWith(".json")) {
      path += ".json";
    }

    File newDatabaseFile = new File(path);

    if (!newDatabaseFile.exists()) {
      try {
        newDatabaseFile.createNewFile();
        this.database = new Database(path);
        this.database.save();
        this.database = new DatabaseReader(path).read();
        return ResponseEntity.ok().build();
      } catch (IOException e) {
        throw new RuntimeException("File creation failed");
      }
    } else {
      throw new RuntimeException("File already exists");
    }
  }

  @PostMapping("/database/upload")
  public ResponseEntity<Void> uploadDatabase(@RequestParam String path) {
    try {
      database = new DatabaseReader(path).read();
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File not found");
    } catch (JsonSyntaxException | JsonIOException e) {
      throw new RuntimeException("Invalid JSON");
    }
    return ResponseEntity.ok().build();
  }

  @PostMapping("/database/download")
  public ResponseEntity<byte[]> downloadDatabase() throws RuntimeException {
    final var serviceDatabase = databaseService.getDatabase();
    if (serviceDatabase == null) {
      throw new RuntimeException("No active database");
    }
    return ResponseEntity.ok()
        .header("content-disposition", "attachment; filename=" + DOWNLOAD_DEFAULT_FILENAME)
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(serviceDatabase.download());
  }
}
