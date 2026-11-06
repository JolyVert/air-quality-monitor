package pl.shkumat.airquality.model;

public enum InvalidReason {
    // value below 0 or above the measuring range of the sensors
    OUT_OF_RANGE,
    // the same PM2.5 and PM10 several times in a row, e.g. a broken sensor sending zeros
    STUCK_VALUE
}
