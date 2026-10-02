package practice.j09.ex04.sample.cycle;

import practice.j09.ex04.Component;

/** Components whose dependencies go round in circles. */
public final class CycleSamples {

    private CycleSamples() {
    }

    /** A needs B, and B needs A. */
    @Component
    public static class A {

        public A(B b) {
        }
    }

    @Component
    public static class B {

        public B(A a) {
        }
    }

    /** Leads into the A–B cycle, but isn't part of it. */
    @Component
    public static class Start {

        public Start(A a) {
        }
    }

    /** Needs itself. */
    @Component
    public static class SelfLoop {

        public SelfLoop(SelfLoop self) {
        }
    }

    /** X needs Y, Y needs Z, and Z needs X. */
    @Component
    public static class X {

        public X(Y y) {
        }
    }

    @Component
    public static class Y {

        public Y(Z z) {
        }
    }

    @Component
    public static class Z {

        public Z(X x) {
        }
    }

    /** Hub needs Leaf (which is finished first) and Spoke, and Spoke needs Hub. */
    @Component
    public static class Hub {

        public Hub(Leaf leaf, Spoke spoke) {
        }
    }

    @Component
    public static class Leaf {
    }

    @Component
    public static class Spoke {

        public Spoke(Hub hub) {
        }
    }

    /** P needs a Q, and the only Q (QImpl) needs a P. */
    public interface Q {
    }

    @Component
    public static class P {

        public P(Q q) {
        }
    }

    @Component
    public static class QImpl implements Q {

        public QImpl(P p) {
        }
    }
}
