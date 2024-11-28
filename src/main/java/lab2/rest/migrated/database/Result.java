package lab2.rest.migrated.database;

import java.io.Serializable;
import java.util.Collection;
import lombok.Getter;

public class Result implements Serializable {
    public enum Status {
        OK,
        FAIL
    }

    @Getter
    private Status status;
    @Getter
    private String report;
    @Getter
    private Collection<Row> rows;

    public Result(Status status) {
        this.status = status;
        report = "";
        rows = null;
    }

    Result setReport(String report) {
        this.report = report;
        return this;
    }

    public void setRows(Collection<Row> rows) {
        this.rows = rows;
    }
}
