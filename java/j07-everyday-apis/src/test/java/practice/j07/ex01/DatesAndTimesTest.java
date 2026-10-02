package practice.j07.ex01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Locale;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for the four small classes of J07-01, grouped by class. All dates are fixed, so the tests give the same
 * result on every day they run.
 */
@DisplayName("J07-01 · Dates and times")
class DatesAndTimesTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final ZoneId NEW_YORK = ZoneId.of("America/New_York");
    private static final ZoneId KOLKATA = ZoneId.of("Asia/Kolkata");

    private static LocalDate date(String isoDate) {
        return LocalDate.parse(isoDate);
    }

    @Nested
    @DisplayName("BusinessDays")
    class BusinessDaysTests {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("Monday plus 3 business days is Thursday")
            void addsWithinTheWeek() {
                assertThat(BusinessDays.add(date("2026-03-02"), 3, Set.of())).isEqualTo(date("2026-03-05"));
            }

            @Test
            @DisplayName("Friday plus 1 business day skips the weekend and is Monday")
            void skipsWeekend() {
                assertThat(BusinessDays.add(date("2026-03-06"), 1, Set.of())).isEqualTo(date("2026-03-09"));
            }

            @Test
            @DisplayName("skips a holiday")
            void skipsHoliday() {
                Set<LocalDate> hangulDay = Set.of(date("2026-10-09")); // a Friday

                assertThat(BusinessDays.add(date("2026-10-08"), 1, hangulDay)).isEqualTo(date("2026-10-12"));
            }

            @Test
            @DisplayName("Monday minus 1 business day is the Friday before")
            void subtractsOverWeekend() {
                assertThat(BusinessDays.add(date("2026-03-09"), -1, Set.of())).isEqualTo(date("2026-03-06"));
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("adding 0 business days returns the start date, even on a Saturday")
            void zeroDaysReturnsStart() {
                assertThat(BusinessDays.add(date("2026-03-07"), 0, Set.of())).isEqualTo(date("2026-03-07"));
            }

            @Test
            @DisplayName("a weekend start date doesn't count: Saturday plus 1 is Monday, Sunday minus 1 is Friday")
            void weekendStartDoesNotCount() {
                assertThat(BusinessDays.add(date("2026-03-07"), 1, Set.of())).isEqualTo(date("2026-03-09"));
                assertThat(BusinessDays.add(date("2026-03-08"), -1, Set.of())).isEqualTo(date("2026-03-06"));
            }

            @Test
            @DisplayName("skips several holidays in a row")
            void skipsHolidaysInARow() {
                Set<LocalDate> longWeekend = Set.of(date("2026-09-23"), date("2026-09-24"), date("2026-09-25"));

                assertThat(BusinessDays.add(date("2026-09-22"), 1, longWeekend)).isEqualTo(date("2026-09-28"));
            }

            @Test
            @DisplayName("moves backward over a holiday")
            void subtractsOverHoliday() {
                Set<LocalDate> hangulDay = Set.of(date("2026-10-09"));

                assertThat(BusinessDays.add(date("2026-10-12"), -1, hangulDay)).isEqualTo(date("2026-10-08"));
            }

            @Test
            @DisplayName("crosses the end of the year")
            void crossesYearEnd() {
                Set<LocalDate> newYearsDay = Set.of(date("2027-01-01"));

                assertThat(BusinessDays.add(date("2026-12-31"), 1, newYearsDay)).isEqualTo(date("2027-01-04"));
            }

            @Test
            @DisplayName("a holiday on a Saturday changes nothing")
            void weekendHolidayChangesNothing() {
                Set<LocalDate> saturdayHoliday = Set.of(date("2026-10-03"));

                assertThat(BusinessDays.add(date("2026-10-02"), 1, saturdayHoliday)).isEqualTo(date("2026-10-05"));
            }

            @Test
            @DisplayName("a year of business days: 2026 has 261 weekdays")
            void wholeYear() {
                assertThat(BusinessDays.add(date("2025-12-31"), 261, Set.of())).isEqualTo(date("2026-12-31"));
                assertThat(BusinessDays.add(date("2027-01-01"), -261, Set.of())).isEqualTo(date("2026-01-01"));
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("add() rejects a null start date or a null holiday set")
            void rejectsNulls() {
                assertThatThrownBy(() -> BusinessDays.add(null, 1, Set.of()))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> BusinessDays.add(date("2026-03-02"), 1, null))
                        .isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("Randomized")
        class Randomized {

            /** Counts business days by looking at every date in the range: a second, independent way to count. */
            private static long businessDaysBetween(LocalDate from, LocalDate to, Set<LocalDate> holidays) {
                return from.datesUntil(to)
                        .filter(day -> day.getDayOfWeek() != DayOfWeek.SATURDAY
                                && day.getDayOfWeek() != DayOfWeek.SUNDAY
                                && !holidays.contains(day))
                        .count();
            }

            @Test
            @DisplayName("always lands on a business day, exactly |days| business days away, over 2,000 random cases")
            void matchesCounting() {
                var random = new Random(42); // fixed seed: a failure is reproducible
                Set<LocalDate> holidays = Set.of(date("2026-01-01"), date("2026-03-02"), date("2026-05-05"),
                        date("2026-06-03"), date("2026-08-17"), date("2026-09-24"), date("2026-09-25"),
                        date("2026-10-05"), date("2026-10-09"), date("2026-12-25"));

                for (int step = 0; step < 2_000; step++) {
                    LocalDate start = date("2026-01-01").plusDays(random.nextInt(365));
                    int days = random.nextInt(61) - 30; // -30 .. 30

                    LocalDate result = BusinessDays.add(start, days, holidays);

                    // Count the business days in (start, result] going forward, or [result, start) going backward.
                    long counted = days >= 0
                            ? businessDaysBetween(start.plusDays(1), result.plusDays(1), holidays)
                            : businessDaysBetween(result, start, holidays);
                    assertThat(counted).as("business days from %s to %s at step %d", start, result, step)
                            .isEqualTo(Math.abs(days));
                    if (days != 0) {
                        assertThat(businessDaysBetween(result, result.plusDays(1), holidays))
                                .as("%s plus %d gave %s, which isn't a business day (step %d)",
                                        start, days, result, step)
                                .isEqualTo(1);
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Ages")
    class AgesTests {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("counts full years")
            void countsFullYears() {
                assertThat(Ages.ageOn(date("1990-07-15"), date("2026-10-02"))).isEqualTo(36);
            }

            @Test
            @DisplayName("gets one year older on the birthday, not the day before")
            void olderOnBirthday() {
                assertThat(Ages.ageOn(date("2000-05-10"), date("2026-05-09"))).isEqualTo(25);
                assertThat(Ages.ageOn(date("2000-05-10"), date("2026-05-10"))).isEqualTo(26);
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("is 0 on the day of birth")
            void zeroOnDayOfBirth() {
                assertThat(Ages.ageOn(date("2026-10-02"), date("2026-10-02"))).isZero();
            }

            @Test
            @DisplayName("born on 29 February: one year older on 1 March in a year without 29 February")
            void leapDayBirthdayInCommonYear() {
                assertThat(Ages.ageOn(date("2004-02-29"), date("2026-02-28"))).isEqualTo(21);
                assertThat(Ages.ageOn(date("2004-02-29"), date("2026-03-01"))).isEqualTo(22);
            }

            @Test
            @DisplayName("born on 29 February: one year older on 29 February in a leap year")
            void leapDayBirthdayInLeapYear() {
                assertThat(Ages.ageOn(date("2004-02-29"), date("2028-02-28"))).isEqualTo(23);
                assertThat(Ages.ageOn(date("2004-02-29"), date("2028-02-29"))).isEqualTo(24);
            }

            @Test
            @DisplayName("born on 1 March: the birthday counts in leap years and common years alike")
            void birthdayAfterFebruaryInLeapYears() {
                assertThat(Ages.ageOn(date("2000-03-01"), date("2001-03-01"))).isEqualTo(1);
                assertThat(Ages.ageOn(date("2001-03-01"), date("2004-02-29"))).isEqualTo(2);
                assertThat(Ages.ageOn(date("2001-03-01"), date("2004-03-01"))).isEqualTo(3);
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("ageOn() rejects a date before the birth date, naming both dates")
            void rejectsDateBeforeBirth() {
                assertThatThrownBy(() -> Ages.ageOn(date("2026-10-02"), date("2026-10-01")))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("2026-10-02")
                        .hasMessageContaining("2026-10-01");
            }

            @Test
            @DisplayName("ageOn() rejects null dates")
            void rejectsNulls() {
                assertThatThrownBy(() -> Ages.ageOn(null, date("2026-10-02")))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> Ages.ageOn(date("2000-01-01"), null))
                        .isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Nested
    @DisplayName("MeetingTimes")
    class MeetingTimesTests {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("9:00 in Seoul in summer is 20:00 the evening before in New York (UTC-4)")
            void convertsInSummer() {
                ZonedDateTime result = MeetingTimes.convert(LocalDateTime.parse("2026-07-01T09:00"), SEOUL, NEW_YORK);

                assertThat(result).isEqualTo(ZonedDateTime.of(LocalDateTime.parse("2026-06-30T20:00"), NEW_YORK));
                assertThat(result.getOffset().getId()).isEqualTo("-04:00");
            }

            @Test
            @DisplayName("9:00 in Seoul in winter is 19:00 the evening before in New York (UTC-5)")
            void convertsInWinter() {
                ZonedDateTime result = MeetingTimes.convert(LocalDateTime.parse("2026-01-15T09:00"), SEOUL, NEW_YORK);

                assertThat(result).isEqualTo(ZonedDateTime.of(LocalDateTime.parse("2026-01-14T19:00"), NEW_YORK));
                assertThat(result.getOffset().getId()).isEqualTo("-05:00");
            }

            @Test
            @DisplayName("formats a time with the English day name, a 24-hour clock and the zone")
            void formats() {
                var time = ZonedDateTime.of(LocalDateTime.parse("2026-03-08T16:30"), SEOUL);

                assertThat(MeetingTimes.format(time)).isEqualTo("Sun 2026-03-08 16:30 (Asia/Seoul)");
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @Test
            @DisplayName("moves a time in New York's DST gap (2026-03-08 02:30) forward to 03:30")
            void dstGapMovesForward() {
                LocalDateTime missing = LocalDateTime.parse("2026-03-08T02:30");

                assertThat(MeetingTimes.convert(missing, NEW_YORK, NEW_YORK).toLocalDateTime())
                        .isEqualTo(LocalDateTime.parse("2026-03-08T03:30"));
                assertThat(MeetingTimes.convert(missing, NEW_YORK, SEOUL))
                        .isEqualTo(ZonedDateTime.of(LocalDateTime.parse("2026-03-08T16:30"), SEOUL));
            }

            @Test
            @DisplayName("uses the earlier moment for a time in New York's DST overlap (2026-11-01 01:30)")
            void dstOverlapUsesEarlierOffset() {
                LocalDateTime twice = LocalDateTime.parse("2026-11-01T01:30");

                assertThat(MeetingTimes.convert(twice, NEW_YORK, SEOUL))
                        .isEqualTo(ZonedDateTime.of(LocalDateTime.parse("2026-11-01T14:30"), SEOUL));
            }

            @Test
            @DisplayName("handles a zone with a half-hour offset (Kolkata, UTC+5:30)")
            void halfHourOffset() {
                assertThat(MeetingTimes.convert(LocalDateTime.parse("2026-10-02T09:00"), SEOUL, KOLKATA))
                        .isEqualTo(ZonedDateTime.of(LocalDateTime.parse("2026-10-02T05:30"), KOLKATA));
            }

            @Test
            @DisplayName("formats the right year in the last days of December")
            void formatsYearEnd() {
                var time = ZonedDateTime.of(LocalDateTime.parse("2026-12-28T09:05"), SEOUL);

                assertThat(MeetingTimes.format(time)).isEqualTo("Mon 2026-12-28 09:05 (Asia/Seoul)");
            }

            @Test
            @DisplayName("formats in English even when the computer's default locale is Korean")
            void formatsInEnglishWhateverTheDefaultLocale() {
                var time = ZonedDateTime.of(LocalDateTime.parse("2026-03-08T16:30"), SEOUL);
                Locale original = Locale.getDefault();
                try {
                    Locale.setDefault(Locale.KOREA);

                    assertThat(MeetingTimes.format(time)).isEqualTo("Sun 2026-03-08 16:30 (Asia/Seoul)");
                } finally {
                    Locale.setDefault(original);
                }
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("convert() and format() reject null arguments")
            void rejectsNulls() {
                var time = LocalDateTime.parse("2026-10-02T09:00");

                assertThatThrownBy(() -> MeetingTimes.convert(null, SEOUL, NEW_YORK))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> MeetingTimes.convert(time, null, NEW_YORK))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> MeetingTimes.convert(time, SEOUL, null))
                        .isInstanceOf(NullPointerException.class);
                assertThatThrownBy(() -> MeetingTimes.format(null)).isInstanceOf(NullPointerException.class);
            }
        }
    }

    @Nested
    @DisplayName("Durations")
    class DurationsTests {

        @Nested
        @DisplayName("Basic")
        class Basic {

            @Test
            @DisplayName("65 minutes is \"1h 5m\"")
            void hoursAndMinutes() {
                assertThat(Durations.humanize(Duration.ofMinutes(65))).isEqualTo("1h 5m");
            }

            @Test
            @DisplayName("45 seconds is \"45s\"")
            void secondsOnly() {
                assertThat(Durations.humanize(Duration.ofSeconds(45))).isEqualTo("45s");
            }

            @Test
            @DisplayName("zero is \"0s\"")
            void zero() {
                assertThat(Durations.humanize(Duration.ZERO)).isEqualTo("0s");
            }
        }

        @Nested
        @DisplayName("Edge cases")
        class EdgeCases {

            @ParameterizedTest(name = "{0} seconds is \"{1}\"", quoteTextArguments = false)
            @CsvSource({
                    "1, 1s",
                    "59, 59s",
                    "60, 1m",
                    "61, 1m 1s",
                    "3599, 59m 59s",
                    "3600, 1h",
                    "3605, 1h 5s",
                    "86399, 23h 59m 59s",
                    "86400, 1d",
                    "90061, 1d 1h 1m 1s",
                    "93930, 1d 2h 5m 30s"
            })
            @DisplayName("shows every unit that isn't zero, largest first")
            void unitsAroundTheirLimits(long seconds, String expected) {
                assertThat(Durations.humanize(Duration.ofSeconds(seconds))).isEqualTo(expected);
            }

            @Test
            @DisplayName("drops fractions of a second")
            void dropsFractions() {
                assertThat(Durations.humanize(Duration.ofMillis(1_900))).isEqualTo("1s");
                assertThat(Durations.humanize(Duration.ofMillis(999))).isEqualTo("0s");
                assertThat(Durations.humanize(Duration.ofNanos(1))).isEqualTo("0s");
            }

            @Test
            @DisplayName("counts long durations in days")
            void longDurations() {
                assertThat(Durations.humanize(Duration.ofDays(400))).isEqualTo("400d");
            }
        }

        @Nested
        @DisplayName("Errors")
        class Errors {

            @Test
            @DisplayName("humanize() rejects a negative duration")
            void rejectsNegative() {
                assertThatThrownBy(() -> Durations.humanize(Duration.ofSeconds(-1)))
                        .isInstanceOf(IllegalArgumentException.class);
            }

            @Test
            @DisplayName("humanize() rejects null")
            void rejectsNull() {
                assertThatThrownBy(() -> Durations.humanize(null)).isInstanceOf(NullPointerException.class);
            }
        }
    }
}
