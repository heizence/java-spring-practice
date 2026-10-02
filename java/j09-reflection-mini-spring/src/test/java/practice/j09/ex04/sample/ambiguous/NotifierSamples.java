package practice.j09.ex04.sample.ambiguous;

import practice.j09.ex04.Component;
import practice.j09.ex04.Primary;

/** Several components implement the same interface. */
public final class NotifierSamples {

    private NotifierSamples() {
    }

    public interface Notifier {

        String send(String message);
    }

    @Component
    public static class EmailNotifier implements Notifier {

        @Override
        public String send(String message) {
            return "email: " + message;
        }
    }

    @Component
    public static class SmsNotifier implements Notifier {

        @Override
        public String send(String message) {
            return "sms: " + message;
        }
    }

    @Component
    @Primary
    public static class PushNotifier implements Notifier {

        @Override
        public String send(String message) {
            return "push: " + message;
        }
    }

    @Component
    @Primary
    public static class PagerNotifier implements Notifier {

        @Override
        public String send(String message) {
            return "pager: " + message;
        }
    }

    /** Needs "the" Notifier. */
    @Component
    public static class AlertService {

        private final Notifier notifier;

        public AlertService(Notifier notifier) {
            this.notifier = notifier;
        }

        public String alert(String message) {
            return notifier.send(message);
        }
    }
}
