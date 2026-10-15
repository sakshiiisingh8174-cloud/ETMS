import java.time.LocalDate;

class TimeOffRequest implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    enum Status { PENDING, APPROVED, DENIED }
    private final int id;
    private final int empId;
    private final LocalDate from;
    private final LocalDate to;
    private final String reason;
    private Status status;
    TimeOffRequest(int id, int empId, LocalDate from, LocalDate to, String reason, Status status) {
        this.id = id; this.empId = empId; this.from = from; this.to = to;
        this.reason = reason; this.status = status;
    }
    int getId() { return id; }
    int getEmpId() { return empId; }
    LocalDate getFrom() { return from; }
    LocalDate getTo() { return to; }
    String getReason() { return reason; }
    Status getStatus() { return status; }
    void setStatus(Status status) { this.status = status; }
    int days() { return (int) java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1; }
    boolean covers(LocalDate date) { return !date.isBefore(from) && !date.isAfter(to); }
    boolean overlaps(LocalDate start, LocalDate end) { return !to.isBefore(start) && !from.isAfter(end); }
}
