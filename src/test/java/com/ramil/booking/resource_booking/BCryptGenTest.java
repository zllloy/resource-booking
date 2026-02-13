package com.ramil.booking.resource_booking;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class BCryptGenTest {

    @Test
    void printHashes() {
        BCryptPasswordEncoder enc = new BCryptPasswordEncoder();
        System.out.println("admin = " + enc.encode("admin"));
        System.out.println("user  = " + enc.encode("user"));
    }
}