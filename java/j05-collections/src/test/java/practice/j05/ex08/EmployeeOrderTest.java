package practice.j05.ex08;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("J05-08 · Sorting and binary search")
class EmployeeOrderTest {

    private static final Employee AHN = new Employee("Ahn", "Design", 85_000);
    private static final Employee LEE = new Employee("Lee", "Engineering", 120_000);
    private static final Employee KIM = new Employee("Kim", "Engineering", 90_000);
    private static final Employee PARK = new Employee("Park", "Engineering", 90_000);
    private static final Employee CHOI = new Employee("Choi", "Sales", 70_000);
    private static final Employee HAN = new Employee("Han", null, 80_000);
    private static final Employee JUNG = new Employee("Jung", null, 60_000);

    /** Sorts a modifiable copy of {@code employees} and returns the names in the new order. */
    private static List<String> sortedNames(List<Employee> employees, Comparator<Employee> order) {
        List<Employee> sorted = new ArrayList<>(employees);
        sorted.sort(order);
        return sorted.stream().map(Employee::name).toList();
    }

    @Nested
    @DisplayName("Basic")
    class Basic {

        @Test
        @DisplayName("payroll() sorts by department, then highest salary first, then name")
        void payrollOrder() {
            var employees = List.of(JUNG, PARK, CHOI, LEE, HAN, KIM, AHN);

            assertThat(sortedNames(employees, EmployeeOrder.payroll()))
                    .containsExactly("Ahn", "Lee", "Kim", "Park", "Choi", "Han", "Jung");
        }

        @Test
        @DisplayName("payroll() puts the highest salary first within a department")
        void highestSalaryFirstInDepartment() {
            var employees = List.of(KIM, LEE);

            assertThat(sortedNames(employees, EmployeeOrder.payroll())).containsExactly("Lee", "Kim");
        }

        @Test
        @DisplayName("bySalaryDescending() puts the highest salary first")
        void bySalaryDescending() {
            var employees = List.of(CHOI, LEE, AHN);

            assertThat(sortedNames(employees, EmployeeOrder.bySalaryDescending()))
                    .containsExactly("Lee", "Ahn", "Choi");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("payroll() puts employees without a department last, whatever their salary")
        void noDepartmentLast() {
            var newHire = new Employee("Amy", null, 1_000_000);
            var zookeeper = new Employee("Zoe", "Zoo", 1);

            assertThat(sortedNames(List.of(newHire, zookeeper), EmployeeOrder.payroll()))
                    .containsExactly("Zoe", "Amy");
        }

        @Test
        @DisplayName("payroll() orders employees without a department by salary, then name")
        void noDepartmentBySalaryThenName() {
            var bora = new Employee("Bora", null, 60_000);

            assertThat(sortedNames(List.of(JUNG, bora, HAN), EmployeeOrder.payroll()))
                    .containsExactly("Han", "Bora", "Jung");
        }

        @Test
        @DisplayName("payroll() uses the name only when the department and the salary are equal")
        void nameIsLastTieBreaker() {
            assertThat(sortedNames(List.of(PARK, KIM), EmployeeOrder.payroll())).containsExactly("Kim", "Park");
        }

        @Test
        @DisplayName("compares salaries of 0 and Long.MAX_VALUE without overflowing")
        void extremeSalaries() {
            var intern = new Employee("Intern", "Lab", 0);
            var founder = new Employee("Founder", "Lab", Long.MAX_VALUE);
            var analyst = new Employee("Analyst", "Lab", 1);

            assertThat(sortedNames(List.of(intern, founder, analyst), EmployeeOrder.bySalaryDescending()))
                    .containsExactly("Founder", "Analyst", "Intern");
            assertThat(sortedNames(List.of(intern, founder, analyst), EmployeeOrder.payroll()))
                    .containsExactly("Founder", "Analyst", "Intern");
        }

        @Test
        @DisplayName("sorting is stable: equal salaries keep the order they had before")
        void stableSort() {
            var employees = List.of(PARK, KIM, LEE, AHN); // Park and Kim both earn 90,000

            assertThat(sortedNames(employees, EmployeeOrder.bySalaryDescending()))
                    .containsExactly("Lee", "Park", "Kim", "Ahn");
        }

        @Test
        @DisplayName("a stable sort by name, then by salary, gives names A to Z among equal salaries")
        void stableSortTwice() {
            List<Employee> employees = new ArrayList<>(List.of(PARK, LEE, KIM, AHN));

            employees.sort(Comparator.comparing(Employee::name));
            employees.sort(EmployeeOrder.bySalaryDescending());

            assertThat(employees).containsExactly(LEE, KIM, PARK, AHN);
        }
    }
}
