package practice.j05.ex08;

import java.util.Objects;

/**
 * An employee in the payroll report.
 *
 * <p>This record is finished: you don't need to change it.
 *
 * @param name       the employee's name; never {@code null}
 * @param department the department's name, or {@code null} for a new employee who isn't assigned to a
 *                   department yet
 * @param salary     the yearly salary; never negative
 */
public record Employee(String name, String department, long salary) {

    public Employee {
        Objects.requireNonNull(name, "name");
        if (salary < 0) {
            throw new IllegalArgumentException("salary must not be negative, but was " + salary);
        }
    }
}
