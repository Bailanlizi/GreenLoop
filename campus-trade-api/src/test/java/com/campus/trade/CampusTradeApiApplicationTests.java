package com.campus.trade;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "orders.payment-expiration-enabled=false",
        "security.bootstrap-admin.enabled=false"
})
class CampusTradeApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
