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
        // TODO: J05-08
        throw new UnsupportedOperationException("TODO: J05-08");
    }

    /**
     * Highest salary first. Employees with the same salary are equal for this comparator, so a stable sort,
     * such as {@code List.sort}, keeps them in the order they had before.
     */
    public static Comparator<Employee> bySalaryDescending() {
        // TODO: J05-08
        throw new UnsupportedOperationException("TODO: J05-08");
    }
}
