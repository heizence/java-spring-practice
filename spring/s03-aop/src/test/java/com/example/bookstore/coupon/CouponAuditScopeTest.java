package com.example.bookstore.coupon;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.bookstore.common.audit.AuditEntry;
import com.example.bookstore.common.audit.AuditLog;
import com.example.bookstore.support.IntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Service;

/**
 * More tests of S03-02, in the coupon package: they need a coupon service with a method that isn't public, and only
 * code in this package can call such a method. {@link CouponReports} exists only in these tests.
 */
@IntegrationTest
@Import(CouponAuditScopeTest.CouponReportsConfiguration.class)
@DisplayName("S03-02 · Pointcuts and advice types")
class CouponAuditScopeTest {

    @Autowired
    private CouponReports reports;

    @Autowired
    private AuditLog auditLog;

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("audits the public methods of every @Service in the coupon package")
        void auditsAnotherCouponService() {
            reports.summary("WELCOME5000");

            assertThat(auditLog.entries()).extracting(AuditEntry::action).containsExactly("CouponReports.summary");
        }

        @Test
        @DisplayName("doesn't audit a method that isn't public")
        void ignoresMethodsThatAreNotPublic() {
            reports.refreshTotals(); // package-private, and still called through the proxy

            assertThat(auditLog.entries()).isEmpty();
        }
    }

    /**
     * A coupon service for these tests. {@code @TestComponent} keeps component scanning from picking it up for every
     * other test; {@link CouponReportsConfiguration} registers it here.
     */
    @Service
    @TestComponent
    static class CouponReports {

        public String summary(String code) {
            return "Report for " + code;
        }

        void refreshTotals() {
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class CouponReportsConfiguration {

        @Bean
        CouponReports couponReports() {
            return new CouponReports();
        }
    }
}
