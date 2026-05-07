package com.shadiwaley.server.onboarding.dto.request;

import com.shadiwaley.server.parent.domain.ParentRelation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ParentInfoRequest {
    private String parentName;
    private ParentRelation parentRelation;
    private String parentPhone;
    private String district;
    private String state;
    private String maslak;
    private String imamReference;
    private String masjidName;
}