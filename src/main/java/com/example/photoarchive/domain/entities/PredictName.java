package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class PredictName {
    String folder;
    String subFolder;
    String subSubFolder;
    String name;
}
