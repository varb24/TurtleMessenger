package io.turtlemessenger.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

public class JwtUtil {
    private static final String HMAC_ALGO = "HmacSHA256";
    private final byte[] secret;
    private final long ttlSeconds;

    public JwtUtil(String secret, long ttlSeconds) {
        this.secret = secret.getBytes(StandardCharsets.UTF_8);
        this.ttlSeconds = ttlSeconds;
    }

    public String generateToken(String subject) {
        long now = Instant.now().getEpochSecond();
        long exp = now + ttlSeconds;
        String headerJson = Json.minify("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payloadJson = Json.minify(String.format("{\"sub\":\"%s\",\"iat\":%d,\"exp\":%d}", escape(subject), now, exp));
        String header = base64Url(headerJson.getBytes(StandardCharsets.UTF_8));
        String payload = base64Url(payloadJson.getBytes(StandardCharsets.UTF_8));
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public boolean isValid(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return false;
            String sig = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(sig, parts[2])) return false;
            Map<String, Object> payload = Json.parse(base64UrlDecode(parts[1]));
            Object exp = payload.get("exp");
            if (exp instanceof Number) {
                long e = ((Number) exp).longValue();
                return Instant.now().getEpochSecond() < e;
            }
            return false;
        } catch (Exception e) { return false; }
    }

    public String getSubject(String token) {
        String[] parts = token.split("\\.");
        Map<String, Object> payload = Json.parse(base64UrlDecode(parts[1]));
        Object sub = payload.get("sub");
        return sub != null ? sub.toString() : null;
    }

    private String sign(String data) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret, HMAC_ALGO));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new RuntimeException(e); }
    }

    private static String base64Url(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
    private static String base64UrlDecode(String s) {
        return new String(Base64.getUrlDecoder().decode(s), StandardCharsets.UTF_8);
    }
    private static boolean constantTimeEquals(String a, String b) {
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0; for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // Minimal JSON helper for flat objects
    static class Json {
        static String minify(String s) { return s; }
        @SuppressWarnings("unchecked")
        static Map<String, Object> parse(String json) {
            // Extremely small JSON parser for our controlled payload
            // Accepts {"k":v} with v as number or string
            java.util.HashMap<String, Object> map = new java.util.HashMap<>();
            String body = json.trim();
            if (body.startsWith("{") && body.endsWith("}")) {
                body = body.substring(1, body.length()-1).trim();
                if (body.isEmpty()) return map;
                for (String part : body.split(",")) {
                    String[] kv = part.split(":", 2);
                    String k = kv[0].trim();
                    String v = kv[1].trim();
                    if (k.startsWith("\"") && k.endsWith("\"")) k = k.substring(1, k.length()-1);
                    if (v.startsWith("\"") && v.endsWith("\"")) {
                        map.put(k, v.substring(1, v.length()-1));
                    } else {
                        try { map.put(k, Long.parseLong(v)); }
                        catch (NumberFormatException e) { map.put(k, v); }
                    }
                }
            }
            return map;
        }
    }
}

