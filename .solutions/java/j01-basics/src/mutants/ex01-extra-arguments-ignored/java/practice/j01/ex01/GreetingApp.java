package practice.j01.ex01;

/**
 * A command-line program that prints a greeting.
 *
 * <pre>
 * java GreetingApp.java Minji 9            prints  Good morning, Minji!
 * java GreetingApp.java "Mary Ann" 20      prints  Good evening, Mary Ann!
 * </pre>
 *
 * <h2>Rules for {@link #main(String[])}</h2>
 * <ul>
 *   <li>With exactly two arguments, a name and an hour, it prints the result of {@link Greeting#format} as one
 *       line to standard output ({@code System.out}).</li>
 *   <li>With any other number of arguments, it prints the line {@value #USAGE} to standard error
 *       ({@code System.err}) and nothing to standard output.</li>
 *   <li>The hour is converted with {@link Integer#parseInt(String)}. Text that isn't a whole number, such as
 *       {@code "nine"}, makes {@code main} throw {@link NumberFormatException}. An hour outside 0 to 23 makes it
 *       throw the {@link IllegalArgumentException} from {@code Greeting.format}. In both cases nothing is
 *       printed to standard output.</li>
 * </ul>
 */
public final class GreetingApp {

    /** The line {@code main} prints to standard error when it gets the wrong number of arguments. */
    public static final String USAGE = "Usage: java GreetingApp <name> <hour>";

    private GreetingApp() {
    }

    /** Prints the greeting for {@code args[0]} (the name) and {@code args[1]} (the hour). */
    public static void main(String[] args) {
        if (args.length < 2) {
            // Error messages go to standard error, so they don't mix with the real output, for example when the
            // output is redirected to a file. A real command-line tool would also stop with System.exit(2) to
            // report the failure, but that would stop the JVM that runs the tests, too.
            System.err.println(USAGE);
            return;
        }
        String name = args[0];
        int hour = Integer.parseInt(args[1]); // NumberFormatException for text like "nine"
        System.out.println(Greeting.format(name, hour));
    }
}
