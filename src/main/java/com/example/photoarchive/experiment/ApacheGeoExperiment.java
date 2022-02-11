package com.example.photoarchive.experiment;

/*
import com.example.photoarchive.domain.entities.Geo;

import com.example.photoarchive.domain.entities.Photo;
import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.ImageMetadata;
import org.apache.commons.imaging.common.RationalNumber;
import org.apache.commons.imaging.formats.jpeg.JpegImageMetadata;
import org.apache.commons.imaging.formats.tiff.TiffField;
import org.apache.commons.imaging.formats.tiff.TiffImageMetadata;
import org.apache.commons.imaging.formats.tiff.constants.GpsTagConstants;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Optional;
*/
public class ApacheGeoExperiment {
/*    public Optional<Geo> getGetGeoData(Photo photo) {
        try {
            final ImageMetadata metadata = Imaging.getMetadata(Paths.get(photo.getFolder(), photo.getName()).toFile());
            log.debug(metadata);
            if (metadata instanceof JpegImageMetadata jpegMetadata) {
                final TiffImageMetadata exifMetadata = jpegMetadata.getExif();
                if (null != exifMetadata) {
                    final TiffImageMetadata.GPSInfo gpsInfo = exifMetadata.getGPS();
                    if (null != gpsInfo) {
                        return Optional.of(
                                Geo.builder()
                                        .longitude(gpsInfo.getLongitudeAsDegreesEast())
                                        .latitude(gpsInfo.getLatitudeAsDegreesNorth())
                                        .altitude(getAltitude(jpegMetadata))
                                        .build());
                    }
                }
            }
        } catch (ImageReadException | IOException e) {
            log.error(e);
        }
        return Optional.empty();
    }

    private Double getAltitude(JpegImageMetadata jpegImageMetadata) {
        try {
            TiffField altitude = jpegImageMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_ALTITUDE);
            RationalNumber calc = (RationalNumber) altitude.getValue();
            TiffField altitudeRef = jpegImageMetadata.findEXIFValueWithExactMatch(GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF);
            Byte level = (Byte) altitudeRef.getValue();
            double offset = level == GpsTagConstants.GPS_TAG_GPS_ALTITUDE_REF_VALUE_ABOVE_SEA_LEVEL ? 1.0D : -1.0D;
            return calc.doubleValue() * offset;
        } catch (NullPointerException | ImageReadException e) {
            return null;
        }
    }
*/
}
