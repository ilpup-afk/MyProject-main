package com.example.demo.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CsvImportResult {
    private int successCount;
    private int failedCount;
    private List<String> errors;

    public boolean hasError(){
        return errors != null && !errors.isEmpty();
    }
}