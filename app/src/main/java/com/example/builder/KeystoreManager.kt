package com.example.builder

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.Signature
import java.security.cert.Certificate
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.security.auth.x500.X500Principal

class KeystoreManager(private val context: Context) {

    private val keystoreDir: File
        get() = File(context.filesDir, "keystores").apply { if (!exists()) mkdirs() }

    private val defaultKeystoreFile: File
        get() = File(keystoreDir, "managed_release.p12")

    fun getOrCreateKeystore(config: KeystoreConfig): Pair<KeyStore, KeyStore.PrivateKeyEntry> {
        val targetFile = if (config.isCustomKeystore && !config.customKeystorePath.isNullOrBlank()) {
            File(config.customKeystorePath)
        } else {
            defaultKeystoreFile
        }

        val storePasswordChar = config.storePassword.toCharArray()
        val keyPasswordChar = config.keyPassword.toCharArray()

        val ks = KeyStore.getInstance("PKCS12")

        if (targetFile.exists() && targetFile.length() > 0) {
            FileInputStream(targetFile).use { fis ->
                ks.load(fis, storePasswordChar)
            }
            if (ks.containsAlias(config.alias)) {
                val entry = ks.getEntry(
                    config.alias,
                    KeyStore.PasswordProtection(keyPasswordChar)
                ) as? KeyStore.PrivateKeyEntry
                if (entry != null) {
                    return Pair(ks, entry)
                }
            }
        }

        // Generate new keypair & keystore
        val generatedPair = createNewKeystore(targetFile, config)
        return generatedPair
    }

    private fun createNewKeystore(
        targetFile: File,
        config: KeystoreConfig
    ): Pair<KeyStore, KeyStore.PrivateKeyEntry> {
        val keyGen = KeyPairGenerator.getInstance("RSA")
        keyGen.initialize(2048, SecureRandom())
        val keyPair = keyGen.generateKeyPair()

        val cert = generateSelfSignedCertificate(keyPair, config.alias, config.validityYears)

        val ks = KeyStore.getInstance("PKCS12")
        ks.load(null, null)

        val certChain = arrayOf<Certificate>(cert)
        ks.setKeyEntry(
            config.alias,
            keyPair.private,
            config.keyPassword.toCharArray(),
            certChain
        )

        FileOutputStream(targetFile).use { fos ->
            ks.store(fos, config.storePassword.toCharArray())
        }

        val entry = KeyStore.PrivateKeyEntry(keyPair.private, certChain)
        return Pair(ks, entry)
    }

    fun saveCustomKeystore(
        inputStream: java.io.InputStream,
        filename: String,
        config: KeystoreConfig
    ): File {
        val customFile = File(keystoreDir, filename)
        FileOutputStream(customFile).use { fos ->
            inputStream.copyTo(fos)
        }
        return customFile
    }

    private fun generateSelfSignedCertificate(
        keyPair: KeyPair,
        alias: String,
        validityYears: Int
    ): X509Certificate {
        val startDate = Date()
        val endDate = Date(startDate.time + (validityYears.toLong() * 365 * 24 * 60 * 60 * 1000L))
        val serialNumber = BigInteger(64, SecureRandom())

        val dn = "CN=$alias, O=HTML App Builder, C=US"
        
        // Construct basic X.509 v3 self-signed DER certificate
        return createX509Certificate(keyPair, dn, startDate, endDate, serialNumber)
    }

    private fun createX509Certificate(
        keyPair: KeyPair,
        dn: String,
        startDate: Date,
        endDate: Date,
        serialNumber: BigInteger
    ): X509Certificate {
        val certBytes = buildV3CertDer(keyPair, dn, startDate, endDate, serialNumber)
        val certFactory = CertificateFactory.getInstance("X.509")
        return certFactory.generateCertificate(certBytes.inputStream()) as X509Certificate
    }

    private fun buildV3CertDer(
        keyPair: KeyPair,
        dn: String,
        startDate: Date,
        endDate: Date,
        serial: BigInteger
    ): ByteArray {
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(keyPair.private)

        val tbsContent = ByteArrayOutputStream()

        // 1. Version [0] EXPLICIT INTEGER (2 = v3)
        val v3Bytes = encodeTag(0x02, BigInteger.valueOf(2).toByteArray())
        tbsContent.write(encodeTag(0xA0, v3Bytes))

        // 2. Serial number INTEGER
        tbsContent.write(encodeTag(0x02, serial.toByteArray()))

        // 3. Signature algorithm (1.2.840.113549.1.1.11 sha256WithRSAEncryption + NULL)
        val sha256RsaOid = byteArrayOf(0x2A.toByte(), 0x86.toByte(), 0x48.toByte(), 0x86.toByte(), 0xF7.toByte(), 0x0D.toByte(), 0x01.toByte(), 0x01.toByte(), 0x0B.toByte())
        val algIdSeq = ByteArrayOutputStream().apply {
            write(encodeTag(0x06, sha256RsaOid))
            write(byteArrayOf(0x05, 0x00)) // NULL
        }.toByteArray()
        tbsContent.write(encodeTag(0x30, algIdSeq))

        // 4. Issuer Name
        val nameSeq = createDerName(dn)
        tbsContent.write(nameSeq)

        // 5. Validity (UTCTime)
        val sdf = SimpleDateFormat("yyMMddHHmmss'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val validitySeq = ByteArrayOutputStream().apply {
            write(encodeTag(0x17, sdf.format(startDate).toByteArray(Charsets.US_ASCII)))
            write(encodeTag(0x17, sdf.format(endDate).toByteArray(Charsets.US_ASCII)))
        }.toByteArray()
        tbsContent.write(encodeTag(0x30, validitySeq))

        // 6. Subject Name (same as issuer for self-signed)
        tbsContent.write(nameSeq)

        // 7. SubjectPublicKeyInfo
        tbsContent.write(keyPair.public.encoded)

        val tbsSeq = encodeTag(0x30, tbsContent.toByteArray())

        // Sign the TBS sequence
        sig.update(tbsSeq)
        val signature = sig.sign()

        // 8. Full Certificate SEQUENCE
        val fullCert = ByteArrayOutputStream().apply {
            write(tbsSeq)
            write(encodeTag(0x30, algIdSeq))
            // BIT STRING signature
            val bitStr = ByteArrayOutputStream().apply {
                write(0x00) // 0 unused bits
                write(signature)
            }.toByteArray()
            write(encodeTag(0x03, bitStr))
        }.toByteArray()

        return encodeTag(0x30, fullCert)
    }

    private fun createDerName(dn: String): ByteArray {
        val cnVal = dn.substringAfter("CN=").substringBefore(",")
        // commonName OID: 2.5.4.3 = 55 04 03
        val cnOid = byteArrayOf(0x55, 0x04, 0x03)
        val attrValue = ByteArrayOutputStream().apply {
            write(encodeTag(0x06, cnOid))
            write(encodeTag(0x0C, cnVal.toByteArray(Charsets.UTF_8))) // UTF8String
        }.toByteArray()

        val rdnSet = encodeTag(0x31, encodeTag(0x30, attrValue)) // SET OF AttributeTypeAndValue
        return encodeTag(0x30, rdnSet) // SEQUENCE OF RDN
    }

    private fun encodeTag(tag: Int, value: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(tag)
        val len = value.size
        if (len < 128) {
            out.write(len)
        } else if (len < 256) {
            out.write(0x81)
            out.write(len)
        } else if (len < 65536) {
            out.write(0x82)
            out.write(len shr 8)
            out.write(len and 0xFF)
        } else {
            out.write(0x83)
            out.write(len shr 16)
            out.write((len shr 8) and 0xFF)
            out.write(len and 0xFF)
        }
        out.write(value)
        return out.toByteArray()
    }
}
