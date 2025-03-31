package launcher.helper;

import launcher.LauncherAPI;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.interfaces.RSAKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class SecurityHelper {
    @LauncherAPI
    public static final String RSA_ALGO = "RSA";
    @LauncherAPI
    public static final String RSA_SIGN_ALGO = "SHA256withRSA";
    @LauncherAPI
    public static final String RSA_CIPHER_ALGO = "RSA/ECB/PKCS1Padding";
    @LauncherAPI
    public static final int TOKEN_LENGTH = 16;
    @LauncherAPI
    public static final int RSA_KEY_LENGTH_BITS = 2048;
    @LauncherAPI
    public static final int RSA_KEY_LENGTH = 256;
    @LauncherAPI
    public static final int CRYPTO_MAX_LENGTH = 2048;
    @LauncherAPI
    public static final String CERTIFICATE_DIGEST = "b87c079e3bf6e709860e05e283678c857b6a27916c2ba280a212f78f1a2ec20a";
    @LauncherAPI
    public static final String HEX = "0123456789abcdef";
    @LauncherAPI
    public static final String JWT = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.";
    private static final char[] VOWELS = new char[]{'e', 'u', 'i', 'o', 'a'};
    private static final char[] CONS = new char[]{'r', 't', 'p', 's', 'd', 'f', 'g', 'h', 'k', 'l', 'c', 'v', 'b', 'n', 'm'};

    private SecurityHelper() {
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm digestAlgorithm, String string) {
        return SecurityHelper.digest(digestAlgorithm, IOHelper.encode(string));
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm digestAlgorithm, URL uRL) throws IOException {
        try (InputStream inputStream = IOHelper.newInput(uRL);){
            byte[] byArray;
            byte[] byArray2;
            byte[] byArray3 = byArray2 = (byArray = SecurityHelper.digest(digestAlgorithm, inputStream));
            return byArray3;
        }
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm digestAlgorithm, Path path) throws IOException {
        try (InputStream inputStream = IOHelper.newInput(path);){
            byte[] byArray;
            byte[] byArray2;
            byte[] byArray3 = byArray2 = (byArray = SecurityHelper.digest(digestAlgorithm, inputStream));
            return byArray3;
        }
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm digestAlgorithm, byte[] byArray) {
        return SecurityHelper.newDigest(digestAlgorithm).digest(byArray);
    }

    @LauncherAPI
    public static byte[] digest(DigestAlgorithm digestAlgorithm, InputStream inputStream) throws IOException {
        byte[] byArray = IOHelper.newBuffer();
        MessageDigest messageDigest = SecurityHelper.newDigest(digestAlgorithm);
        int n = inputStream.read(byArray);
        while (n != -1) {
            messageDigest.update(byArray, 0, n);
            n = inputStream.read(byArray);
        }
        return messageDigest.digest();
    }

    @LauncherAPI
    public static KeyPair genRSAKeyPair(SecureRandom secureRandom) {
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(RSA_ALGO);
            keyPairGenerator.initialize(2048, secureRandom);
            return keyPairGenerator.genKeyPair();
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new InternalError(noSuchAlgorithmException);
        }
    }

    @LauncherAPI
    public static KeyPair genRSAKeyPair() {
        return SecurityHelper.genRSAKeyPair(SecurityHelper.newRandom());
    }

    @LauncherAPI
    public static boolean isValidCertificate(Certificate certificate) {
        try {
            return SecurityHelper.toHex(SecurityHelper.digest(DigestAlgorithm.SHA256, certificate.getEncoded())).equals(CERTIFICATE_DIGEST);
        }
        catch (CertificateEncodingException certificateEncodingException) {
            throw new InternalError(certificateEncodingException);
        }
    }

    @LauncherAPI
    public static boolean isValidCertificates(Certificate ... certificateArray) {
        return certificateArray != null && certificateArray.length == 1 && SecurityHelper.isValidCertificate(certificateArray[0]);
    }

    @LauncherAPI
    public static boolean isValidCertificates(Class clazz) {
        Certificate[] certificateArray = JVMHelper.getCertificates("META-INF/MANIFEST.MF");
        if (certificateArray == null || !SecurityHelper.isValidCertificates(certificateArray)) {
            return false;
        }
        CodeSource codeSource = clazz.getProtectionDomain().getCodeSource();
        return codeSource != null && SecurityHelper.isValidCertificates(codeSource.getCertificates());
    }

    @LauncherAPI
    public static boolean isValidSign(Path path, byte[] byArray, RSAPublicKey rSAPublicKey) throws IOException, SignatureException {
        try (InputStream inputStream = IOHelper.newInput(path);){
            boolean bl;
            boolean bl2;
            boolean bl3 = bl2 = (bl = SecurityHelper.isValidSign(inputStream, byArray, rSAPublicKey));
            return bl3;
        }
    }

    @LauncherAPI
    public static boolean isValidSign(byte[] byArray, byte[] byArray2, RSAPublicKey rSAPublicKey) throws SignatureException {
        Signature signature = SecurityHelper.newRSAVerifySignature(rSAPublicKey);
        try {
            signature.update(byArray);
        }
        catch (SignatureException signatureException) {
            throw new InternalError(signatureException);
        }
        return signature.verify(byArray2);
    }

    @LauncherAPI
    public static boolean isValidSign(InputStream inputStream, byte[] byArray, RSAPublicKey rSAPublicKey) throws IOException, SignatureException {
        Signature signature = SecurityHelper.newRSAVerifySignature(rSAPublicKey);
        SecurityHelper.updateSignature(inputStream, signature);
        return signature.verify(byArray);
    }

    @LauncherAPI
    public static boolean isValidSign(URL uRL, byte[] byArray, RSAPublicKey rSAPublicKey) throws IOException, SignatureException {
        try (InputStream inputStream = IOHelper.newInput(uRL);){
            boolean bl;
            boolean bl2;
            boolean bl3 = bl2 = (bl = SecurityHelper.isValidSign(inputStream, byArray, rSAPublicKey));
            return bl3;
        }
    }

    @LauncherAPI
    public static boolean isValidToken(CharSequence charSequence) {
        return charSequence.chars().allMatch(n -> JWT.indexOf(n) >= 0);
    }

    @LauncherAPI
    public static MessageDigest newDigest(DigestAlgorithm digestAlgorithm2) {
        VerifyHelper.verify(digestAlgorithm2, digestAlgorithm -> digestAlgorithm != DigestAlgorithm.PLAIN, "PLAIN digest");
        try {
            return MessageDigest.getInstance(digestAlgorithm2.name);
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new InternalError(noSuchAlgorithmException);
        }
    }

    @LauncherAPI
    public static Cipher newRSADecryptCipher(RSAPrivateKey rSAPrivateKey) {
        return SecurityHelper.newRSACipher(2, rSAPrivateKey);
    }

    @LauncherAPI
    public static Cipher newRSAEncryptCipher(RSAPublicKey rSAPublicKey) {
        return SecurityHelper.newRSACipher(1, rSAPublicKey);
    }

    @LauncherAPI
    public static Signature newRSASignSignature(RSAPrivateKey rSAPrivateKey) {
        Signature signature = SecurityHelper.newRSASignature();
        try {
            signature.initSign(rSAPrivateKey);
        }
        catch (InvalidKeyException invalidKeyException) {
            throw new InternalError(invalidKeyException);
        }
        return signature;
    }

    @LauncherAPI
    public static Signature newRSAVerifySignature(RSAPublicKey rSAPublicKey) {
        Signature signature = SecurityHelper.newRSASignature();
        try {
            signature.initVerify(rSAPublicKey);
        }
        catch (InvalidKeyException invalidKeyException) {
            throw new InternalError(invalidKeyException);
        }
        return signature;
    }

    @LauncherAPI
    public static SecureRandom newRandom() {
        return new SecureRandom();
    }

    @LauncherAPI
    public static byte[] randomBytes(Random random, int n) {
        byte[] byArray = new byte[n];
        random.nextBytes(byArray);
        return byArray;
    }

    @LauncherAPI
    public static byte[] randomBytes(int n) {
        return SecurityHelper.randomBytes(SecurityHelper.newRandom(), n);
    }

    @LauncherAPI
    public static String randomStringToken(Random random) {
        return SecurityHelper.toHex(SecurityHelper.randomToken(random));
    }

    @LauncherAPI
    public static String randomStringToken() {
        return SecurityHelper.randomStringToken(SecurityHelper.newRandom());
    }

    @LauncherAPI
    public static byte[] randomToken(Random random) {
        return SecurityHelper.randomBytes(random, 16);
    }

    @LauncherAPI
    public static byte[] randomToken() {
        return SecurityHelper.randomToken(SecurityHelper.newRandom());
    }

    @LauncherAPI
    public static String randomUsername(Random random) {
        String string;
        String string2;
        int n = 3 + random.nextInt(7);
        int n2 = random.nextInt(7);
        if (n >= 5 && n2 == 6) {
            string2 = random.nextBoolean() ? "Mr" : "Dr";
            n -= 2;
        } else if (n >= 6 && n2 == 5) {
            string2 = "Mrs";
            n -= 3;
        } else {
            string2 = "";
        }
        int n3 = random.nextInt(7);
        if (n >= 5 && n3 == 6) {
            string = String.valueOf(10 + random.nextInt(90));
            n -= 2;
        } else if (n >= 7 && n3 == 5) {
            string = String.valueOf(1990 + random.nextInt(26));
            n -= 4;
        } else {
            string = "";
        }
        int n4 = 0;
        boolean bl = random.nextBoolean();
        char[] cArray = new char[n];
        for (int i = 0; i < cArray.length; ++i) {
            if (i > 1 && bl && random.nextInt(10) == 0) {
                cArray[i] = cArray[i - 1];
                continue;
            }
            if (n4 < 1 && random.nextInt() == 5) {
                ++n4;
            } else {
                n4 = 0;
                bl ^= true;
            }
            char[] cArray2 = bl ? CONS : VOWELS;
            cArray[i] = cArray2[random.nextInt(cArray2.length)];
        }
        if (!string2.isEmpty() || random.nextBoolean()) {
            cArray[0] = Character.toUpperCase(cArray[0]);
        }
        return VerifyHelper.verifyUsername(string2 + new String(cArray) + string);
    }

    @LauncherAPI
    public static String randomUsername() {
        return SecurityHelper.randomUsername(SecurityHelper.newRandom());
    }

    @LauncherAPI
    public static byte[] sign(InputStream inputStream, RSAPrivateKey rSAPrivateKey) throws IOException {
        Signature signature = SecurityHelper.newRSASignSignature(rSAPrivateKey);
        SecurityHelper.updateSignature(inputStream, signature);
        try {
            return signature.sign();
        }
        catch (SignatureException signatureException) {
            throw new InternalError(signatureException);
        }
    }

    @LauncherAPI
    public static byte[] sign(byte[] byArray, RSAPrivateKey rSAPrivateKey) {
        Signature signature = SecurityHelper.newRSASignSignature(rSAPrivateKey);
        try {
            signature.update(byArray);
            return signature.sign();
        }
        catch (SignatureException signatureException) {
            throw new InternalError(signatureException);
        }
    }

    @LauncherAPI
    public static byte[] sign(Path path, RSAPrivateKey rSAPrivateKey) throws IOException {
        try (InputStream inputStream = IOHelper.newInput(path);){
            byte[] byArray;
            byte[] byArray2;
            byte[] byArray3 = byArray2 = (byArray = SecurityHelper.sign(inputStream, rSAPrivateKey));
            return byArray3;
        }
    }

    @LauncherAPI
    public static String toHex(byte[] byArray) {
        int n = 0;
        char[] cArray = new char[byArray.length << 1];
        for (byte by : byArray) {
            int n2 = Byte.toUnsignedInt(by);
            cArray[n] = HEX.charAt(n2 >>> 4);
            cArray[++n] = HEX.charAt(n2 & 0xF);
            ++n;
        }
        return new String(cArray);
    }

    @LauncherAPI
    public static RSAPrivateKey toPrivateRSAKey(byte[] byArray) throws InvalidKeySpecException {
        return (RSAPrivateKey)SecurityHelper.newRSAKeyFactory().generatePrivate(new PKCS8EncodedKeySpec(byArray));
    }

    @LauncherAPI
    public static RSAPublicKey toPublicRSAKey(byte[] byArray) throws InvalidKeySpecException {
        return (RSAPublicKey)SecurityHelper.newRSAKeyFactory().generatePublic(new X509EncodedKeySpec(byArray));
    }

    @LauncherAPI
    public static void verifyCertificates(Class clazz) {
    }

    @LauncherAPI
    public static void verifySign(byte[] byArray, byte[] byArray2, RSAPublicKey rSAPublicKey) throws SignatureException {
        if (!SecurityHelper.isValidSign(byArray, byArray2, rSAPublicKey)) {
            throw new SignatureException("Invalid sign");
        }
    }

    @LauncherAPI
    public static void verifySign(InputStream inputStream, byte[] byArray, RSAPublicKey rSAPublicKey) throws SignatureException, IOException {
        if (!SecurityHelper.isValidSign(inputStream, byArray, rSAPublicKey)) {
            throw new SignatureException("Invalid stream sign");
        }
    }

    @LauncherAPI
    public static void verifySign(Path path, byte[] byArray, RSAPublicKey rSAPublicKey) throws SignatureException, IOException {
        if (!SecurityHelper.isValidSign(path, byArray, rSAPublicKey)) {
            throw new SignatureException(String.format("Invalid file sign: '%s'", path));
        }
    }

    @LauncherAPI
    public static void verifySign(URL uRL, byte[] byArray, RSAPublicKey rSAPublicKey) throws SignatureException, IOException {
        if (!SecurityHelper.isValidSign(uRL, byArray, rSAPublicKey)) {
            throw new SignatureException(String.format("Invalid URL sign: '%s'", uRL));
        }
    }

    @LauncherAPI
    public static String verifyToken(String string) {
        return VerifyHelper.verify(string, SecurityHelper::isValidToken, String.format("Invalid token: '%s'", string));
    }

    private static Cipher newCipher(String string) {
        try {
            return Cipher.getInstance(string);
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new InternalError(noSuchAlgorithmException);
        }
        catch (NoSuchPaddingException noSuchPaddingException) {
            throw new InternalError(noSuchPaddingException);
        }
    }

    private static Cipher newRSACipher(int n, RSAKey rSAKey) {
        Cipher cipher = SecurityHelper.newCipher(RSA_CIPHER_ALGO);
        try {
            cipher.init(n, (Key)((Object)rSAKey));
        }
        catch (InvalidKeyException invalidKeyException) {
            throw new InternalError(invalidKeyException);
        }
        return cipher;
    }

    private static KeyFactory newRSAKeyFactory() {
        try {
            return KeyFactory.getInstance(RSA_ALGO);
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new InternalError(noSuchAlgorithmException);
        }
    }

    private static Signature newRSASignature() {
        try {
            return Signature.getInstance(RSA_SIGN_ALGO);
        }
        catch (NoSuchAlgorithmException noSuchAlgorithmException) {
            throw new InternalError(noSuchAlgorithmException);
        }
    }

    private static void updateSignature(InputStream inputStream, Signature signature) throws IOException {
        byte[] byArray = IOHelper.newBuffer();
        int n = inputStream.read(byArray);
        while (n >= 0) {
            try {
                signature.update(byArray, 0, n);
            }
            catch (SignatureException signatureException) {
                throw new InternalError(signatureException);
            }
            n = inputStream.read(byArray);
        }
    }

    @LauncherAPI
    public enum DigestAlgorithm {
        PLAIN("plain", -1), MD5("MD5", 128), SHA1("SHA-1", 160), SHA224("SHA-224", 224), SHA256("SHA-256", 256), SHA512("SHA-512", 512);
        private static final Map<String, DigestAlgorithm> ALGORITHMS;
        static {
            DigestAlgorithm[] digestAlgorithmArray = values();
            ALGORITHMS = new HashMap<>(digestAlgorithmArray.length);
            for (DigestAlgorithm digestAlgorithm : digestAlgorithmArray) {
                ALGORITHMS.put(digestAlgorithm.name, digestAlgorithm);
            }
        }
        public final String name;
        public final int bits;
        public final int bytes;

        DigestAlgorithm(String name, int bits) {
            this.name = name;
            this.bits = bits;
            bytes = bits / Byte.SIZE;
            assert bits % Byte.SIZE == 0;
        }
        public static DigestAlgorithm byName(String string) {
            return (DigestAlgorithm)((Object)VerifyHelper.getMapValue(ALGORITHMS, string, String.format("Unknown digest algorithm: '%s'", string)));
        }

        public String toString() {
            return this.name;
        }

        public byte[] verify(byte[] byArray) {
            if (byArray.length != this.bytes) {
                throw new IllegalArgumentException("Invalid digest length: " + byArray.length);
            }
            return byArray;
        }
    }
}