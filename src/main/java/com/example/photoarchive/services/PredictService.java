package com.example.photoarchive.services;

import com.example.photoarchive.domain.entities.Photo;
import com.example.photoarchive.domain.entities.PredictName;

import java.time.LocalDateTime;

public interface PredictService {
    String getFormattedDateTime(LocalDateTime date);
    String filenameWithHash(Photo photo, String prefix);
    PredictName get(Photo photo);
}
