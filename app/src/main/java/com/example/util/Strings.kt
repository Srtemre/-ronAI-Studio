package com.example.util

import com.example.domain.model.AppLanguage

object Strings {
    fun get(key: String, language: AppLanguage): String {
        val isTr = language == AppLanguage.TURKISH
        return when (key) {
            // App Title & Tagline
            "app_title" -> "HTML App Builder"
            "app_subtitle" -> if (isTr) "Web projelerinizi Android uygulamalarına dönüştürün." else "Turn your web projects into Android apps."
            "developed_by" -> if (isTr) "Geliştirici: ZONELAND" else "Developed by ZONELAND"
            
            // Navigation
            "nav_home" -> if (isTr) "Ana Sayfa" else "Home"
            "nav_projects" -> if (isTr) "Projeler" else "Projects"
            "nav_settings" -> if (isTr) "Ayarlar" else "Settings"
            
            // Home Screen
            "action_create_app" -> if (isTr) "Uygulama Oluştur" else "Create App"
            "action_pwa_builder" -> if (isTr) "PWA Dönüştürücü" else "PWA Builder"
            "recent_projects" -> if (isTr) "Son Projeler" else "Recent Projects"
            "no_projects_yet" -> if (isTr) "Henüz proje yok." else "No projects yet."
            "quick_stats_title" -> if (isTr) "Proje Özeti" else "Project Overview"
            "total_projects_count" -> if (isTr) "Toplam Yapılandırılmış Proje" else "Total Configured Projects"
            "view_all" -> if (isTr) "Tümünü Gör" else "View All"

            // Create Mode Selection Screen
            "create_mode_selection_title" -> if (isTr) "Yeni Uygulama Oluştur" else "Create New App"
            "create_mode_selection_subtitle" -> if (isTr) "Uygulamanızı nasıl oluşturmak istediğinizi seçin." else "Choose how you want to create your app."
            "mode_fast_title" -> if (isTr) "Hızlı Mod" else "Fast Mode"
            "mode_fast_badge" -> if (isTr) "Basit Kurulum" else "Simple setup"
            "mode_fast_desc" -> if (isTr) "Teknik yapılandırmaları sizin için otomatik olarak halleder." else "Automatically handles the technical configuration for you."
            "mode_expert_title" -> if (isTr) "Uzman Modu" else "Expert Mode"
            "mode_expert_badge" -> if (isTr) "Gelişmiş Kurulum" else "Advanced setup"
            "mode_expert_desc" -> if (isTr) "Uygulama yapılandırması ve derleme seçenekleri üzerinde tam kontrol." else "Full control over app configuration and build options."

            // App Icon Management
            "app_icon_title" -> if (isTr) "Uygulama Simgesi" else "App Icon"
            "upload_icon" -> if (isTr) "Simge Yükle" else "Upload Icon"
            "change_icon" -> if (isTr) "Simgeyi Değiştir" else "Change Icon"
            "remove_icon" -> if (isTr) "Simgeyi Kaldır" else "Remove Icon"
            "icon_preview" -> if (isTr) "Simge Önizlemesi" else "Icon Preview"
            "custom_icon_selected" -> if (isTr) "Özel simge yüklendi" else "Custom icon selected"

            // Fast Mode Screen
            "fast_create_title" -> if (isTr) "Hızlı Uygulama Oluşturucu" else "Fast App Creator"
            "fast_create_subtitle" -> if (isTr) "Otomatik yapılandırma ile hızlı kurulum" else "Quick setup with automated configuration"
            "fast_build_apk" -> if (isTr) "APK DERLE" else "BUILD APK"
            "fast_open_workspace" -> if (isTr) "Çalışma Alanında Aç" else "Open in Workspace"
            "select_zip_file" -> if (isTr) "ZIP Dosyası Seç" else "Choose ZIP File"
            "select_html_file" -> if (isTr) "HTML Dosyası Seç" else "Choose HTML File"
            "html_source_mode" -> if (isTr) "HTML Giriş Yöntemi" else "HTML Input Method"
            "html_mode_code" -> if (isTr) "Kod Yaz" else "Write Code"
            "html_mode_file" -> if (isTr) "Dosya Yükle" else "Upload File"
            "zip_file_selected" -> if (isTr) "ZIP arşivi seçildi" else "ZIP file selected"
            "html_file_selected" -> if (isTr) "HTML dosyası seçildi" else "HTML file selected"
            "auto_config_title" -> if (isTr) "Otomatik Yapılandırma" else "Automated Configuration"
            "auto_config_note" -> if (isTr) "Paket Kimliği, Android Manifesti, Web ayarları ve İmzalı Keystore sizin için otomatik olarak optimize edilir." else "Package ID, Android Manifest, Web settings, and Signing Keystore are automatically optimized for you."
            "build_in_progress" -> if (isTr) "APK Derleniyor..." else "Building APK..."
            "building_apk_fast" -> if (isTr) "Sürüm APK derleniyor ve imzalanıyor" else "Compiling and cryptographically signing release APK"
            
            // Create App Screen (Expert Mode)
            "title_create_app" -> if (isTr) "Yeni Uygulama Oluştur" else "Create New App"
            "source_type" -> if (isTr) "Kaynak Türü" else "Source Type"
            "app_metadata" -> if (isTr) "Uygulama Bilgileri" else "App Metadata"
            "app_name_label" -> if (isTr) "Uygulama Adı" else "App Name"
            "app_name_placeholder" -> if (isTr) "Örn. Web Mağazam" else "e.g. My Web Shop"
            "package_name_label" -> if (isTr) "Paket Kimliği (Package ID)" else "Package Name ID"
            "package_name_placeholder" -> "com.company.myapp"
            "version_label" -> if (isTr) "Sürüm Adı (Version Name)" else "Version Name"
            "version_code_label" -> if (isTr) "Sürüm Kodu (Version Code)" else "Version Code"
            "advanced_options" -> if (isTr) "Gelişmiş Yapılandırma" else "Advanced Settings"
            "target_url_label" -> if (isTr) "Hedef Web Adresi (URL)" else "Target Website URL"
            "target_url_placeholder" -> "https://example.com"
            "entry_file_label" -> if (isTr) "Giriş Dosyası" else "Entry File Name"
            "entry_file_placeholder" -> "index.html"
            "html_code_label" -> if (isTr) "HTML Kod İçeriği" else "HTML Source Code"
            "html_code_placeholder" -> "<html><body><h1>Hello World</h1></body></html>"
            
            // Display & Behavior Options
            "display_behavior" -> if (isTr) "Görüntü ve Davranış" else "Display & Behavior"
            "display_mode" -> if (isTr) "Görüntüleme Modu" else "Display Mode"
            "orientation" -> if (isTr) "Ekran Yönü" else "Orientation"
            "enable_javascript" -> if (isTr) "JavaScript Etkin" else "Enable JavaScript"
            "enable_local_storage" -> if (isTr) "Yerel Depolama (DOM Storage)" else "Enable Local Storage"
            "enable_offline_cache" -> if (isTr) "Çevrimdışı Önbellek Desteği" else "Enable Offline Caching"
            
            // Icon Style & Color
            "accent_color" -> if (isTr) "Vurgu Rengi" else "Accent Color"
            "save_project" -> if (isTr) "Projeyi Kaydet" else "Save Project"
            "project_saved_success" -> if (isTr) "Proje başarıyla kaydedildi" else "Project saved successfully"
            
            // Projects Screen
            "search_projects" -> if (isTr) "Projelerde Ara..." else "Search projects..."
            "project_details" -> if (isTr) "Proje Detayları" else "Project Details"
            "last_modified" -> if (isTr) "Son Güncelleme" else "Last Modified"
            "action_open" -> if (isTr) "Aç" else "Open"
            "action_edit" -> if (isTr) "Düzenle" else "Edit"
            "action_delete" -> if (isTr) "Sil" else "Delete"
            "duplicate_project" -> if (isTr) "Projeyi Çoğalt" else "Duplicate Project"
            "delete_confirm_title" -> if (isTr) "Projeyi Sil" else "Delete Project"
            "delete_confirm_message" -> if (isTr) "Bu projeyi silmek istediğinizden emin misiniz?" else "Are you sure you want to delete this project?"
            "cancel" -> if (isTr) "İptal" else "Cancel"
            "close" -> if (isTr) "Kapat" else "Close"
            "create" -> if (isTr) "Oluştur" else "Create"
            
            // Workspace & Tabs
            "tab_files" -> if (isTr) "Dosyalar" else "Files"
            "tab_editor" -> if (isTr) "HTML Düzenleyici" else "HTML Editor"
            "tab_preview" -> if (isTr) "Önizleme" else "Preview"
            "tab_build" -> if (isTr) "APK Oluştur" else "Build APK"
            "import_file_zip" -> if (isTr) "Dosya / ZIP İçe Aktar" else "Import File / ZIP"
            "new_file" -> if (isTr) "Yeni Dosya" else "New File"
            "no_files_message" -> if (isTr) "Projeye henüz dosya eklenmedi. Dosya veya ZIP içe aktarabilirsiniz." else "No files in project. Tap Import File / ZIP to add files."
            "project_storage" -> if (isTr) "Proje Depolama" else "Project Storage"
            "save_file" -> if (isTr) "Kaydet" else "Save"
            "saved" -> if (isTr) "Kaydedildi" else "Saved"
            "unsaved" -> if (isTr) "Kaydedilmemiş" else "Unsaved"
            "create_new_file_title" -> if (isTr) "Yeni Dosya Oluştur" else "Create New File"
            "create_new_file_desc" -> if (isTr) "Göreceli dosya yolunu girin (örn. style.css veya js/app.js):" else "Enter relative file path (e.g., style.css or js/app.js):"
            
            // Build Tab
            "build_completed" -> if (isTr) "Derleme Tamamlandı" else "Build Completed"
            "apk_ready" -> if (isTr) "APK Hazır" else "APK Ready"
            "apk_ready_desc" -> if (isTr) "Sürüm APK başarıyla derlendi ve dijital olarak imzalandı" else "Release APK successfully built & cryptographically signed"
            "package_name" -> if (isTr) "Paket Kimliği" else "Package Name"
            "apk_size" -> if (isTr) "APK Boyutu" else "APK Size"
            "signing_info" -> if (isTr) "Dijital İmza" else "Digital Signing"
            "download_apk" -> if (isTr) "APK İNDİR" else "DOWNLOAD APK"
            "build_again" -> if (isTr) "TEKRAR DERLE" else "BUILD AGAIN"
            "export_android_project" -> if (isTr) "ANDROID PROJESİNİ DIŞA AKTAR" else "EXPORT ANDROID PROJECT"
            "share_apk" -> if (isTr) "APK'yı Paylaş / Yükle" else "Share / Install Binary"
            "build_failed" -> if (isTr) "Derleme Başarısız" else "Build Failed"
            "phase" -> if (isTr) "Aşama" else "Phase"
            "explanation" -> if (isTr) "Açıklama:" else "Explanation:"
            "technical_details" -> if (isTr) "Teknik Ayrıntılar" else "Technical Details"
            "build_logs_title" -> if (isTr) "Derleme Günlükleri ve Tanılama" else "Build Logs & Diagnostics"
            "retry_build" -> if (isTr) "TEKRAR DENE" else "RETRY BUILD"
            "reset" -> if (isTr) "Sıfırla" else "Reset"
            "building_project" -> if (isTr) "Derleniyor:" else "Building"
            "release_target" -> if (isTr) "Hedef Yapı" else "Release Target"
            "keystore_config" -> if (isTr) "Dijital İmza Anahtarı" else "Digital Signing Keystore"
            "managed_keystore" -> if (isTr) "Otomatik Yönetilen Keystore" else "Managed Release Keystore"
            "managed_keystore_desc" -> if (isTr) "RSA 2048-bit / SHA-256 X.509 Sertifikası (30 Yıl Geçerli)" else "RSA 2048-bit / SHA-256 with X.509 Certificate (30-Year Validity)"
            "keystore_alias" -> if (isTr) "Keystore Takma Adı" else "Keystore Alias"
            "keystore_password" -> if (isTr) "Keystore Parolası" else "Keystore Password"
            "build_and_sign_apk" -> if (isTr) "Sürüm APK Derle ve İmzala" else "Build & Sign Release APK"
            "release_compilation_desc" -> if (isTr) "Çok adımlı derleme zincirini çalıştırır: proje doğrulama, manifest oluşturma, DEX baytkod paketleme, RSA-2048 imzalama ve APK bütünlük doğrulaması." else "Executes full multi-step pipeline: project validation, manifest generation, DEX bytecode packaging, cryptographic RSA-2048 signing, and APK integrity verification."
            
            // Settings Screen
            "appearance" -> if (isTr) "Görünüm" else "Appearance"
            "theme" -> if (isTr) "Tema" else "Theme"
            "theme_system" -> if (isTr) "Sistem" else "System"
            "theme_light" -> if (isTr) "Açık" else "Light"
            "theme_dark" -> if (isTr) "Koyu" else "Dark"
            "accent_colors" -> if (isTr) "Vurgu Renkleri" else "Accent Colors"
            "language" -> if (isTr) "Dil" else "Language"
            "data_and_storage" -> if (isTr) "Veri Yönetimi" else "Data Management"
            "export_projects" -> if (isTr) "Projeleri Dışa Aktar" else "Export Projects"
            "export_projects_subtitle" -> if (isTr) "Tüm projelerinizi ve dosyalarınızı ZIP yedek dosyası olarak kaydedin." else "Save all your projects and files as a ZIP backup archive."
            "import_projects" -> if (isTr) "Projeleri İçe Aktar" else "Import Projects"
            "import_projects_subtitle" -> if (isTr) "Önceden dışa aktarılmış bir ZIP yedeğini geri yükleyin." else "Restore projects from a previously exported ZIP backup."
            "clear_project_data" -> if (isTr) "Proje Verilerini Temizle" else "Clear Project Data"
            "clear_project_data_subtitle" -> if (isTr) "Tüm projeleri ve yerel depolamadaki dosyaları kalıcı olarak siler." else "Permanently removes all saved projects and local files."
            "clear_data_confirm" -> if (isTr) "Tüm projeler ve ilişkili web dosyaları kalıcı olarak silinecektir. Bu işlem geri alınamaz. Devam etmek istiyor musunuz?" else "All project configurations and workspace files will be permanently deleted. This action cannot be undone. Are you sure you want to proceed?"
            "about" -> if (isTr) "Hakkında" else "About"
            "about_app_desc" -> if (isTr) "Modern HTML, CSS, JavaScript ve PWA projelerini yerel Android APK uygulamalarına dönüştüren profesyonel mobil inşa platformu." else "Professional mobile platform for turning modern HTML, CSS, JavaScript, and PWA projects into native Android APK packages."
            "projects_exported_success" -> if (isTr) "Projeler başarıyla dışa aktarıldı." else "Projects exported successfully."
            "projects_imported_success" -> if (isTr) "proje başarıyla içe aktarıldı." else "projects imported successfully."
            "data_cleared_success" -> if (isTr) "Tüm proje verileri temizlendi." else "All project data has been cleared."
            "color_blue" -> if (isTr) "Mavi" else "Blue"
            "color_purple" -> if (isTr) "Mor" else "Purple"
            "color_green" -> if (isTr) "Yeşil" else "Green"
            "color_orange" -> if (isTr) "Turuncu" else "Orange"
            "color_pink" -> if (isTr) "Pembe" else "Pink"
            "color_teal" -> if (isTr) "Camgöbeği" else "Teal"
            
            else -> key
        }
    }
}

