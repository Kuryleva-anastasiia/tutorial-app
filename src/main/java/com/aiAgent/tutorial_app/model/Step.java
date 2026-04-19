package com.aiAgent.tutorial_app.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Step {
    private int number;
    private String description;
    private boolean completed;
}