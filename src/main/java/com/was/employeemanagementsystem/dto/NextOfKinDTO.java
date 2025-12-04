package com.was.employeemanagementsystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NextOfKinDTO {
    private Long id;
    private String name;
    private String contact;
    private String address;
    private String relationship;
}
