package com.was.employeemanagementsystem.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "employment_records")
public class EmploymentRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_title")
    private String jobTitle;

    @Column(name = "employment_period")
    private String employmentPeriod;

    @Column(name = "employer_name")
    private String employerName;

    @Column(name = "employer_address", columnDefinition = "TEXT")
    private String employerAddress;

    @Column(name = "contact_person_title")
    private String contactPersonTitle; // Mr, Mrs, Miss, Ms, Dr, Prof, etc.

    @Column(name = "contact_person_name")
    private String contactPersonName;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    @JsonIgnore
    @ToString.Exclude
    private Employee employee;
}

