package com.example.photoarchive.domain.entities;

import lombok.Builder;
import lombok.Data;
import lombok.ToString;

@Data
@Builder
@ToString
public class ReadableGeocode {
    String country;
    String locality;
    String address;
}
