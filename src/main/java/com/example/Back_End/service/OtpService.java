package com.example.Back_End.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class OtpService {

    private static class OtpEntry {
        String otp;
        long expiry;

        OtpEntry(String otp, long expiry) {
            this.otp = otp;
            this.expiry = expiry;
        }
    }

    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();

    public String generateOtp(String email) {
        String otp = String.format("%06d", ThreadLocalRandom.current().nextInt(999999));
        long expiry = System.currentTimeMillis() + 5 * 60 * 1000;
        otpStore.put(email, new OtpEntry(otp, expiry));
        return otp;
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpStore.get(email);
        if (entry == null || entry.expiry < System.currentTimeMillis()) {
            otpStore.remove(email);
            return false;
        }
        return entry.otp.equals(otp);
    }

    public void removeOtp(String email) {
        otpStore.remove(email);
    }
}
