package practice.j05.ex01;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.TreeSet;

/**
 * J05-01 · Collection traps and Big-O.
 *
 * <p>Each method is a small puzzle. <strong>Don't run anything yet.</strong> Read each puzzle, decide what it
 * returns, and write your prediction in {@link Answers}. Then run the tests: a test fails when a prediction
 * was wrong, and the module README explains every answer.
 *
 * <p>A puzzle that returns an {@link Outcome} runs some code and reports how it ended: normally, or with an
 * exception. The Big-O questions at the bottom have no code at all.
 */
final class Puzzles {

    private Puzzles() {
    }

    // ---- Unmodifiable and fixed-size lists -------------------------------------------------------

    static Outcome addToListOf() {
        List<String> names = List.of("Ann", "Bob");
        return outcomeOf(() -> names.add("Cid"));
    }

    static String setThroughArraysAsList() {
        String[] array = {"a", "b", "c"};
        List<String> list = Arrays.asList(array);
        list.set(1, "z");
        return Arrays.toString(array);
    }

    static Outcome addToArraysAsList() {
        List<String> list = Arrays.asList("a", "b", "c");
        return outcomeOf(() -> list.add("d"));
    }

    // ---- Views and copies ------------------------------------------------------------------------

    static String unmodifiableViewAfterChange() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> view = Collections.unmodifiableList(source);
        source.add("b");
        return view.toString();
    }

    static String copyOfAfterChange() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> copy = List.copyOf(source);
        source.add("b");
        return copy.toString();
    }

    // ---- Sequenced collections (Java 21) ---------------------------------------------------------

    static String reversedAfterChange() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> reversed = numbers.reversed();
        numbers.add(4);
        return reversed.toString();
    }

    static Outcome getFirstOfEmptyList() {
        List<String> empty = new ArrayList<>();
        return outcomeOf(() -> empty.getFirst());
    }

    // ---- Removing elements while iterating -------------------------------------------------------

    static Outcome removeInsideForEach() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3, 4));
        return outcomeOf(() -> {
            for (Integer number : numbers) {
                if (number % 2 == 0) {
                    numbers.remove(number);
                }
            }
        });
    }

    static String removeSecondToLastInsideForEach() {
        List<String> letters = new ArrayList<>(List.of("a", "b", "c", "d"));
        for (String letter : letters) {
            if (letter.equals("c")) {
                letters.remove(letter);
            }
        }
        return letters.toString();
    }

    // ---- remove(int) or remove(Object)? ----------------------------------------------------------

    static String removeWithInt() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3));
        numbers.remove(1);
        return numbers.toString();
    }

    static String removeWithInteger() {
        List<Integer> numbers = new ArrayList<>(List.of(1, 2, 3));
        numbers.remove(Integer.valueOf(1));
        return numbers.toString();
    }

    // ---- The order of a set ----------------------------------------------------------------------

    static String treeSetOrder() {
        Set<String> fruits = new TreeSet<>(List.of("banana", "cherry", "Apple", "apple"));
        return fruits.toString();
    }

    static String linkedHashSetOrder() {
        Set<String> tags = new LinkedHashSet<>();
        tags.add("c");
        tags.add("a");
        tags.add("b");
        tags.add("a");
        return tags.toString();
    }

    // ---- Keys that aren't in a map ---------------------------------------------------------------

    static String getMissingKey() {
        Map<String, Integer> stock = new HashMap<>();
        stock.put("apple", 3);
        return "pears: " + stock.get("pear");
    }

    static Outcome incrementMissingKey() {
        Map<String, Integer> stock = new HashMap<>();
        stock.put("apple", 3);
        return outcomeOf(() -> stock.put("pear", stock.get("pear") + 1));
    }

    static int getOrDefault() {
        Map<String, Integer> stock = new HashMap<>();
        stock.put("apple", 3);
        return stock.getOrDefault("apple", 0) + stock.getOrDefault("pear", 0);
    }

    // ---- Big-O -----------------------------------------------------------------------------------
    //
    // These questions have no code. For each one, answer in Answers with a BigO constant: how does the cost
    // of ONE call grow with n, the number of elements in the collection?
    //   - For ArrayList.add(x), give the average ("amortized") cost over many calls.
    //   - For HashMap, assume the keys have a well-spread hashCode().
    //
    //   ARRAY_LIST_GET_BY_INDEX     arrayList.get(i)
    //   ARRAY_LIST_ADD_AT_END       arrayList.add(x)
    //   ARRAY_LIST_ADD_AT_FRONT     arrayList.add(0, x)
    //   ARRAY_LIST_CONTAINS         arrayList.contains(x)
    //   LINKED_LIST_GET_BY_INDEX    linkedList.get(n / 2)
    //   LINKED_LIST_ADD_FIRST       linkedList.addFirst(x)
    //   HASH_MAP_GET                hashMap.get(key)
    //   TREE_MAP_GET                treeMap.get(key)
    //   PRIORITY_QUEUE_PEEK         priorityQueue.peek()
    //   PRIORITY_QUEUE_POLL         priorityQueue.poll()
    //   PRIORITY_QUEUE_CONTAINS     priorityQueue.contains(x)
    //   ARRAY_LIST_SORT             arrayList.sort(comparator)   (the whole sort)

    // ---- Helpers (not puzzles) -------------------------------------------------------------------

    /** Runs {@code action} and reports how it ended. */
    private static Outcome outcomeOf(Runnable action) {
        try {
            action.run();
            return Outcome.RETURNS_NORMALLY;
        } catch (UnsupportedOperationException _) {
            return Outcome.THROWS_UNSUPPORTED_OPERATION_EXCEPTION;
        } catch (ConcurrentModificationException _) {
            return Outcome.THROWS_CONCURRENT_MODIFICATION_EXCEPTION;
        } catch (NoSuchElementException _) {
            return Outcome.THROWS_NO_SUCH_ELEMENT_EXCEPTION;
        } catch (NullPointerException _) {
            return Outcome.THROWS_NULL_POINTER_EXCEPTION;
        } catch (IndexOutOfBoundsException _) {
            return Outcome.THROWS_INDEX_OUT_OF_BOUNDS_EXCEPTION;
        }
    }

    /** How a puzzle ends: normally, or with one of these exceptions. */
    enum Outcome {
        RETURNS_NORMALLY,
        THROWS_UNSUPPORTED_OPERATION_EXCEPTION,
        THROWS_CONCURRENT_MODIFICATION_EXCEPTION,
        THROWS_NO_SUCH_ELEMENT_EXCEPTION,
        THROWS_NULL_POINTER_EXCEPTION,
        THROWS_INDEX_OUT_OF_BOUNDS_EXCEPTION
    }

    /** How the cost of one call grows with n, the number of elements. */
    enum BigO {
        /** Constant: the cost doesn't depend on n. */
        O_1,
        /** Logarithmic: when n doubles, the cost grows by one step. */
        O_LOG_N,
        /** Linear: when n doubles, the cost doubles. */
        O_N,
        /** A little more than linear. The cost of sorting n elements by comparing them. */
        O_N_LOG_N
    }
}
