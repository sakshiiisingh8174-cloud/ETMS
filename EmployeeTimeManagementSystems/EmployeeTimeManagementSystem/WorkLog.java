import java.time.LocalDate;
import java.time.LocalTime;

class WorkLog implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int empId;
    private final LocalDate date;
    private LocalTime start;
    private LocalTime end;
    WorkLog(int empId, LocalDate date, LocalTime start, LocalTime end) {
        this.empId = empId; this.date = date; this.start = start; this.end = end;
    }
    int getEmpId() { return empId; }
    LocalDate getDate() { return date; }
    LocalTime getStart() { return start; }
    LocalTime getEnd() { return end; }
    double hours() { return java.time.Duration.between(start, end).toMinutes() / 60.0; }
    void correct(LocalTime start, LocalTime end) { this.start = start; this.end = end; }
}
