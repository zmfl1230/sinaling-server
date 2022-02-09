package com.webrtc.signalingserver.dto;

import javax.validation.constraints.NotEmpty;

public class SdpDto {

    @NotEmpty
    public String sdp;

    @NotEmpty
    public String type;
}
