import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Swing GUI for the Employee Time Management System.
 * Uses the same DataStore and model classes as the console version (Main.java),
 * so both programs read and write the same CSV files in the "data" folder.
 */
public class GuiApp extends JFrame {

    /** Change the text between the quotes to change the HR password. */
    private static final String HR_PASSWORD = "12345";

    // ---- colours -----------------------------------------------------------
    private static final Color INK = new Color(0x0F1620);
    private static final Color TEAL = new Color(0x0B7A66);
    private static final Color TEAL_BRIGHT = new Color(0x3FD0B0);
    private static final Color BG = new Color(0xF3F1EA);
    private static final Color CARD = new Color(0xFBFAF6);
    private static final Color LINE = new Color(0xDDD9CC);
    private static final Color MUTED = new Color(0x5F6B78);
    private static final Color SLATE = new Color(0x263445);
    private static final Color GREEN = new Color(0x1B7F4B);
    private static final Color AMBER = new Color(0xA85F00);
    private static final Color RED = new Color(0xB3261E);

    private final DataStore store = new DataStore();
    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);
    private final List<Runnable> hrRefresh = new ArrayList<>();
    private JComboBox<String> loginNames;
    private JPanel empPanel;

    public GuiApp() {
        super("Employee Time Management System");
        store.load();
        root.add(buildLogin(), "LOGIN");
        root.add(buildHr(), "HR");
        setContentPane(root);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { store.save(); }
        });
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setLocationRelativeTo(null);
        showLogin();
    }

    // =================================================================== navigation

    void showLogin() {
        loginNames.removeAllItems();
        for (Employee e : store.employees) loginNames.addItem(e.getName());
        cards.show(root, "LOGIN");
    }

    void openHr() {
        run(hrRefresh);
        cards.show(root, "HR");
    }

    void openEmployee(Employee emp) {
        if (empPanel != null) root.remove(empPanel);
        empPanel = buildEmployee(emp);
        root.add(empPanel, "EMP");
        cards.show(root, "EMP");
        root.revalidate();
    }

    private static void run(List<Runnable> list) {
        for (Runnable r : list) r.run();
    }

    // =================================================================== login screen

    private JPanel buildLogin() {
        JPanel bg = new JPanel(new GridBagLayout());
        bg.setBackground(INK);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(CARD);
        card.setBorder(new EmptyBorder(32, 44, 36, 44));

        JLabel eyebrow = label("TIME MANAGEMENT", 12, true, TEAL);
        JLabel title = label("Employee Time Management System", 24, true, INK);
        JLabel sub = label("Choose how you want to sign in.", 14, false, MUTED);
        for (JLabel l : new JLabel[]{eyebrow, title, sub}) l.setAlignmentX(Component.LEFT_ALIGNMENT);

        // HR tab
        JPanel hrForm = formPanel();
        JPasswordField pw = new JPasswordField(20);
        addField(hrForm, "HR password", pw);
        JButton hrBtn = button("Sign in as HR", TEAL);
        addButton(hrForm, hrBtn);
        Runnable hrLogin = () -> {
            String typed = new String(pw.getPassword());
            pw.setText("");
            if (typed.equals(HR_PASSWORD)) openHr();
            else error("Wrong password. Please try again.");
        };
        hrBtn.addActionListener(e -> hrLogin.run());
        pw.addActionListener(e -> hrLogin.run());

        // Employee tab
        JPanel empForm = formPanel();
        loginNames = new JComboBox<>();
        addField(empForm, "Select your name", loginNames);
        JButton empBtn = button("Sign in as Employee", TEAL);
        addButton(empForm, empBtn);
        empBtn.addActionListener(e -> {
            String name = (String) loginNames.getSelectedItem();
            Employee emp = name == null ? null : store.findEmployeeByName(name);
            if (emp == null) error("No employee found. Ask HR to add you first.");
            else openEmployee(emp);
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("HR Manager", hrForm);
        tabs.addTab("Employee", empForm);
        tabs.setAlignmentX(Component.LEFT_ALIGNMENT);
        tabs.setPreferredSize(new Dimension(380, 200));

        card.add(eyebrow);
        card.add(Box.createVerticalStrut(6));
        card.add(title);
        card.add(Box.createVerticalStrut(4));
        card.add(sub);
        card.add(Box.createVerticalStrut(22));
        card.add(tabs);
        bg.add(card);
        return bg;
    }

    // =================================================================== HR dashboard

    private JPanel buildHr() {
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Time Off", timeOffTab());
        tabs.addTab("Schedules", schedulesTab());
        tabs.addTab("Time Logs", timeLogsTab());
        tabs.addTab("Reports", reportsTab());
        tabs.addTab("Employees", employeesTab());

        JPanel p = new JPanel(new BorderLayout());
        p.add(header("HR Manager Dashboard", "Review requests, schedules, hours and reports", "Log out",
                () -> { store.save(); showLogin(); }), BorderLayout.NORTH);
        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    // ---- Time off ---------------------------------------------------------

    private JPanel timeOffTab() {
        JPanel page = page();
        DefaultTableModel m = model("ID", "Employee", "From", "To", "Days", "Status", "Reason");
        JTable t = table(m);
        JComboBox<String> filter = new JComboBox<>(new String[]{"All", "PENDING", "APPROVED", "DENIED"});
        JLabel counts = label("", 14, false, MUTED);

        Runnable rows = () -> {
            m.setRowCount(0);
            int pending = 0, approved = 0, denied = 0;
            List<TimeOffRequest> list = new ArrayList<>(store.requests);
            list.sort((a, b) -> b.getFrom().compareTo(a.getFrom()));
            String f = (String) filter.getSelectedItem();
            for (TimeOffRequest r : list) {
                if (r.getStatus() == TimeOffRequest.Status.PENDING) pending++;
                else if (r.getStatus() == TimeOffRequest.Status.APPROVED) approved++;
                else denied++;
                if (!"All".equals(f) && !r.getStatus().name().equals(f)) continue;
                m.addRow(new Object[]{r.getId(), store.nameOf(r.getEmpId()), r.getFrom().toString(),
                        r.getTo().toString(), (int) r.days(), r.getStatus().name(), r.getReason()});
            }
            counts.setText("Pending " + pending + "   ·   Approved " + approved + "   ·   Denied " + denied);
        };
        filter.addActionListener(e -> rows.run());
        hrRefresh.add(rows);

        JButton approve = button("Approve selected", GREEN);
        JButton deny = button("Deny selected", RED);
        approve.addActionListener(e -> decide(t, m, TimeOffRequest.Status.APPROVED));
        deny.addActionListener(e -> decide(t, m, TimeOffRequest.Status.DENIED));

        JPanel bar = toolbar(label("Show:", 14, true, INK), filter, approve, deny);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(counts);
        page.add(bar, BorderLayout.NORTH);
        page.add(scroll(t), BorderLayout.CENTER);
        return page;
    }

    private void decide(JTable t, DefaultTableModel m, TimeOffRequest.Status status) {
        int row = t.getSelectedRow();
        if (row < 0) { info("Select a request in the table first."); return; }
        int id = (Integer) m.getValueAt(t.convertRowIndexToModel(row), 0);
        TimeOffRequest req = null;
        for (TimeOffRequest r : store.requests) if (r.getId() == id) req = r;
        if (req == null) return;
        if (req.getStatus() != TimeOffRequest.Status.PENDING) {
            info("This request has already been " + req.getStatus().name().toLowerCase() + ".");
            return;
        }
        req.setStatus(status);
        store.save();
        run(hrRefresh);
    }

    // ---- Schedules --------------------------------------------------------

    private JPanel schedulesTab() {
        JPanel page = page();
        DefaultTableModel m = model("ID", "Employee", "Shift", "Days off");
        JTable t = table(m);
        hrRefresh.add(() -> {
            m.setRowCount(0);
            for (Schedule s : store.schedules) {
                m.addRow(new Object[]{s.getEmpId(), store.nameOf(s.getEmpId()),
                        s.getShiftStart() + " - " + s.getShiftEnd(), s.daysOffText()});
            }
        });
        Runnable edit = () -> {
            int row = t.getSelectedRow();
            if (row < 0) { info("Select an employee in the table first."); return; }
            int id = (Integer) m.getValueAt(t.convertRowIndexToModel(row), 0);
            Schedule s = store.findSchedule(id);
            if (s != null) editSchedule(s);
        };
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) edit.run(); }
        });
        JButton btn = button("Edit selected schedule", TEAL);
        btn.addActionListener(e -> edit.run());
        page.add(toolbar(btn, label("Tip: double-click a row to edit it.", 14, false, MUTED)), BorderLayout.NORTH);
        page.add(scroll(t), BorderLayout.CENTER);
        return page;
    }

    private void editSchedule(Schedule s) {
        JTextField start = field(s.getShiftStart().toString(), 10);
        JTextField end = field(s.getShiftEnd().toString(), 10);
        JCheckBox[] days = new JCheckBox[7];
        JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
        p.add(label("Shift start (HH:mm)", 13, true, MUTED));
        p.add(start);
        p.add(label("Shift end (HH:mm)", 13, true, MUTED));
        p.add(end);
        p.add(label("Days off", 13, true, MUTED));
        for (int i = 0; i < 7; i++) {
            DayOfWeek d = DayOfWeek.of(i + 1);
            days[i] = new JCheckBox(d.getDisplayName(TextStyle.FULL, Locale.ENGLISH), s.getDaysOff().contains(d));
            p.add(days[i]);
        }
        while (true) {
            int res = JOptionPane.showConfirmDialog(this, p, "Edit schedule - " + store.nameOf(s.getEmpId()),
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
            if (res != JOptionPane.OK_OPTION) return;
            try {
                LocalTime a = time(start.getText(), "Shift start");
                LocalTime b = time(end.getText(), "Shift end");
                if (!b.isAfter(a)) throw new IllegalArgumentException("Shift end must be after shift start.");
                Set<DayOfWeek> off = EnumSet.noneOf(DayOfWeek.class);
                for (int i = 0; i < 7; i++) if (days[i].isSelected()) off.add(DayOfWeek.of(i + 1));
                s.update(a, b, off);
                store.save();
                run(hrRefresh);
                return;
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        }
    }

    // ---- Time logs --------------------------------------------------------

    private JPanel timeLogsTab() {
        JPanel page = page();
        DefaultTableModel m = model("Employee", "Date", "Day", "In", "Out", "Hours");
        JTable t = table(m);
        NameCombo who = new NameCombo("All employees");
        JLabel total = label("", 14, false, MUTED);
        List<WorkLog> shown = new ArrayList<>();

        Runnable rows = () -> {
            shown.clear();
            m.setRowCount(0);
            Employee f = who.employee();
            List<WorkLog> src = new ArrayList<>(store.logs);
            src.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            double sum = 0;
            for (WorkLog w : src) {
                if (f != null && w.getEmpId() != f.getId()) continue;
                shown.add(w);
                sum += w.hours();
                m.addRow(new Object[]{store.nameOf(w.getEmpId()), w.getDate().toString(), dayName(w.getDate()),
                        w.getStart().toString(), w.getEnd().toString(), w.hours()});
            }
            total.setText(shown.size() + " entries   ·   " + String.format("%.2f", sum) + " hours");
        };
        who.addActionListener(e -> { if (!who.busy) rows.run(); });
        hrRefresh.add(() -> { who.reload(); rows.run(); });

        Runnable correct = () -> {
            int row = t.getSelectedRow();
            if (row < 0) { info("Select a log entry in the table first."); return; }
            WorkLog w = shown.get(t.convertRowIndexToModel(row));
            JTextField start = field(w.getStart().toString(), 10);
            JTextField end = field(w.getEnd().toString(), 10);
            JPanel p = new JPanel(new GridLayout(0, 1, 0, 4));
            p.add(label("Start time (HH:mm)", 13, true, MUTED));
            p.add(start);
            p.add(label("End time (HH:mm)", 13, true, MUTED));
            p.add(end);
            while (true) {
                int res = JOptionPane.showConfirmDialog(this, p,
                        "Correct log - " + store.nameOf(w.getEmpId()) + ", " + w.getDate(),
                        JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
                if (res != JOptionPane.OK_OPTION) return;
                try {
                    LocalTime a = time(start.getText(), "Start time");
                    LocalTime b = time(end.getText(), "End time");
                    if (!b.isAfter(a)) throw new IllegalArgumentException("End time must be after start time.");
                    w.correct(a, b);
                    store.save();
                    run(hrRefresh);
                    return;
                } catch (IllegalArgumentException ex) {
                    error(ex.getMessage());
                }
            }
        };
        JButton btn = button("Correct selected entry", TEAL);
        btn.addActionListener(e -> correct.run());
        t.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { if (e.getClickCount() == 2) correct.run(); }
        });
        JPanel bar = toolbar(label("Employee:", 14, true, INK), who, btn);
        bar.add(Box.createHorizontalStrut(12));
        bar.add(total);
        page.add(bar, BorderLayout.NORTH);
        page.add(scroll(t), BorderLayout.CENTER);
        return page;
    }

    // ---- Reports ----------------------------------------------------------

    private JPanel reportsTab() {
        JPanel page = page();
        NameCombo who = new NameCombo("All employees");
        JTextField from = field(LocalDate.now().minusDays(29).toString(), 10);
        JTextField to = field(LocalDate.now().toString(), 10);
        JButton go = button("Generate report", TEAL);
        JLabel summary = label("", 14, false, MUTED);

        DefaultTableModel logsModel = model("Employee", "Date", "In", "Out", "Hours");
        JTable logsTable = table(logsModel);
        DefaultTableModel offModel = model("Employee", "From", "To", "Days", "Status");
        JTable offTable = table(offModel);
        BarChart chart = new BarChart();
        JScrollPane chartScroll = new JScrollPane(chart);
        chartScroll.setBorder(new LineBorder(LINE));
        chartScroll.getViewport().setBackground(Color.WHITE);

        Runnable generate = () -> report(who, from, to, logsModel, offModel, chart, summary, false);
        go.addActionListener(e -> generate.run());
        who.addActionListener(e -> { if (!who.busy) generate.run(); });
        hrRefresh.add(() -> { who.reload(); report(who, from, to, logsModel, offModel, chart, summary, true); });

        JPanel bar = toolbar(label("Employee:", 14, true, INK), who, label("From:", 14, true, INK), from,
                label("To:", 14, true, INK), to, go);
        page.add(bar, BorderLayout.NORTH);

        JPanel rightTop = section("Total hours worked", chartScroll);
        JPanel rightBottom = section("Time off in this range", scroll(offTable));
        JSplitPane right = new JSplitPane(JSplitPane.VERTICAL_SPLIT, rightTop, rightBottom);
        right.setResizeWeight(0.5);
        right.setBorder(null);
        JSplitPane main = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, section("Time logs", scroll(logsTable)), right);
        main.setResizeWeight(0.5);
        main.setBorder(null);
        page.add(main, BorderLayout.CENTER);
        page.add(summary, BorderLayout.SOUTH);
        return page;
    }

    private void report(NameCombo who, JTextField fromF, JTextField toF, DefaultTableModel logsModel,
                        DefaultTableModel offModel, BarChart chart, JLabel summary, boolean quiet) {
        LocalDate from, to;
        try {
            from = date(fromF.getText(), "From date");
            to = date(toF.getText(), "To date");
            if (to.isBefore(from)) throw new IllegalArgumentException("The 'To' date cannot be before the 'From' date.");
        } catch (IllegalArgumentException ex) {
            if (!quiet) error(ex.getMessage());
            return;
        }
        Employee f = who.employee();
        logsModel.setRowCount(0);
        offModel.setRowCount(0);
        Map<String, Double> totals = new LinkedHashMap<>();
        List<WorkLog> src = new ArrayList<>(store.logs);
        src.sort((a, b) -> a.getDate().compareTo(b.getDate()));
        double all = 0;
        int count = 0;
        for (WorkLog w : src) {
            if (f != null && w.getEmpId() != f.getId()) continue;
            if (w.getDate().isBefore(from) || w.getDate().isAfter(to)) continue;
            String name = store.nameOf(w.getEmpId());
            logsModel.addRow(new Object[]{name, w.getDate().toString(), w.getStart().toString(),
                    w.getEnd().toString(), w.hours()});
            totals.merge(name, w.hours(), Double::sum);
            all += w.hours();
            count++;
        }
        int off = 0;
        for (TimeOffRequest r : store.requests) {
            if (f != null && r.getEmpId() != f.getId()) continue;
            if (!r.overlaps(from, to)) continue;
            offModel.addRow(new Object[]{store.nameOf(r.getEmpId()), r.getFrom().toString(), r.getTo().toString(),
                    (int) r.days(), r.getStatus().name()});
            off++;
        }
        chart.setData(totals);
        summary.setText(count + " time logs, " + String.format("%.2f", all) + " hours in total, " + off
                + " time-off requests between " + from + " and " + to + ".");
    }

    // ---- Employees --------------------------------------------------------

    private JPanel employeesTab() {
        JPanel page = page();
        DefaultTableModel m = model("ID", "Name", "Shift", "Days off", "Logs", "Total hours", "Requests");
        JTable t = table(m);
        hrRefresh.add(() -> {
            m.setRowCount(0);
            for (Employee e : store.employees) {
                Schedule s = store.findSchedule(e.getId());
                int logs = 0, reqs = 0;
                double hours = 0;
                for (WorkLog w : store.logs) if (w.getEmpId() == e.getId()) { logs++; hours += w.hours(); }
                for (TimeOffRequest r : store.requests) if (r.getEmpId() == e.getId()) reqs++;
                m.addRow(new Object[]{e.getId(), e.getName(),
                        s == null ? "-" : s.getShiftStart() + " - " + s.getShiftEnd(),
                        s == null ? "-" : s.daysOffText(), logs, hours, reqs});
            }
        });
        JTextField name = field("", 18);
        JButton add = button("Add employee", TEAL);
        Runnable doAdd = () -> {
            String n = clean(name.getText());
            if (n.isEmpty()) { error("Please type a name."); return; }
            if (store.findEmployeeByName(n) != null) { error("An employee with that name already exists."); return; }
            Employee e = store.addEmployee(n);
            store.save();
            name.setText("");
            run(hrRefresh);
            info(e.getName() + " was added with ID " + e.getId() + " and the default schedule (09:00-17:00, weekends off).");
        };
        add.addActionListener(e -> doAdd.run());
        name.addActionListener(e -> doAdd.run());
        page.add(toolbar(label("New employee name:", 14, true, INK), name, add), BorderLayout.NORTH);
        page.add(scroll(t), BorderLayout.CENTER);
        return page;
    }

    // =================================================================== employee dashboard

    private JPanel buildEmployee(Employee emp) {
        List<Runnable> refreshers = new ArrayList<>();
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Log Hours", logHoursTab(emp, refreshers));
        tabs.addTab("Request Time Off", requestTab(emp, refreshers, tabs));
        tabs.addTab("My Requests", myRequestsTab(emp, refreshers));
        tabs.addTab("My Schedule", myScheduleTab(emp));

        JPanel p = new JPanel(new BorderLayout());
        p.add(header("Welcome, " + emp.getName(), "Employee dashboard", "Log out",
                () -> { store.save(); showLogin(); }), BorderLayout.NORTH);
        p.add(tabs, BorderLayout.CENTER);
        run(refreshers);
        return p;
    }

    private JPanel logHoursTab(Employee emp, List<Runnable> refreshers) {
        JPanel page = page();
        JPanel form = formCard("Log work hours");
        JTextField dateF = field(LocalDate.now().toString(), 12);
        JTextField startF = field("09:00", 12);
        JTextField endF = field("17:00", 12);
        addField(form, "Date (yyyy-MM-dd)", dateF);
        addField(form, "Start time (HH:mm)", startF);
        addField(form, "End time (HH:mm)", endF);
        JButton save = button("Save hours", TEAL);
        addButton(form, save);

        DefaultTableModel m = model("Date", "Day", "In", "Out", "Hours");
        JTable t = table(m);
        JLabel note = label("", 14, false, MUTED);
        refreshers.add(() -> {
            m.setRowCount(0);
            List<WorkLog> mine = new ArrayList<>();
            for (WorkLog w : store.logs) if (w.getEmpId() == emp.getId()) mine.add(w);
            mine.sort((a, b) -> b.getDate().compareTo(a.getDate()));
            double week = 0, all = 0;
            LocalDate cutoff = LocalDate.now().minusDays(6);
            for (WorkLog w : mine) {
                all += w.hours();
                if (!w.getDate().isBefore(cutoff)) week += w.hours();
                m.addRow(new Object[]{w.getDate().toString(), dayName(w.getDate()), w.getStart().toString(),
                        w.getEnd().toString(), w.hours()});
            }
            note.setText("Last 7 days: " + String.format("%.2f", week) + " h   ·   All entries: "
                    + String.format("%.2f", all) + " h");
        });
        save.addActionListener(ev -> {
            try {
                LocalDate d = date(dateF.getText(), "Date");
                LocalTime a = time(startF.getText(), "Start time");
                LocalTime b = time(endF.getText(), "End time");
                if (!b.isAfter(a)) throw new IllegalArgumentException("End time must be after start time.");
                WorkLog w = new WorkLog(emp.getId(), d, a, b);
                store.logs.add(w);
                store.save();
                run(refreshers);
                info(String.format("Saved %.2f hours for %s.", w.hours(), d));
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        page.add(west(form, 360), BorderLayout.WEST);
        page.add(section("My recent hours", scroll(t), note), BorderLayout.CENTER);
        return page;
    }

    private JPanel requestTab(Employee emp, List<Runnable> refreshers, JTabbedPane tabs) {
        JPanel page = page();
        JPanel form = formCard("Request time off");
        JTextField fromF = field(LocalDate.now().plusDays(7).toString(), 12);
        JTextField toF = field(LocalDate.now().plusDays(8).toString(), 12);
        JTextField reasonF = field("", 12);
        addField(form, "From (yyyy-MM-dd)", fromF);
        addField(form, "To (yyyy-MM-dd)", toF);
        addField(form, "Reason", reasonF);
        JButton send = button("Submit request", TEAL);
        addButton(form, send);
        send.addActionListener(ev -> {
            try {
                LocalDate a = date(fromF.getText(), "From date");
                LocalDate b = date(toF.getText(), "To date");
                if (b.isBefore(a)) throw new IllegalArgumentException("The 'To' date cannot be before the 'From' date.");
                String reason = clean(reasonF.getText());
                if (reason.isEmpty()) throw new IllegalArgumentException("Please give a short reason.");
                TimeOffRequest r = new TimeOffRequest(store.nextRequestId(), emp.getId(), a, b, reason,
                        TimeOffRequest.Status.PENDING);
                store.requests.add(r);
                store.save();
                reasonF.setText("");
                run(refreshers);
                info("Request #" + r.getId() + " submitted for " + r.days() + " day(s). Status: PENDING.");
                tabs.setSelectedIndex(2);
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        });
        JPanel hint = new JPanel(new BorderLayout());
        hint.setOpaque(false);
        hint.setBorder(new EmptyBorder(8, 8, 0, 0));
        hint.add(label("<html>Your request starts as <b>PENDING</b>.<br>HR will approve or deny it, and you can follow it under <b>My Requests</b>.</html>",
                15, false, MUTED), BorderLayout.NORTH);
        page.add(west(form, 360), BorderLayout.WEST);
        page.add(hint, BorderLayout.CENTER);
        return page;
    }

    private JPanel myRequestsTab(Employee emp, List<Runnable> refreshers) {
        JPanel page = page();
        DefaultTableModel m = model("ID", "From", "To", "Days", "Status", "Reason");
        JTable t = table(m);
        JLabel counts = label("", 14, false, MUTED);
        refreshers.add(() -> {
            m.setRowCount(0);
            int p = 0, a = 0, d = 0;
            List<TimeOffRequest> mine = new ArrayList<>();
            for (TimeOffRequest r : store.requests) if (r.getEmpId() == emp.getId()) mine.add(r);
            mine.sort((x, y) -> y.getFrom().compareTo(x.getFrom()));
            for (TimeOffRequest r : mine) {
                if (r.getStatus() == TimeOffRequest.Status.PENDING) p++;
                else if (r.getStatus() == TimeOffRequest.Status.APPROVED) a++;
                else d++;
                m.addRow(new Object[]{r.getId(), r.getFrom().toString(), r.getTo().toString(), (int) r.days(),
                        r.getStatus().name(), r.getReason()});
            }
            counts.setText("Pending " + p + "   ·   Approved " + a + "   ·   Denied " + d);
        });
        page.add(section("My time-off requests", scroll(t), counts), BorderLayout.CENTER);
        return page;
    }

    private JPanel myScheduleTab(Employee emp) {
        JPanel page = page();
        JTextField fromF = field(LocalDate.now().toString(), 10);
        JTextField toF = field(LocalDate.now().plusDays(13).toString(), 10);
        JButton show = button("Show schedule", TEAL);
        DefaultTableModel m = model("Date", "Day", "Work schedule");
        JTable t = table(m);
        JLabel shift = label("", 14, false, MUTED);

        Runnable rows = () -> {
            Schedule s = store.findSchedule(emp.getId());
            m.setRowCount(0);
            if (s == null) { shift.setText("No schedule has been set for you yet."); return; }
            shift.setText("Regular shift " + s.getShiftStart() + " - " + s.getShiftEnd() + "   ·   Days off: " + s.daysOffText());
            try {
                LocalDate a = date(fromF.getText(), "From date");
                LocalDate b = date(toF.getText(), "To date");
                if (b.isBefore(a)) throw new IllegalArgumentException("The 'To' date cannot be before the 'From' date.");
                if (b.isAfter(a.plusDays(90))) throw new IllegalArgumentException("Please choose a range of at most 90 days.");
                for (LocalDate d = a; !d.isAfter(b); d = d.plusDays(1)) {
                    String info;
                    if (approvedLeave(emp.getId(), d)) info = "Approved time off";
                    else if (s.getDaysOff().contains(d.getDayOfWeek())) info = "Day off";
                    else info = s.getShiftStart() + " - " + s.getShiftEnd();
                    m.addRow(new Object[]{d.toString(), d.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH), info});
                }
            } catch (IllegalArgumentException ex) {
                error(ex.getMessage());
            }
        };
        show.addActionListener(e -> rows.run());
        page.add(toolbar(label("From:", 14, true, INK), fromF, label("To:", 14, true, INK), toF, show),
                BorderLayout.NORTH);
        page.add(section("My schedule", scroll(t), shift), BorderLayout.CENTER);
        rows.run();
        return page;
    }

    private boolean approvedLeave(int empId, LocalDate d) {
        for (TimeOffRequest r : store.requests) {
            if (r.getEmpId() == empId && r.getStatus() == TimeOffRequest.Status.APPROVED && r.covers(d)) return true;
        }
        return false;
    }

    // =================================================================== helpers: data

    private static String clean(String s) {
        return s.replace(",", ";").trim();
    }

    private static String dayName(LocalDate d) {
        return d.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
    }

    private static LocalDate date(String s, String what) {
        try {
            return LocalDate.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(what + " must look like 2026-10-12 (yyyy-MM-dd).");
        }
    }

    private static LocalTime time(String s, String what) {
        try {
            return LocalTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(what + " must be a 24-hour time like 09:30 (HH:mm).");
        }
    }

    private void error(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Please check", JOptionPane.ERROR_MESSAGE);
    }

    private void info(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Employee Time Management", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Drop-down of employee names with an "all" entry on top. */
    private class NameCombo extends JComboBox<String> {
        private final String all;
        boolean busy;

        NameCombo(String all) {
            this.all = all;
            reload();
        }

        void reload() {
            busy = true;
            Object keep = getSelectedItem();
            removeAllItems();
            addItem(all);
            for (Employee e : store.employees) addItem(e.getName());
            if (keep != null) setSelectedItem(keep);
            busy = false;
        }

        Employee employee() {
            return getSelectedIndex() <= 0 ? null : store.findEmployeeByName((String) getSelectedItem());
        }
    }

    // =================================================================== helpers: look and feel

    private static JLabel label(String text, int size, boolean bold, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(l.getFont().deriveFont(bold ? Font.BOLD : Font.PLAIN, (float) size));
        l.setForeground(color);
        return l;
    }

    private static JTextField field(String text, int cols) {
        JTextField f = new JTextField(text, cols);
        f.setBorder(BorderFactoryHelper.input());
        return f;
    }

    private static JButton button(String text, Color bg) {
        JButton b = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        b.setFont(b.getFont().deriveFont(Font.BOLD, 14f));
        b.setForeground(Color.WHITE);
        b.setBackground(bg);
        b.setOpaque(false);
        b.setContentAreaFilled(false);
        b.setBorderPainted(false);
        b.setFocusPainted(false);
        b.setBorder(new EmptyBorder(9, 18, 9, 18));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { b.setBackground(bg.brighter()); }
            @Override public void mouseExited(MouseEvent e) { b.setBackground(bg); }
        });
        return b;
    }

    private static JPanel page() {
        JPanel p = new JPanel(new BorderLayout(18, 14));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(18, 20, 18, 20));
        return p;
    }

    private static JPanel toolbar(Component... parts) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        p.setOpaque(false);
        for (Component c : parts) p.add(c);
        return p;
    }

    private static JPanel section(String title, JComponent body, JLabel... note) {
        JPanel p = new JPanel(new BorderLayout(0, 8));
        p.setOpaque(false);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(label(title, 16, true, INK), BorderLayout.WEST);
        if (note.length > 0) top.add(note[0], BorderLayout.EAST);
        p.add(top, BorderLayout.NORTH);
        p.add(body, BorderLayout.CENTER);
        return p;
    }

    private static JPanel west(JComponent c, int width) {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.add(c, BorderLayout.NORTH);
        p.setPreferredSize(new Dimension(width, 10));
        return p;
    }

    private static JPanel formPanel() {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(CARD);
        p.setBorder(new EmptyBorder(14, 18, 18, 18));
        return p;
    }

    private static JPanel formCard(String title) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBackground(CARD);
        p.setBorder(new javax.swing.border.CompoundBorder(new LineBorder(LINE), new EmptyBorder(18, 20, 22, 20)));
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.gridy = 0; g.weightx = 1; g.anchor = GridBagConstraints.WEST;
        p.add(label(title, 18, true, INK), g);
        return p;
    }

    private static void addField(JPanel p, String text, JComponent c) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST;
        g.gridy = p.getComponentCount();
        g.insets = new Insets(12, 0, 3, 0);
        p.add(label(text, 13, true, MUTED), g);
        g.gridy = p.getComponentCount();
        g.insets = new Insets(0, 0, 2, 0);
        p.add(c, g);
    }

    private static void addButton(JPanel p, JButton b) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = 0; g.weightx = 1; g.fill = GridBagConstraints.HORIZONTAL;
        g.gridy = p.getComponentCount();
        g.insets = new Insets(18, 0, 0, 0);
        p.add(b, g);
    }

    private static JPanel header(String title, String subtitle, String buttonText, Runnable action) {
        JPanel h = new JPanel(new BorderLayout());
        h.setBackground(INK);
        h.setBorder(new EmptyBorder(14, 24, 14, 24));
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.add(label(title, 22, true, Color.WHITE));
        left.add(label(subtitle, 13, false, TEAL_BRIGHT));
        JButton b = button(buttonText, SLATE);
        b.addActionListener(e -> action.run());
        JPanel right = new JPanel(new GridBagLayout());
        right.setOpaque(false);
        right.add(b);
        h.add(left, BorderLayout.WEST);
        h.add(right, BorderLayout.EAST);
        return h;
    }

    private static DefaultTableModel model(String... cols) {
        return new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) {
                return getRowCount() > 0 && getValueAt(0, c) != null ? getValueAt(0, c).getClass() : Object.class;
            }
        };
    }

    private static JTable table(DefaultTableModel m) {
        JTable t = new JTable(m);
        t.setRowHeight(32);
        t.setShowVerticalLines(false);
        t.setGridColor(LINE);
        t.setSelectionBackground(new Color(0xCFEFE7));
        t.setSelectionForeground(INK);
        t.setFillsViewportHeight(true);
        t.setAutoCreateRowSorter(true);
        CellRenderer r = new CellRenderer();
        for (Class<?> k : new Class<?>[]{Object.class, Integer.class, Double.class, Long.class}) t.setDefaultRenderer(k, r);
        JTableHeader h = t.getTableHeader();
        h.setBackground(INK);
        h.setForeground(Color.WHITE);
        h.setFont(h.getFont().deriveFont(Font.BOLD, 13f));
        h.setPreferredSize(new Dimension(0, 36));
        h.setReorderingAllowed(false);
        return t;
    }

    private static JScrollPane scroll(JComponent c) {
        JScrollPane sp = new JScrollPane(c);
        sp.setBorder(new LineBorder(LINE));
        sp.getViewport().setBackground(Color.WHITE);
        return sp;
    }

    /** Striped rows, coloured status words. */
    private static class CellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
            String text = v instanceof Double ? String.format("%.2f", (Double) v) : String.valueOf(v);
            super.getTableCellRendererComponent(t, text, sel, false, r, c);
            setBorder(new EmptyBorder(0, 10, 0, 10));
            setHorizontalAlignment(v instanceof Number ? CENTER : LEFT);
            if (!sel) {
                setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF7F5EF));
                setForeground(INK);
                if (text.equals("APPROVED")) { setForeground(GREEN); setFont(getFont().deriveFont(Font.BOLD)); }
                else if (text.equals("PENDING")) { setForeground(AMBER); setFont(getFont().deriveFont(Font.BOLD)); }
                else if (text.equals("DENIED")) { setForeground(RED); setFont(getFont().deriveFont(Font.BOLD)); }
                else if (text.equals("Day off")) setForeground(MUTED);
                else if (text.equals("Approved time off")) { setForeground(AMBER); setFont(getFont().deriveFont(Font.BOLD)); }
            }
            return this;
        }
    }

    /** Horizontal bar chart of total hours per employee. */
    private static class BarChart extends JPanel {
        private Map<String, Double> data = new LinkedHashMap<>();

        BarChart() { setBackground(Color.WHITE); }

        void setData(Map<String, Double> d) {
            List<Map.Entry<String, Double>> list = new ArrayList<>(d.entrySet());
            list.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            data = new LinkedHashMap<>();
            for (Map.Entry<String, Double> e : list) data.put(e.getKey(), e.getValue());
            setPreferredSize(new Dimension(300, 24 + Math.max(1, data.size()) * 34));
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0) {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.setFont(getFont().deriveFont(Font.PLAIN, 13f));
            if (data.isEmpty()) {
                g.setColor(MUTED);
                g.drawString("No time logs in this range.", 14, 30);
                g.dispose();
                return;
            }
            double max = 0;
            for (double v : data.values()) max = Math.max(max, v);
            int labelW = 100, valueW = 64, barX = labelW + 14;
            int avail = Math.max(40, getWidth() - barX - valueW - 16);
            int y = 12;
            for (Map.Entry<String, Double> e : data.entrySet()) {
                g.setColor(INK);
                g.drawString(e.getKey(), 12, y + 18);
                int w = max == 0 ? 0 : (int) Math.round(avail * e.getValue() / max);
                g.setColor(TEAL);
                g.fillRoundRect(barX, y + 3, Math.max(w, 2), 22, 8, 8);
                g.setColor(INK);
                g.drawString(String.format("%.1f h", e.getValue()), barX + w + 8, y + 18);
                y += 34;
            }
            g.dispose();
        }
    }

    /** Tiny helper so field borders look the same everywhere. */
    private static class BorderFactoryHelper {
        static javax.swing.border.Border input() {
            return new javax.swing.border.CompoundBorder(new LineBorder(LINE), new EmptyBorder(6, 8, 6, 8));
        }
    }

    // =================================================================== start-up

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.put("swing.boldMetal", Boolean.FALSE);
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                String family = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getAvailableFontFamilyNames()).contains("Segoe UI") ? "Segoe UI" : Font.SANS_SERIF;
                for (Object key : java.util.Collections.list(UIManager.getDefaults().keys())) {
                    if (key.toString().endsWith(".font")) {
                        UIManager.put(key, new FontUIResource(family, Font.PLAIN, 14));
                    }
                }
            } catch (Exception ignored) {
                // fall back to the default look
            }
            new GuiApp().setVisible(true);
        });
    }
}
