package com.webrtc.signalingserver;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.webrtc.signalingserver.util.EncryptString.*;

public class GenerateSHA1HashTest {

    @Test
    @DisplayName("같은 값으로 동일한 해시값 생성되는지 확인")
    public void isEqualValue() {
        String encryptedString1 = convertedToEncryption(1L, 2L);
        String encryptedString2 = convertedToEncryption(1L, 2L);

        Assertions.assertThat(encryptedString1).isEqualTo(encryptedString2);
        System.out.println("encryptedString1 = " + encryptedString1);
        System.out.println("encryptedString2 = " + encryptedString2);
    }

}
