package com.webrtc.signalingserver.domain.dto;

import lombok.Builder;
import javax.validation.constraints.NotEmpty;

@Builder
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
