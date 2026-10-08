package com.example.application_layer_software_framework_for_solar_powered_iot_systems;

import org.mindrot.jbcrypt.BCrypt;

public class HashSecurity {

    // Hashes a plain-text password with a generated salt
    public static String hashPassword(String plainPassword) {
        if (plainPassword == null || plainPassword.isEmpty()) {
            return null;
        }
        // BCrypt generates a unique salt automatically and embeds it in the hash string
        return BCrypt.hashpw(plainPassword, BCrypt.gensalt(12));
    }

    // Verifies a plain-text candidate against an existing BCrypt hash
    public static boolean checkPassword(String plainPassword, String storedHash) {
        if (plainPassword == null || storedHash == null || !storedHash.startsWith("$2")) {
            return false;
        }
        try {
            return BCrypt.checkpw(plainPassword, storedHash);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}