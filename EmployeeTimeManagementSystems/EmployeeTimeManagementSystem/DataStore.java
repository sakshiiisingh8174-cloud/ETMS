import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.ArrayList;
import java.util.List;

class DataStore implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private static final String DATA_FILE = "employee_time_data.ser";
    List<Employee> employees = new ArrayList<>();
    List<Schedule> schedules = new ArrayList<>();
    List<WorkLog> logs = new ArrayList<>();
    List<TimeOffRequest> requests = new ArrayList<>();
    private int nextEmployeeId = 1;
    private int nextRequestId = 1;

    void load() {
        java.io.File file = new java.io.File(DATA_FILE);
        if (!file.exists()) {
            addEmployee("John Smith");
            addEmployee("Emily Davis");
            return;
        }
        try (java.io.ObjectInputStream input = new java.io.ObjectInputStream(new java.io.FileInputStream(file))) {
            DataStore saved = (DataStore) input.readObject();
            employees = saved.employees;
            schedules = saved.schedules;
            logs = saved.logs;
            requests = saved.requests;
            nextEmployeeId = saved.nextEmployeeId;
            nextRequestId = saved.nextRequestId;
        } catch (java.io.IOException | ClassNotFoundException | ClassCastException ex) {
            System.out.println("Could not load saved data. Starting with a fresh employee list.");
            employees.clear(); schedules.clear(); logs.clear(); requests.clear();
            nextEmployeeId = 1; nextRequestId = 1;
            addEmployee("John Smith"); addEmployee("Emily Davis");
        }
    }
    void save() {
        try (java.io.ObjectOutputStream output = new java.io.ObjectOutputStream(new java.io.FileOutputStream(DATA_FILE))) {
            output.writeObject(this);
        } catch (java.io.IOException ex) {
            System.out.println("Warning: data could not be saved: " + ex.getMessage());
        }
    }
    Employee addEmployee(String name) {
        Employee employee = new Employee(nextEmployeeId++, name);
        employees.add(employee);
        schedules.add(new Schedule(employee.getId(), LocalTime.of(9, 0), LocalTime.of(17, 0),
                EnumSet.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)));
        return employee;
    }
    Employee findEmployee(int id) {
        for (Employee e : employees) if (e.getId() == id) return e;
        return null;
    }
    Employee findEmployeeByName(String name) {
        String target = name.trim();
        for (Employee e : employees) if (e.getName().equalsIgnoreCase(target)) return e;
        return null;
    }
    Schedule findSchedule(int id) {
        for (Schedule s : schedules) if (s.getEmpId() == id) return s;
        return null;
    }
    String nameOf(int id) {
        Employee e = findEmployee(id);
        return e == null ? "Unknown" : e.getName();
    }
    int nextRequestId() { return nextRequestId++; }
}
