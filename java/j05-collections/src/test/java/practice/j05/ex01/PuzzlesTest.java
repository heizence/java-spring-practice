package practice.j05.ex01;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import practice.j05.ex01.Puzzles.BigO;
import practice.testkit.Predict;

@DisplayName("J05-01 · Collection traps and Big-O")
class PuzzlesTest {

    @Nested
    @DisplayName("Unmodifiable and fixed-size lists")
    class UnmodifiableAndFixedSizeLists {

        @Test
        @DisplayName("List.of(...).add(...)")
        void addToListOf() {
            Predict.check(Answers.ADD_TO_LIST_OF, Puzzles.addToListOf());
        }

        @Test
        @DisplayName("set(...) on Arrays.asList(array), then print the array")
        void setThroughArraysAsList() {
            Predict.check(Answers.SET_THROUGH_ARRAYS_AS_LIST, Puzzles.setThroughArraysAsList());
        }

        @Test
        @DisplayName("Arrays.asList(...).add(...)")
        void addToArraysAsList() {
            Predict.check(Answers.ADD_TO_ARRAYS_AS_LIST, Puzzles.addToArraysAsList());
        }
    }

    @Nested
    @DisplayName("Views and copies")
    class ViewsAndCopies {

        @Test
        @DisplayName("Collections.unmodifiableList(source) after source changes")
        void unmodifiableViewAfterChange() {
            Predict.check(Answers.UNMODIFIABLE_VIEW_AFTER_CHANGE, Puzzles.unmodifiableViewAfterChange());
        }

        @Test
        @DisplayName("List.copyOf(source) after source changes")
        void copyOfAfterChange() {
            Predict.check(Answers.COPY_OF_AFTER_CHANGE, Puzzles.copyOfAfterChange());
        }
    }

    @Nested
    @DisplayName("Sequenced collections")
    class SequencedCollections {

        @Test
        @DisplayName("numbers.reversed() after numbers changes")
        void reversedAfterChange() {
            Predict.check(Answers.REVERSED_AFTER_CHANGE, Puzzles.reversedAfterChange());
        }

        @Test
        @DisplayName("getFirst() on an empty list")
        void getFirstOfEmptyList() {
            Predict.check(Answers.GET_FIRST_OF_EMPTY_LIST, Puzzles.getFirstOfEmptyList());
        }
    }

    @Nested
    @DisplayName("Removing elements while iterating")
    class RemovingWhileIterating {

        @Test
        @DisplayName("remove the even numbers inside a for-each loop")
        void removeInsideForEach() {
            Predict.check(Answers.REMOVE_INSIDE_FOR_EACH, Puzzles.removeInsideForEach());
        }

        @Test
        @DisplayName("remove the second-to-last element inside a for-each loop")
        void removeSecondToLastInsideForEach() {
            Predict.check(Answers.REMOVE_SECOND_TO_LAST_INSIDE_FOR_EACH, Puzzles.removeSecondToLastInsideForEach());
        }
    }

    @Nested
    @DisplayName("remove(int) or remove(Object)")
    class RemoveIntOrObject {

        @Test
        @DisplayName("numbers.remove(1)")
        void removeWithInt() {
            Predict.check(Answers.REMOVE_WITH_INT, Puzzles.removeWithInt());
        }

        @Test
        @DisplayName("numbers.remove(Integer.valueOf(1))")
        void removeWithInteger() {
            Predict.check(Answers.REMOVE_WITH_INTEGER, Puzzles.removeWithInteger());
        }
    }

    @Nested
    @DisplayName("The order of a set")
    class SetOrder {

        @Test
        @DisplayName("a TreeSet of fruit names")
        void treeSetOrder() {
            Predict.check(Answers.TREE_SET_ORDER, Puzzles.treeSetOrder());
        }

        @Test
        @DisplayName("a LinkedHashSet with a repeated tag")
        void linkedHashSetOrder() {
            Predict.check(Answers.LINKED_HASH_SET_ORDER, Puzzles.linkedHashSetOrder());
        }
    }

    @Nested
    @DisplayName("Keys that aren't in a map")
    class MissingKeys {

        @Test
        @DisplayName("\"pears: \" + stock.get(\"pear\")")
        void getMissingKey() {
            Predict.check(Answers.GET_MISSING_KEY, Puzzles.getMissingKey());
        }

        @Test
        @DisplayName("stock.put(\"pear\", stock.get(\"pear\") + 1)")
        void incrementMissingKey() {
            Predict.check(Answers.INCREMENT_MISSING_KEY, Puzzles.incrementMissingKey());
        }

        @Test
        @DisplayName("getOrDefault(\"apple\", 0) + getOrDefault(\"pear\", 0)")
        void getOrDefault() {
            Predict.check(Answers.GET_OR_DEFAULT, Puzzles.getOrDefault());
        }
    }

    /**
     * The expected values here are the costs documented in the JDK. They are the answer key, so answer in
     * {@link Answers} before you read them.
     */
    @Nested
    @DisplayName("Big-O")
    class BigOCosts {

        @Test
        @DisplayName("ArrayList: get(i)")
        void arrayListGetByIndex() {
            Predict.checkAnswer(Answers.ARRAY_LIST_GET_BY_INDEX, BigO.O_1);
        }

        @Test
        @DisplayName("ArrayList: add(x), on average")
        void arrayListAddAtEnd() {
            Predict.checkAnswer(Answers.ARRAY_LIST_ADD_AT_END, BigO.O_1);
        }

        @Test
        @DisplayName("ArrayList: add(0, x)")
        void arrayListAddAtFront() {
            Predict.checkAnswer(Answers.ARRAY_LIST_ADD_AT_FRONT, BigO.O_N);
        }

        @Test
        @DisplayName("ArrayList: contains(x)")
        void arrayListContains() {
            Predict.checkAnswer(Answers.ARRAY_LIST_CONTAINS, BigO.O_N);
        }

        @Test
        @DisplayName("LinkedList: get(n / 2)")
        void linkedListGetByIndex() {
            Predict.checkAnswer(Answers.LINKED_LIST_GET_BY_INDEX, BigO.O_N);
        }

        @Test
        @DisplayName("LinkedList: addFirst(x)")
        void linkedListAddFirst() {
            Predict.checkAnswer(Answers.LINKED_LIST_ADD_FIRST, BigO.O_1);
        }

        @Test
        @DisplayName("HashMap: get(key)")
        void hashMapGet() {
            Predict.checkAnswer(Answers.HASH_MAP_GET, BigO.O_1);
        }

        @Test
        @DisplayName("TreeMap: get(key)")
        void treeMapGet() {
            Predict.checkAnswer(Answers.TREE_MAP_GET, BigO.O_LOG_N);
        }

        @Test
        @DisplayName("PriorityQueue: peek()")
        void priorityQueuePeek() {
            Predict.checkAnswer(Answers.PRIORITY_QUEUE_PEEK, BigO.O_1);
        }

        @Test
        @DisplayName("PriorityQueue: poll()")
        void priorityQueuePoll() {
            Predict.checkAnswer(Answers.PRIORITY_QUEUE_POLL, BigO.O_LOG_N);
        }

        @Test
        @DisplayName("PriorityQueue: contains(x)")
        void priorityQueueContains() {
            Predict.checkAnswer(Answers.PRIORITY_QUEUE_CONTAINS, BigO.O_N);
        }

        @Test
        @DisplayName("ArrayList: sort(comparator)")
        void arrayListSort() {
            Predict.checkAnswer(Answers.ARRAY_LIST_SORT, BigO.O_N_LOG_N);
        }
    }
}
