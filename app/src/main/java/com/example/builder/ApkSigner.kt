package com.example.builder

import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.PrivateKey
import java.security.Signature
import java.security.cert.X509Certificate
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ApkSigner {

    fun signApk(
        unsignedApkFile: File,
        signedApkFile: File,
        privateKey: PrivateKey,
        certChain: Array<out java.security.cert.Certificate>
    ): File {
        val cert = certChain[0] as X509Certificate
        val digest = MessageDigest.getInstance("SHA-256")

        val manifestMfSb = StringBuilder()
        manifestMfSb.append("Manifest-Version: 1.0\r\n")
        manifestMfSb.append("Created-By: 1.0 (HTML App Builder Engine)\r\n\r\n")

        val fileEntries = mutableMapOf<String, ByteArray>()

        ZipInputStream(FileInputStream(unsignedApkFile)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory && !entry.name.startsWith("META-INF/")) {
                    val bytes = zis.readBytes()
                    fileEntries[entry.name] = bytes

                    val hash = digest.digest(bytes)
                    val base64Hash = Base64.encodeToString(hash, Base64.NO_WRAP)

                    manifestMfSb.append("Name: ${entry.name}\r\n")
                    manifestMfSb.append("SHA-256-Digest: $base64Hash\r\n\r\n")
                }
                entry = zis.nextEntry
            }
        }

        val manifestMfBytes = manifestMfSb.toString().toByteArray(Charsets.UTF_8)
        val manifestDigest = digest.digest(manifestMfBytes)
        val manifestDigestBase64 = Base64.encodeToString(manifestDigest, Base64.NO_WRAP)

        val certSfSb = StringBuilder()
        certSfSb.append("Signature-Version: 1.0\r\n")
        certSfSb.append("Created-By: 1.0 (HTML App Builder Engine)\r\n")
        certSfSb.append("SHA-256-Digest-Manifest: $manifestDigestBase64\r\n\r\n")

        val certSfBytes = certSfSb.toString().toByteArray(Charsets.UTF_8)

        // Sign CERT.SF with Private Key
        val sig = Signature.getInstance("SHA256withRSA")
        sig.initSign(privateKey)
        sig.update(certSfBytes)
        val signatureValue = sig.sign()

        // Construct CERT.RSA PKCS#7 block
        val certRsaBytes = buildPkcs7SignatureBlock(signatureValue, cert)

        // Write output signed APK ZIP
        ZipOutputStream(FileOutputStream(signedApkFile)).use { zos ->
            // Write standard files
            for ((name, data) in fileEntries) {
                val ze = ZipEntry(name)
                zos.putNextEntry(ze)
                zos.write(data)
                zos.closeEntry()
            }

            // Write META-INF/MANIFEST.MF
            val zeManifest = ZipEntry("META-INF/MANIFEST.MF")
            zos.putNextEntry(zeManifest)
            zos.write(manifestMfBytes)
            zos.closeEntry()

            // Write META-INF/CERT.SF
            val zeSf = ZipEntry("META-INF/CERT.SF")
            zos.putNextEntry(zeSf)
            zos.write(certSfBytes)
            zos.closeEntry()

            // Write META-INF/CERT.RSA
            val zeRsa = ZipEntry("META-INF/CERT.RSA")
            zos.putNextEntry(zeRsa)
            zos.write(certRsaBytes)
            zos.closeEntry()
        }

        return signedApkFile
    }

    private fun buildPkcs7SignatureBlock(
        signatureBytes: ByteArray,
        cert: X509Certificate
    ): ByteArray {
        val out = ByteArrayOutputStream()
        val writer = DerWriter(out)

        // PKCS#7 ContentInfo wrapping SignedData
        writer.writeSequence { seq ->
            seq.writeOid("1.2.840.113549.1.7.2") // signedData OID
            seq.writeExplicitTag(0) { exp ->
                exp.writeSequence { signedData ->
                    // CMSVersion (1)
                    signedData.writeInteger(1)
                    // DigestAlgorithms SET (SHA-256)
                    signedData.writeSet { algs ->
                        algs.writeSequence { alg ->
                            alg.writeOid("2.16.840.1.101.3.4.2.1") // SHA-256
                            alg.writeNull()
                        }
                    }
                    // EncapsulatedContentInfo
                    signedData.writeSequence { contentInfo ->
                        contentInfo.writeOid("1.2.840.113549.1.7.1") // data OID
                    }
                    // Certificates [0] IMPLICIT
                    signedData.writeImplicitTag(0) { certs ->
                        certs.writeRawBytes(cert.encoded)
                    }
                    // SignerInfos SET
                    signedData.writeSet { signerInfos ->
                        signerInfos.writeSequence { signerInfo ->
                            signerInfo.writeInteger(1) // version
                            // IssuerAndSerialNumber
                            signerInfo.writeSequence { issuerAndSerial ->
                                issuerAndSerial.writeRawBytes(cert.issuerX500Principal.encoded)
                                issuerAndSerial.writeInteger(cert.serialNumber)
                            }
                            // DigestAlgorithmIdentifier (SHA-256)
                            signerInfo.writeSequence { alg ->
                                alg.writeOid("2.16.840.1.101.3.4.2.1")
                                alg.writeNull()
                            }
                            // SignatureAlgorithmIdentifier (RSA)
                            signerInfo.writeSequence { alg ->
                                alg.writeOid("1.2.840.113549.1.1.1") // rsaEncryption
                                alg.writeNull()
                            }
                            // SignatureValue
                            signerInfo.writeOctetString(signatureBytes)
                        }
                    }
                }
            }
        }

        return out.toByteArray()
    }

    private class DerWriter(private val out: ByteArrayOutputStream) {
        fun writeTag(tag: Int, content: ByteArray) {
            out.write(tag)
            writeLength(content.size)
            out.write(content)
        }

        fun writeLength(len: Int) {
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
        }

        fun writeSequence(block: (DerWriter) -> Unit) {
            val sub = ByteArrayOutputStream()
            val subWriter = DerWriter(sub)
            block(subWriter)
            writeTag(0x30, sub.toByteArray())
        }

        fun writeSet(block: (DerWriter) -> Unit) {
            val sub = ByteArrayOutputStream()
            val subWriter = DerWriter(sub)
            block(subWriter)
            writeTag(0x31, sub.toByteArray())
        }

        fun writeExplicitTag(tagNo: Int, block: (DerWriter) -> Unit) {
            val sub = ByteArrayOutputStream()
            val subWriter = DerWriter(sub)
            block(subWriter)
            writeTag(0xA0 or tagNo, sub.toByteArray())
        }

        fun writeImplicitTag(tagNo: Int, block: (DerWriter) -> Unit) {
            val sub = ByteArrayOutputStream()
            val subWriter = DerWriter(sub)
            block(subWriter)
            writeTag(0xA0 or tagNo, sub.toByteArray())
        }

        fun writeInteger(value: Long) {
            writeTag(0x02, java.math.BigInteger.valueOf(value).toByteArray())
        }

        fun writeInteger(value: java.math.BigInteger) {
            writeTag(0x02, value.toByteArray())
        }

        fun writeNull() {
            out.write(0x05)
            out.write(0x00)
        }

        fun writeOid(oid: String) {
            val parts = oid.split(".").map { it.toInt() }
            val bytes = ByteArrayOutputStream()
            bytes.write(parts[0] * 40 + parts[1])
            for (i in 2 until parts.size) {
                var v = parts[i]
                val tmp = ByteArrayOutputStream()
                tmp.write(v and 0x7F)
                v = v shr 7
                while (v > 0) {
                    tmp.write((v and 0x7F) or 0x80)
                    v = v shr 7
                }
                val rev = tmp.toByteArray().reversedArray()
                bytes.write(rev)
            }
            writeTag(0x06, bytes.toByteArray())
        }

        fun writeOctetString(bytes: ByteArray) {
            writeTag(0x04, bytes)
        }

        fun writeRawBytes(bytes: ByteArray) {
            out.write(bytes)
        }
    }
}
