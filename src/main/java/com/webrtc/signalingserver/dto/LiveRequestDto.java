package com.webrtc.signalingserver.dto;

import javax.validation.constraints.NotEmpty;

public class LiveRequestDto {

    @NotEmpty
    public String type;

    @NotEmpty
    public Long userId;

    @NotEmpty
    public Long lectureId;

    public String token;

    public SdpDto sdp;
}
