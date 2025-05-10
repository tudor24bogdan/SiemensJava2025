package com.siemens.internship.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString // so we can debug faster and helps with logging by providing a readable string for an obj
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @NotBlank(message = "Name is required") // added validator
    private String name;
    private String description;
    private String status;

    // Add email regex validation
    @Email(message = "Email must be a valid email address") // implemented email validator
    private String email;
    /*
    * field validations ensure data integrity before it reaches the database
    * */
}