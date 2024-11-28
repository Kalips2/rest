package lab2.rest.service;

import lab2.rest.migrated.database.Database;
import lab2.rest.migrated.database.DatabaseReader;
import org.springframework.stereotype.Service;

@Service
public class DatabaseServiceImpl implements DatabaseService {
    @Override
    public Database getDatabase() {
        var database = DatabaseReader.getDatabase();
        if (database == null) {
            throw new RuntimeException("No active database");
        }
        return database;
    }
}
