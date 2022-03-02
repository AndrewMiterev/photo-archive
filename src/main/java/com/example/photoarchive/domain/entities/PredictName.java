package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@ToString
public class PredictName {
    String folder;
    String subFolder;
    String subSubFolder;
    String name;
    LocalDateTime date;
}
