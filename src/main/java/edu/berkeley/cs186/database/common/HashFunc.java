package edu.berkeley.cs186.database.common;

import java.util.Arrays;
import java.util.function.Function;
import edu.berkeley.cs186.database.databox.DataBox;
import edu.berkeley.cs186.database.databox.IntDataBox;

public class HashFunc {
    /**
     * Returns an independent hash function for the given pass. For pass 0, returns the default
     * hash function for the given DataBox type.
     * @param pass - Which pass of hashing this function belongs to.
     * @return A hash function, taking in a DataBox and returning an integer hash value. Hash values
     * CAN be negative.
     */
    public static Function<DataBox, Integer> getHashFunction(int pass) {
        if (pass == 1) {
            // First pass just uses regular hash function
            return (DataBox d) -> {
                return d.hashCode();
            };
        }

        return (DataBox d) -> {
            return hashBytes(d.toBytes(), pass);
        };
    }

    /**
     * Based on postgres's hash_bytes_extended()
     */
    private static int hashBytes(byte[] k, long seed) {
        HashState state = new HashState(k.length);
        if (seed != 0) {
            state.a += (int) (seed >> 32);
            state.b += (int) (seed);
            state.mix();
        }
        while (k.length > 12) {
            // Handle most of key
            // a += (k[3] + ((uint32) k[2] << 8) + ((uint32) k[1] << 16) + ((uint32) k[0] << 24))
            state.a += k[3]  + ((bytesToInt(k, 2)  << 8)) + ((bytesToInt(k, 1)  << 16)) + ((bytesToInt(k, 0)  << 24));
            state.b += k[7]  + ((bytesToInt(k, 6)  << 8)) + ((bytesToInt(k, 5)  << 16)) + ((bytesToInt(k, 4)  << 24));
            state.c += k[11] + ((bytesToInt(k, 10) << 8)) + ((bytesToInt(k, 9)  << 16)) + ((bytesToInt(k, 8)  << 24));
            state.a += k[0]  + ((bytesToInt(k, 1)  << 8)) + ((bytesToInt(k, 2)  << 16)) + ((bytesToInt(k, 3)  << 24));
            state.b += k[4]  + ((bytesToInt(k, 5)  << 8)) + ((bytesToInt(k, 6)  << 16)) + ((bytesToInt(k, 7)  << 24));
            state.c += k[8] +  ((bytesToInt(k, 9)  << 8)) + ((bytesToInt(k, 10) << 16)) + ((bytesToInt(k, 11) << 24));
            state.mix();
            k = Arrays.copyOfRange(k, 12, k.length - 12);
        }

        switch(k.length) {
            case 11:
                state.c += ((int) k[10]) << 8;
                /* fall through */
            case 10:
                state.c += ((int) k[9]) << 16;
                /* fall through */
            case 9:
                state.c += ((int) k[8]) << 24;
                /* fall through */
            case 8:
                /* the lowest byte of c is reserved for the length */
                state.b += bytesToInt(k, 1);
                state.a += bytesToInt(k, 0);
                break;
            case 7:
                state.b += ((int) k[6]) << 8;
                /* fall through */
            case 6:
                state.b += ((int) k[5]) << 16;
                /* fall through */
            case 5:
                state.b += ((int) k[4]) << 24;
                /* fall through */
            case 4:
                state.a += bytesToInt(k, 0);
                break;
            case 3:
                state.a += ((int) k[2]) << 8;
                /* fall through */
            case 2:
                state.a += ((int) k[1]) << 16;
                /* fall through */
            case 1:
                state.a += ((int) k[0]) << 24;
        }
        state.finalMix();
        return state.c;
    }

    /**
     * Rotates the bits of i left by offset, with wrapping.
     */
    private static int rot(int i, int offset) {
        return (i<<offset) | (i>>(32 - offset));
    }

    /**
     * Converts the bytes from offset to offset + 4 of k into a Big Endian integer
     */
    static int bytesToInt(byte[] k, int offset) {
        return ByteBuffer.wrap(k, offset, 4).getInt();
    }

    private static class HashState {
        int a, b, c;
        public HashState(int len) {
            a = b = c = 0x9e3779b9 + len + 3923095;
        }

        /**
         * Mixes the three states. Based on Postgres's hashfunc::mix
         */
        public void mix()  {
            a -= c;  a ^= rot(c, 4);  c += b;
            b -= a;  b ^= rot(a, 6);  a += c;
            c -= b;  c ^= rot(b, 8);  b += a;
            a -= c;  a ^= rot(c,16);  c += b;
            b -= a;  b ^= rot(a,19);  a += c;
            c -= b;  c ^= rot(b, 4);  b += a;
        }

        /**
         * Alternate mix function. Based on Postgres's hashfunc::final
         */
        public void finalMix() {
            c ^= b; c -= rot(b,14);
            a ^= c; a -= rot(c,11);
            b ^= a; b -= rot(a,25);
            c ^= b; c -= rot(b,16);
            a ^= c; a -= rot(c, 4);
            b ^= a; b -= rot(a,14);
            c ^= b; c -= rot(b,24);
        }
    }
}