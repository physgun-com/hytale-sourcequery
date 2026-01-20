package com.infraly.sourcequery.protocol;

public final class A2SProtocol {

    public static final int PACKET_HEADER = 0xFFFFFFFF;

    public static final byte REQUEST_INFO = 0x54;
    public static final byte REQUEST_PLAYER = 0x55;
    public static final byte REQUEST_RULES = 0x56;
    public static final byte REQUEST_CHALLENGE = 0x57;

    public static final byte RESPONSE_INFO = 0x49;
    public static final byte RESPONSE_PLAYER = 0x44;
    public static final byte RESPONSE_RULES = 0x45;
    public static final byte RESPONSE_CHALLENGE = 0x41;

    public static final byte GAMEPORT_FLAG = (byte) 0x80;

    public static final byte SERVER_TYPE_DEDICATED = 'd';

    public static final byte ENVIRONMENT_LINUX = 'l';
    public static final byte ENVIRONMENT_WINDOWS = 'w';
    public static final byte ENVIRONMENT_MAC = 'm';

    public static final byte VAC_UNSECURED = 0;

    private A2SProtocol() {}
}
