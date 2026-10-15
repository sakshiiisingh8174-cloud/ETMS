import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

class Schedule implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int empId;
    private LocalTime shiftStart;
    private LocalTime shiftEnd;
    private Set<DayOfWeek> daysOff;
    Schedule(int empId, LocalTime start, LocalTime end, Set<DayOfWeek> daysOff) {
        this.empId = empId; this.shiftStart = start; this.shiftEnd = end;
        this.daysOff = daysOff.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(daysOff);
    }
    int getEmpId() { return empId; }
    LocalTime getShiftStart() { return shiftStart; }
    LocalTime getShiftEnd() { return shiftEnd; }
    Set<DayOfWeek> getDaysOff() { return daysOff; }
    void update(LocalTime start, LocalTime end, Set<DayOfWeek> off) {
        shiftStart = start; shiftEnd = end;
        daysOff = off.isEmpty() ? EnumSet.noneOf(DayOfWeek.class) : EnumSet.copyOf(off);
    }
    String daysOffText() {
        if (daysOff.isEmpty()) return "None";
        List<String> names = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) if (daysOff.contains(d)) names.add(d.name().substring(0, 3));
        return String.join(",", names);
    }
}
