package com.webrtc.signalingserver.service;

import org.springframework.stereotype.Component;

public class TemplateForSynchronized {

    public synchronized void executeToSynchronize(NeedToSynchronized method) {
        method.execute();
    }
}
