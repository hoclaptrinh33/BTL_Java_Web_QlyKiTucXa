package com.ktx.security;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPT = 5;
    private static final int LOCK_TIME_MINUTES = 10;

    private final Map<String, Attempt> attemptsCache = new ConcurrentHashMap<>();

    public static class Attempt {
        private int count;
        private LocalDateTime lockTime;

        public Attempt(int count, LocalDateTime lockTime) {
            this.count = count;
            this.lockTime = lockTime;
        }

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public LocalDateTime getLockTime() {
            return lockTime;
        }

        public void setLockTime(LocalDateTime lockTime) {
            this.lockTime = lockTime;
        }
    }

    /**
     * Registers a failed login attempt.
     */
    public void loginFailed(String ip, String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        String key = buildKey(ip, username);
        Attempt attempt = attemptsCache.get(key);
        
        if (attempt == null) {
            attempt = new Attempt(1, null);
            attemptsCache.put(key, attempt);
        } else {
            if (attempt.getLockTime() != null) {
                // If already locked but lock duration is expired, reset the count
                if (attempt.getLockTime().plusMinutes(LOCK_TIME_MINUTES).isBefore(LocalDateTime.now())) {
                    attempt.setCount(1);
                    attempt.setLockTime(null);
                }
            } else {
                attempt.setCount(attempt.getCount() + 1);
                if (attempt.getCount() >= MAX_ATTEMPT) {
                    attempt.setLockTime(LocalDateTime.now());
                }
            }
        }
    }

    /**
     * Resets failed login attempts upon success.
     */
    public void loginSucceeded(String ip, String username) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        String key = buildKey(ip, username);
        attemptsCache.remove(key);
    }

    /**
     * Checks if a user is currently locked.
     */
    public boolean isBlocked(String ip, String username) {
        if (username == null || username.trim().isEmpty()) {
            return false;
        }
        String key = buildKey(ip, username);
        Attempt attempt = attemptsCache.get(key);
        
        if (attempt == null) {
            return false;
        }
        
        if (attempt.getLockTime() != null) {
            // Check if lock duration has expired
            if (attempt.getLockTime().plusMinutes(LOCK_TIME_MINUTES).isBefore(LocalDateTime.now())) {
                attemptsCache.remove(key);
                return false;
            }
            return true;
        }
        
        return false;
    }

    private String buildKey(String ip, String username) {
        String cleanIp = (ip == null) ? "unknown" : ip.trim();
        String cleanUsername = username.trim().toLowerCase();
        return cleanIp + ":" + cleanUsername;
    }
    
    // Package-private method for testing purposes to clear cache
    void clearCache() {
        attemptsCache.clear();
    }
}
