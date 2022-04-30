package org.tsdl.storage.influxdb;

public enum InfluxDbStorageProperty {
    /**
     * initialize
     */
    TOKEN,

    /**
     * initialize
     */
    ORGANIZATION,

    /**
     * store, load
     */
    BUCKET,

    /**
     * initialize
     */
    URL,

    /**
     * load
     */
    QUERY,

    /**
     * load
     */
    LOAD_FROM,

    /*
     * load
     */
    LOAD_UNTIL,

    /**
     * transform
     * -1: take values from all tables
     * >= 0: index of table to take values from
     */
    TABLE_INDEX
}
