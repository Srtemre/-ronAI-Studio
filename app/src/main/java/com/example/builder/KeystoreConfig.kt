package com.example.builder

import java.io.File

data class KeystoreConfig(
    val alias: String = "releasekey",
    val storePassword: String = "auto_managed_store_pass_sec_982",
    val keyPassword: String = "auto_managed_key_pass_sec_982",
    val customKeystorePath: String? = null,
    val isCustomKeystore: Boolean = false,
    val validityYears: Int = 30
) {
    fun getMaskedStorePassword(): String = "••••••••••••"
    fun getMaskedKeyPassword(): String = "••••••••••••"
}
