import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/** Employee Time Management System - console application. */
public class Main {
    private static final Scanner in = new Scanner(System.in);
    private static final DataStore store = new DataStore();
    private static final String HR_PASSWORD = "12345";

    public static void main(String[] args) {
        store.load();
        System.out.println("=== Employee Time Management System ===");
        while (true) {
            System.out.println("\n1. Login as HR Manager");
            System.out.println("2. Login as Employee");
            System.out.println("3. Exit");
            int choice = readInt("Choose: ");
            switch (choice) {
                case 1: hrLogin(); break;
                case 2: employeeLogin(); break;
                case 3:
                    store.save();
                    System.out.println("Goodbye!");
                    return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    // ------------------------------------------------------------------ HR

    private static void hrLogin() {
        String pw = readLine("HR password:");
        if (!pw.trim().equals(HR_PASSWORD)) {
            System.out.println("Wrong password.");
            return;
        }
        while (true) {
            System.out.println("\n--- HR Manager Dashboard ---");
            System.out.println("1. Time Off Management");
            System.out.println("2. Schedule Management");
            System.out.println("3. Time Tracking Review");
            System.out.println("4. Generate Reports");
            System.out.println("5. Add Employee");
            System.out.println("6. Logout");
            switch (readInt("Choose: ")) {
                case 1: timeOffManagement(); break;
                case 2: scheduleManagement(); break;
                case 3: timeTrackingReview(); break;
                case 4: generateReport(); break;
                case 5: addEmployee(); break;
                case 6: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private static void timeOffManagement() {
        System.out.println("\n--- Time Off Requests ---");
        if (store.requests.isEmpty()) {
            System.out.println("No time off requests.");
            return;
        }
        printRequests(store.requests);
        int id = readInt("Enter request ID to review (0 to go back): ");
        if (id == 0) return;
        TimeOffRequest req = null;
        for (TimeOffRequest r : store.requests) if (r.getId() == id) req = r;
        if (req == null) {
            System.out.println("Request not found.");
            return;
        }
        if (req.getStatus() != TimeOffRequest.Status.PENDING) {
            System.out.println("This request has already been " + req.getStatus() + ".");
            return;
        }
        String d = readLine("Approve or Deny? (A/D): ").toUpperCase();
        if (d.startsWith("A")) {
            req.setStatus(TimeOffRequest.Status.APPROVED);
        } else if (d.startsWith("D")) {
            req.setStatus(TimeOffRequest.Status.DENIED);
        } else {
            System.out.println("No change made.");
            return;
        }
        store.save();
        System.out.println("Request #" + req.getId() + " for " + store.nameOf(req.getEmpId())
                + " has been " + req.getStatus() + ".");
    }

    private static void scheduleManagement() {
        System.out.println("\n--- Employee Schedules ---");
        System.out.printf("%-4s %-15s %-13s %s%n", "ID", "Name", "Shift", "Days Off");
        for (Schedule s : store.schedules) {
            System.out.printf("%-4d %-15s %-13s %s%n", s.getEmpId(), store.nameOf(s.getEmpId()),
                    s.getShiftStart() + "-" + s.getShiftEnd(), s.daysOffText());
        }
        int id = readInt("Enter employee ID to update (0 to go back): ");
        if (id == 0) return;
        Schedule s = store.findSchedule(id);
        if (s == null) {
            System.out.println("Employee not found.");
            return;
        }
        LocalTime start = readTime("Shift start (HH:mm): ");
        LocalTime end = readTime("Shift end (HH:mm): ");
        if (!end.isAfter(start)) {
            System.out.println("Shift end must be after shift start. Nothing changed.");
            return;
        }
        Set<DayOfWeek> off = readDaysOff();
        s.update(start, end, off);
        store.save();
        System.out.println("Schedule updated for " + store.nameOf(id) + ".");
    }

    private static void timeTrackingReview() {
        System.out.println("\n--- Time Logs ---");
        if (store.logs.isEmpty()) {
            System.out.println("No work hours have been logged yet.");
            return;
        }
        System.out.printf("%-4s %-15s %-12s %-6s %-6s %s%n", "#", "Employee", "Date", "In", "Out", "Hours");
        for (int i = 0; i < store.logs.size(); i++) {
            WorkLog w = store.logs.get(i);
            System.out.printf("%-4d %-15s %-12s %-6s %-6s %.2f%n", i + 1, store.nameOf(w.getEmpId()),
                    w.getDate(), w.getStart(), w.getEnd(), w.hours());
        }
        int n = readInt("Enter log # to correct (0 to go back): ");
        if (n == 0) return;
        if (n < 1 || n > store.logs.size()) {
            System.out.println("Invalid log number.");
            return;
        }
        WorkLog w = store.logs.get(n - 1);
        LocalTime start = readTime("Corrected start time (HH:mm): ");
        LocalTime end = readTime("Corrected end time (HH:mm): ");
        if (!end.isAfter(start)) {
            System.out.println("End time must be after start time. Nothing changed.");
            return;
        }
        w.correct(start, end);
        store.save();
        System.out.printf("Log #%d corrected. New total: %.2f hours.%n", n, w.hours());
    }

    private static void generateReport() {
        System.out.println("\n--- Generate Report ---");
        int empId = readInt("Employee ID (0 for all employees): ");
        if (empId != 0 && store.findEmployee(empId) == null) {
            System.out.println("Employee not found.");
            return;
        }
        LocalDate from = readDate("From date (yyyy-MM-dd): ", null);
        LocalDate to = readDate("To date (yyyy-MM-dd): ", null);
        if (to.isBefore(from)) {
            System.out.println("'To' date cannot be before 'From' date.");
            return;
        }

        System.out.println("\n=== Time Log Report (" + from + " to " + to + ") ===");
        System.out.printf("%-15s %-12s %-6s %-6s %s%n", "Employee", "Date", "In", "Out", "Hours");
        Map<String, Double> totals = new LinkedHashMap<>();
        for (WorkLog w : store.logs) {
            if (empId != 0 && w.getEmpId() != empId) continue;
            if (w.getDate().isBefore(from) || w.getDate().isAfter(to)) continue;
            String name = store.nameOf(w.getEmpId());
            System.out.printf("%-15s %-12s %-6s %-6s %.2f%n", name, w.getDate(), w.getStart(), w.getEnd(), w.hours());
            totals.merge(name, w.hours(), Double::sum);
        }
        if (totals.isEmpty()) System.out.println("(no logs in this range)");

        System.out.println("\n=== Time Off Report ===");
        List<TimeOffRequest> matching = new ArrayList<>();
        for (TimeOffRequest r : store.requests) {
            if (empId != 0 && r.getEmpId() != empId) continue;
            if (r.overlaps(from, to)) matching.add(r);
        }
        if (matching.isEmpty()) System.out.println("(no time off in this range)");
        else printRequests(matching);

        if (!totals.isEmpty()) {
            System.out.println("\n=== Total Hours Worked (chart) ===");
            double max = 0;
            for (double v : totals.values()) max = Math.max(max, v);
            double scale = max > 50 ? 50.0 / max : 1.0;
            for (Map.Entry<String, Double> e : totals.entrySet()) {
                int bar = (int) Math.round(e.getValue() * scale);
                System.out.printf("%-15s |%s %.2f h%n", e.getKey(), "#".repeat(bar), e.getValue());
            }
        }
    }

    private static void addEmployee() {
        String name = clean(readLine("New employee name: "));
        if (name.isEmpty()) {
            System.out.println("Name cannot be empty.");
            return;
        }
        if (store.findEmployeeByName(name) != null) {
            System.out.println("An employee with that name already exists.");
            return;
        }
        Employee e = store.addEmployee(name);
        store.save();
        System.out.println("Employee added: " + e.getName() + " (ID " + e.getId()
                + ") with default schedule 09:00-17:00, weekends off.");
    }

    // ------------------------------------------------------------ Employee

    private static void employeeLogin() {
        String name = readLine("Enter your name: ");
        Employee emp = store.findEmployeeByName(name);
        if (emp == null) {
            System.out.println("No employee found with that name. Ask HR to add you.");
            return;
        }
        System.out.println("Welcome, " + emp.getName() + "!");
        while (true) {
            System.out.println("\n--- Employee Dashboard ---");
            System.out.println("1. Log Work Hours");
            System.out.println("2. Request Time Off");
            System.out.println("3. My Time Off Requests");
            System.out.println("4. View Schedule");
            System.out.println("5. Logout");
            switch (readInt("Choose: ")) {
                case 1: logWorkHours(emp); break;
                case 2: requestTimeOff(emp); break;
                case 3: myRequests(emp); break;
                case 4: viewSchedule(emp); break;
                case 5: return;
                default: System.out.println("Invalid choice.");
            }
        }
    }

    private static void logWorkHours(Employee emp) {
        LocalDate date = readDate("Date (yyyy-MM-dd, Enter for today): ", LocalDate.now());
        LocalTime start = readTime("Start time (HH:mm): ");
        LocalTime end = readTime("End time (HH:mm): ");
        if (!end.isAfter(start)) {
            System.out.println("End time must be after start time. Entry not saved.");
            return;
        }
        WorkLog w = new WorkLog(emp.getId(), date, start, end);
        store.logs.add(w);
        store.save();
        System.out.printf("Work hours logged successfully: %.2f hours on %s.%n", w.hours(), date);
    }

    private static void requestTimeOff(Employee emp) {
        LocalDate from = readDate("From date (yyyy-MM-dd): ", null);
        LocalDate to = readDate("To date (yyyy-MM-dd): ", null);
        if (to.isBefore(from)) {
            System.out.println("'To' date cannot be before 'From' date. Request not submitted.");
            return;
        }
        String reason = clean(readLine("Reason: "));
        TimeOffRequest r = new TimeOffRequest(store.nextRequestId(), emp.getId(), from, to, reason,
                TimeOffRequest.Status.PENDING);
        store.requests.add(r);
        store.save();
        System.out.println("Time off request #" + r.getId() + " submitted (" + r.days()
                + " day(s)). Status: PENDING.");
    }

    private static void myRequests(Employee emp) {
        List<TimeOffRequest> mine = new ArrayList<>();
        for (TimeOffRequest r : store.requests) if (r.getEmpId() == emp.getId()) mine.add(r);
        if (mine.isEmpty()) {
            System.out.println("You have no time off requests.");
            return;
        }
        printRequests(mine);
    }

    private static void viewSchedule(Employee emp) {
        Schedule s = store.findSchedule(emp.getId());
        if (s == null) {
            System.out.println("No schedule set for you yet.");
            return;
        }
        LocalDate from = readDate("From date (yyyy-MM-dd, Enter for today): ", LocalDate.now());
        LocalDate to = readDate("To date (yyyy-MM-dd, Enter for 6 days later): ", from.plusDays(6));
        if (to.isBefore(from)) {
            System.out.println("'To' date cannot be before 'From' date.");
            return;
        }
        if (to.isAfter(from.plusDays(90))) {
            System.out.println("Please choose a range of at most 90 days.");
            return;
        }
        System.out.println("\nSchedule for " + emp.getName());
        System.out.printf("%-12s %-10s %s%n", "Date", "Day", "Work Schedule");
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            String info;
            if (isOnApprovedTimeOff(emp.getId(), d)) info = "Approved time off";
            else if (s.getDaysOff().contains(d.getDayOfWeek())) info = "Day off";
            else info = s.getShiftStart() + " - " + s.getShiftEnd();
            System.out.printf("%-12s %-10s %s%n", d, d.getDayOfWeek().toString().substring(0, 3), info);
        }
    }

    private static boolean isOnApprovedTimeOff(int empId, LocalDate d) {
        for (TimeOffRequest r : store.requests) {
            if (r.getEmpId() == empId && r.getStatus() == TimeOffRequest.Status.APPROVED && r.covers(d)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------- Helpers

    private static void printRequests(List<TimeOffRequest> list) {
        System.out.printf("%-4s %-15s %-12s %-12s %-5s %-9s %s%n", "ID", "Employee", "From", "To", "Days", "Status", "Reason");
        for (TimeOffRequest r : list) {
            System.out.printf("%-4d %-15s %-12s %-12s %-5d %-9s %s%n", r.getId(), store.nameOf(r.getEmpId()),
                    r.getFrom(), r.getTo(), r.days(), r.getStatus(), r.getReason());
        }
    }

    private static Set<DayOfWeek> readDaysOff() {
        while (true) {
            String line = readLine("Days off (e.g. SAT,SUN or 'none'): ").trim().toUpperCase();
            Set<DayOfWeek> set = EnumSet.noneOf(DayOfWeek.class);
            if (line.equals("NONE") || line.isEmpty()) return set;
            boolean ok = true;
            for (String part : line.split("[,\\s]+")) {
                DayOfWeek match = null;
                for (DayOfWeek d : DayOfWeek.values()) {
                    if (part.length() >= 3 && d.name().startsWith(part)) match = d;
                }
                if (match == null) { ok = false; break; }
                set.add(match);
            }
            if (ok) return set;
            System.out.println("Could not understand that. Use day names like MON,TUE or SAT,SUN.");
        }
    }

    private static String clean(String s) {
        return s.replace(",", ";").trim();
    }

    private static String readLine(String prompt) {
        System.out.print(prompt);
        if (!in.hasNextLine()) {
            store.save();
            System.out.println("\n(input closed)");
            System.exit(0);
        }
        return in.nextLine();
    }

    private static int readInt(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            try {
                return Integer.parseInt(s);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static LocalDate readDate(String prompt, LocalDate defaultValue) {
        while (true) {
            String s = readLine(prompt).trim();
            if (s.isEmpty() && defaultValue != null) return defaultValue;
            try {
                return LocalDate.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid date. Use the format yyyy-MM-dd (e.g. 2026-10-06).");
            }
        }
    }

    private static LocalTime readTime(String prompt) {
        while (true) {
            String s = readLine(prompt).trim();
            try {
                return LocalTime.parse(s);
            } catch (DateTimeParseException e) {
                System.out.println("Invalid time. Use 24-hour HH:mm (e.g. 09:30).");
            }
        }
    }
}
