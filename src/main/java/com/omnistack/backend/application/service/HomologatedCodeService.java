package com.omnistack.backend.application.service;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Locale;
import org.springframework.stereotype.Service;

/**
 * Genera codigos homologados de autorizacion unicos, alfanumericos y de maximo 10 caracteres.
 *
 * <p>El algoritmo combina un timestamp en base-36 (6 chars) con un sufijo aleatorio (4 chars)
 * para garantizar unicidad por transaccion sin necesidad de secuencia en BD.</p>
 *
 * <p>Formato resultante: {@code TTTTTTSSSS} donde T = timestamp base-36 y S = random base-36.</p>
 */
@Service
public class HomologatedCodeService {

    private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int CODE_LENGTH = 10;
    private static final int TIMESTAMP_LENGTH = 6;
    private static final int RANDOM_LENGTH = CODE_LENGTH - TIMESTAMP_LENGTH;
    private static final int BASE = ALPHABET.length();

    private final SecureRandom random = new SecureRandom();

    /**
     * Genera un codigo homologado unico de 10 caracteres alfanumericos.
     *
     * @return codigo alfanumerico de 10 caracteres, unico por transaccion
     */
    public String generate() {
        String timestampPart = encodeTimestamp(Instant.now().toEpochMilli());
        String randomPart = generateRandom(RANDOM_LENGTH);
        return (timestampPart + randomPart).toUpperCase(Locale.ROOT);
    }

    private String encodeTimestamp(long millis) {
        StringBuilder sb = new StringBuilder();
        long value = millis;
        while (sb.length() < TIMESTAMP_LENGTH) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    private String generateRandom(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(ALPHABET.charAt(random.nextInt(BASE)));
        }
        return sb.toString();
    }
}
