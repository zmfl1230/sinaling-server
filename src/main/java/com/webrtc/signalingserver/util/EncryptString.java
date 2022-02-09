package com.webrtc.signalingserver.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptString {

    public static String encryptThisString(String input)
    {
        try {
            // getInstance() method is called with algorithm SHA-1
            MessageDigest md = MessageDigest.getInstance("SHA-1");

            // digest() method is called
            // to calculate message digest of the input string
            // returned as array of byte
            byte[] messageDigest = md.digest(input.getBytes());

            // Convert byte array into signum representation
            BigInteger no = new BigInteger(1, messageDigest);

            // Convert message digest into hex value
            StringBuilder hashText = new StringBuilder(no.toString(16));

            // Add preceding 0s to make it 32 bit
            while (hashText.length() < 32) {
                hashText.insert(0, "0");
            }

            // return the HashText
            return hashText.toString();
        }

        // For specifying wrong message digest algorithms
        catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    public static String convertedToEncryption(Long arg1, Long arg2) {
        String s = changeLongToString(arg1, arg2);
        return encryptThisString(s);
    }

    public static String changeLongToString(Long arg1, Long arg2) {
        return arg1 + Long.toString(arg2);
    }

    public static String changeLongToString(Long arg) {
        return Long.toString(arg);
    }

}
