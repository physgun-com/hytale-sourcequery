package com.infraly.sourcequery.protocol;

import java.net.InetSocketAddress;
import java.util.Arrays;

public final class ChallengeManager {

    private static final int SECRET = (int) (System.nanoTime() & 0xFFFFFFFFL);

    private ChallengeManager() {
    }

    public static int generate(InetSocketAddress address) {
        int timeBucket = (int) (System.currentTimeMillis() / 30000);
        byte[] ip = address.getAddress().getAddress();
        int ipHash = Arrays.hashCode(ip);

        return SECRET ^ timeBucket ^ ipHash ^ address.getPort();
    }

    public static boolean validate(InetSocketAddress address, int challenge) {
        int currentBucket = (int) (System.currentTimeMillis() / 30000);
        byte[] ip = address.getAddress().getAddress();
        int ipHash = Arrays.hashCode(ip);
        int port = address.getPort();

        for (int bucket = currentBucket; bucket >= currentBucket - 2; bucket--) {
            if ((SECRET ^ bucket ^ ipHash ^ port) == challenge) {
                return true;
            }
        }

        return false;
    }
}
