
class Employee implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private final int id;
    private final String name;
    Employee(int id, String name) { this.id = id; this.name = name; }
    int getId() { return id; }
    String getName() { return name; }
}
