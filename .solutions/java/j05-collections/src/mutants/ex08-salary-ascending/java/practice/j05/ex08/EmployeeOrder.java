package practice.j05.ex08;

import java.util.Comparator;

/** The orders in which the payroll report lists employees. */
public final class EmployeeOrder {

    private EmployeeOrder() {
    }

    /**
     * The order of the payroll report:
     * <ol>
     *   <li>by department name, A to Z ({@code String.compareTo}), with employees who have no department
     *       ({@code null}) last;</li>
     *   <li>within a department, highest salary first;</li>
     *   <li>for equal salaries, by name, A to Z.</li>
     * </ol>
     */
    public static Comparator<Employee> payroll() {
        // Each thenComparing only decides when everything before it is equal.
        // nullsLast wraps the department comparator: null departments sort after every name, and two nulls are
        // equal, so the salary and name rules decide between them. Without it, compareTo would throw a
        // NullPointerException.
        return Comparator.comparing(Employee::department, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Comparator.comparingLong(Employee::salary))
                .thenComparing(Employee::name);
    }

    /**
     * Highest salary first. Employees with the same salary are equal for this comparator, so a stable sort,
     * such as {@code List.sort}, keeps them in the order they had before.
     */
    public static Comparator<Employee> bySalaryDescending() {
        // reversed() applies only to the comparator it is called on. Here that's the whole comparator,
        // which is what we want. In payroll() it is called on the salary comparator alone, on purpose.
        return Comparator.comparingLong(Employee::salary).reversed();
    }
}
