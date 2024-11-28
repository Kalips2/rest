package lab2.rest.migrated.database;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.mentaregex.Regex;

public class Database {

    private String filePath;

    private final HashMap<String, Table> tables;

    public Database(String filePath) {
        this.filePath = filePath;
        tables = new HashMap<>();
    }

    public void setFilePath(String value) {
        filePath = value;
    }

    public Collection<Column> getTableColumns(String name) throws Exception {
        if (!tables.containsKey(name))
            throw new Exception(String.format("A table with the name '%s' doesn't exist", name));
        return tables.get(name).getColumns();
    }

    public Collection<Row> getTableRows(String name) throws Exception {
        if (!tables.containsKey(name))
            throw new Exception(String.format("A table with the name '%s' doesn't exist", name));
        return tables.get(name).getRows();
    }

    public void createTable(String name, Collection<Column> columns) throws Exception {
        if (tables.containsKey(name))
            throw new Exception(String.format("A table with the name '%s' already exists", name));
        tables.put(name, new Table(name, columns));
    }

    public void removeTable(String name) throws Exception {
        if (!tables.containsKey(name))
            throw new Exception(String.format("A table with the name '%s' doesn't exist", name));
        tables.remove(name);
    }

    public Collection<String> getTableNames() {
        return new ArrayList<>(tables.keySet());
    }

    public Result query(String queryMessage) {
        System.out.printf("%s%n", queryMessage);
        try {
            for (QueryRegex regex : QueryRegex.values()) {
                String[] found = Regex.match(queryMessage, regex.getRegex());
                if (found == null) continue;

                ArrayList<String> matches = Arrays.stream(found).filter(Predicate.isEqual(null).negate()).map(String::trim).collect(Collectors.toCollection(ArrayList::new));

                Result result = new Result(Result.Status.OK);
                boolean modified = false;

                switch (regex) {
                    case INSERT_ROW: {
                        Table table = tables.get(matches.get(0));

                        ArrayList<Column> columns = toColumns(parseValues(matches.get(1)), table);
                        ArrayList<String> values = parseValues(matches.get(2));

                        table.insert(IntStream.range(0, columns.size()).boxed().collect(Collectors.toMap(columns::get, values::get)));
                        modified = true;
                        break;
                    }
                    case DELETE_ROWS: {
                        Predicate<Row> predicate = parsePredicate(matches.get(1));
                        tables.get(matches.get(0)).delete(predicate);
                        modified = true;
                        break;
                    }
                    case UPDATE_ROWS: {
                        Table table = tables.get(matches.get(0));

                        ArrayList<String> assignmentExprs = parseValues(matches.get(1));
                        Map<Column, String> assignments = new HashMap<>();

                        for (String assignmentExpr : assignmentExprs) {
                            String[] pair = parseEqual(assignmentExpr);
                            assignments.put(table.getColumn(pair[0]), pair[1]);
                        }

                        table.update(assignments, parsePredicate(matches.get(2)));
                        modified = true;
                        break;
                    }
                    case SELECT_ROWS: {
                        Table table = tables.get(matches.get(1));
                        String predicateExpr = matches.size() > 2 ? matches.get(2) : null;
                        Collection<Column> columns = matches.get(0).equals("*") ? table.getColumns() : toColumns(parseValues(matches.get(0)), table);

                        result.setRows(table.select(columns, parsePredicate(predicateExpr)));
                        break;
                    }
                    case CREATE_TABLE: {
                        String[] columnProps = matches.get(1).split(",");

                        ArrayList<Column> columns = new ArrayList<>();
                        for (String prop : columnProps) {
                            String[] tokens = prop.trim().split("\\s+");

                            Column column = new Column(Column.Type.valueOf(tokens[0]), tokens[1]);
                            columns.add(column);
                        }

                        createTable(matches.get(0), columns);
                        modified = true;
                        break;
                    }
                    case DELETE_TABLE: {
                        removeTable(matches.get(0));
                        modified = true;
                        break;
                    }
                    case LIST_TABLES: {
                        result.setReport(String.join("; ", getTableNames()));
                        break;
                    }
                    case SORT_BY: {
                        String tableName = matches.get(0);
                        String columnName = matches.get(1);
                        String sortOrder = matches.size() > 2 ? matches.get(2).toUpperCase() : "ASC";

                        Table table = tables.get(tableName);
                        Column columnToSort = table.getColumn(columnName);

                        ElementComparator elementComparator = new ElementComparator(columnToSort);
                        Comparator<Row> comparator = (row1, row2) -> {
                            Element e1 = row1.getElement(columnToSort.getName());
                            Element e2 = row2.getElement(columnToSort.getName());
                            return elementComparator.compare(e1, e2);
                        };

                        if ("DESC".equals(sortOrder)) {
                            comparator = comparator.reversed();
                        }

                        List<Row> rows = table.getRows().stream().toList();
                        List<Row> sortedRows = rows.stream().sorted(comparator).collect(Collectors.toList());

                        table.setRows(sortedRows);
                        modified = true;
                        break;
                    }
                }

                if (modified) save();

                return result;
            }
        } catch (Exception e) {
            return new Result(Result.Status.FAIL).setReport(e.getMessage());
        }

        return new Result(Result.Status.FAIL).setReport("Invalid query syntax");
    }

    private Predicate<Row> parsePredicate(String expr) throws Exception {
        Predicate<Row> result = row -> true;

        if (expr == null) return result;

        for (String equalExpr : parseValues(expr)) {
            result = result.and(toEqualPredicate(parseEqual(equalExpr)));
        }

        return result;
    }

    private String[] parseEqual(String expr) throws Exception {
        String[] pair = expr.split("=");
        if (pair.length != 2) throw new Exception("Invalid equal expression");
        return pair;
    }

    private Predicate<Row> toEqualPredicate(String[] pair) {
        return row -> {
            Element element = row.getElement(pair[0].trim());
            if (element == null) return false;
            return element.getAsString().trim().equals(pair[1]);
        };
    }

    private ArrayList<String> parseValues(String expr) {
        return Arrays.stream(expr.split(",")).map(String::trim).collect(Collectors.toCollection(ArrayList::new));
    }

    private ArrayList<Column> toColumns(Collection<String> names, Table table) throws Exception {
        ArrayList<Column> result = new ArrayList<>();
        for (String colName : names) result.add(table.getColumn(colName));
        return result;
    }

    public void save() throws IOException {
        if (filePath == null) return;

        FileWriter writer = new FileWriter(filePath);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        gson.toJson(this, writer);
        writer.close();
    }

    public byte[] download() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this).getBytes();
    }
}
