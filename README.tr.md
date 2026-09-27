# Email Client

[English](README.md) | **Türkçe**

Java ve JavaFX ile yazılmış masaüstü e-posta istemcisi. Jakarta Mail ile IMAP üzerinden e-postaları okur, SMTP üzerinden gönderir.

> Geliştirme sürüyor. Aşağıdaki plan, proje v1.0'a ulaştığında tam README'ye dönüşecek.

## Özellikler (plan)

**MVP**
- [ ] Hesap kurulumu: e-posta adresi ve uygulama şifresi; Gmail, Yandex ve iCloud için hazır ayarlar ya da özel sunucu; "Bağlantıyı test et" düğmesi
- [ ] Gelen kutusu (IMAP): klasörler, "daha fazla yükle" ile son mesajlar, okuma bölmesi
- [ ] Okundu / okunmadı işareti, silme (çöpe taşıma), yüklenen mesajlarda arama
- [ ] Gönderme (SMTP): yeni mesaj, yanıtla, ilet; alıcı adresi doğrulama
- [ ] Ağ işlemleri arka planda ve yükleniyor göstergesiyle, arayüz donmaz
- [ ] Şifre Windows DPAPI ile şifrelenir (asla düz metin saklanmaz); geliştirme ve testler için ortam değişkeni desteği
- [ ] HTML e-postalar düz metin olarak gösterilir (ham HTML asla render edilmez)
- [ ] Ayarlar kullanıcı klasöründe, uygulama ikonu, sürüm numarası, Hakkında penceresi
- [ ] Birim ve entegrasyon testleri (JUnit 5 + bellekte çalışan GreenMail e-posta sunucusu)
- [ ] Windows kurulum dosyası (jpackage + Inno Setup)

**Sonra eklenecekler**
- Eklerin görüntülenmesi ve kaydedilmesi
- HTML görünümü
- Birden fazla hesap
- Yeni e-posta bildirimi
- OAuth2 ile giriş (Outlook / Hotmail için gerekli)

## Kullanılan Teknolojiler

- Java 21, Maven
- JavaFX
- Jakarta Mail (Eclipse Angus)
- JUnit 5, GreenMail
- jpackage, Inno Setup
