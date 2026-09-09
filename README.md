# 🐝 PrettieMBee

<div align="center">

![PrettieMBee Banner](https://raw.githubusercontent.com/anhiutangerine/PrettieMBee/main/app/src/main/res/drawable/ic_launcher_foreground.xml)

**Ứng dụng Root Quản lý & Áp dụng Theme Tuỳ Chỉnh cho MB Bank (`com.mbmobile`)**

[![Build PrettieMBee APK](https://github.com/anhiutangerine/PrettieMBee/actions/workflows/build.yml/badge.svg)](https://github.com/anhiutangerine/PrettieMBee/actions/workflows/build.yml)
[![Platform](https://img.shields.io/badge/Platform-Android%20Root-brightgreen.svg)](https://github.com/anhiutangerine/PrettieMBee)
[![Package](https://img.shields.io/badge/Package-anhiutangerine.prettiembee-blue.svg)](https://github.com/anhiutangerine/PrettieMBee)
[![License](https://img.shields.io/badge/License-GPL--3.0-orange.svg)](LICENSE)

[Tính năng](#-tính-năng-nổi-bật) • [Cài đặt](#-cài-đặt--yêu-cầu) • [Cách sử dụng](#-hướng-dẫn-sử-dụng) • [Khắc phục sự cố](#-khắc-phục-sự-cố-tại-sao-app-này-tốt-hơn-magisk-module) • [Xây dựng APK](#-hướng-dẫn-build-apk)

</div>

---

## 🌟 Giới thiệu

**PrettieMBee** là ứng dụng Android mã nguồn mở được phát triển riêng cho các thiết bị đã Root (hỗ trợ toàn diện **Magisk**, **KernelSU**, **APatch**), cho phép người dùng tùy biến giao diện của ứng dụng **MB Bank (`com.mbmobile`)** theo phong cách cá nhân hóa (Anime, Minimalist AMOLED Dark, Pastel...) một cách an toàn và tiện lợi.

Trước đây, việc đổi theme MB thường dựa vào các module Magisk rời rạc (như `MBCP_Furina.zip`), vốn có rất nhiều nhược điểm:
- Bị gán cứng (hardcode) vào duy nhất một UUID theme gốc.
- Không thể áp dụng nếu người dùng chưa tải theme đó từ MB Store.
- Thường xuyên bị lỗi mất phân quyền hoặc SELinux khiến app MB Bank văng về theme mặc định.

**PrettieMBee** ra đời nhằm giải quyết triệt để tất cả các hạn chế trên!

---

## ✨ Tính năng nổi bật

- 🎯 **Ánh xạ Linh Hoạt (Dynamic Mapping):** Cho phép bạn áp dụng bất kỳ theme cộng đồng nào (Furina, Elysia, Hina...) vào **BẤT KỲ theme nào** trong kho 33 theme gốc của MB Store hoặc theme bạn đang dùng trên máy.
- 🔒 **Tự động sửa Lỗi Quyền Hạn & SELinux:** 
  - Tự động phát hiện chính xác `UID:GID` của MB Bank (`stat -c '%u:%g'`).
  - Thiết lập lại quyền POSIX chuẩn (`chmod 755` thư mục, `chmod 644` file).
  - Tự động khôi phục ngữ cảnh SELinux (`restorecon -R`) tránh triệt để lỗi *Permission Denied*.
- 👑 **Hỗ trợ Chế độ Priority (Tài khoản Ưu tiên):** Tích hợp sẵn biến thể `token_priority.json` tối ưu màu sắc và độ tương phản cho giao diện tài khoản MB Priority / Private.
- ⚡ **Kích hoạt 1 chạm qua Deeplink:** Tự động phát Intent `mbbank://installingnew` mở ngay màn hình chi tiết theme để người dùng bấm "Áp dụng" trong nháy mắt.
- 📦 **Tích hợp sẵn 7+ Theme Cộng Đồng Hot:**
  - 🌊 **Furina (Regina of Waters)** - *Genshin Impact*
  - 🌸 **Elysia (Miss Pink Elf)** - *Honkai Impact 3rd*
  - 💜 **Sorasaki Hina (Gehenna)** - *Blue Archive*
  - 💛 **Yurizono Seia (Tea Party)** - *Blue Archive*
  - 🖤 **Pitch Black AMOLED Dark** - *Fukiame Minimalist*
  - ❄️ **March 7th (Astral Express)** - *Honkai: Star Rail*
  - 🎨 **Varesa Pastel Gradient** - *Aesthetic Minimalist*

---

## 📱 Cài đặt & Yêu cầu

1. **Thiết bị:** Android 8.0 (API 26) trở lên.
2. **Quyền Root:** Magisk v24+, KernelSU v0.6+, hoặc APatch.
3. **Ứng dụng ngân hàng:** Đã cài đặt MB Bank (`com.mbmobile`).

---

## 🚀 Hướng dẫn sử dụng

1. Tải và cài đặt file `PrettieMBee.apk`.
2. Mở **PrettieMBee** và cấp quyền **Superuser (Root)** khi có hộp thoại nhắc.
3. Tại trang chủ:
   - Kiểm tra xem app đã nhận diện được **Quyền Root** và **MB Bank** hay chưa.
   - Chọn một theme bạn yêu thích trong danh sách (hoặc lọc theo nhóm Genshin / Honkai / Blue Archive...).
4. Trong bảng cấu hình theme:
   - Xem theme đích mặc định. Nếu muốn đổi sang theme MB khác, nhấn **"Đổi"** và chọn một theme bất kỳ trong danh mục 33 theme của MB Bank.
   - Bật **"Chế độ Priority"** nếu tài khoản ngân hàng của bạn là Priority.
   - Nhấn **"Áp Dụng Theme Ngay"**.
5. Đợi thanh tiến trình hoàn tất. MB Bank sẽ tự động mở trang chi tiết theme, bạn chỉ cần bấm nút **"Áp dụng" (Apply)** để hoàn tất!

---

## 🛠️ Hướng dẫn Build APK

### 1. Tự động qua GitHub Actions
Kho lưu trữ này đã được cấu hình sẵn CI/CD trong `.github/workflows/build.yml`. Mỗi khi bạn push code hoặc tạo tag phát hành, GitHub Actions sẽ tự động biên dịch và tạo file APK trong mục **Artifacts** / **Releases**.

### 2. Biên dịch thủ công trên máy tính
```bash
git clone https://github.com/anhiutangerine/PrettieMBee.git
cd PrettieMBee

# Cấp quyền chạy cho Gradle wrapper
chmod +x gradlew

# Biên dịch Debug APK
./gradlew assembleDebug

# File APK được tạo tại:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚖️ Tuyên bố miễn trừ trách nhiệm (Disclaimer)

- **PrettieMBee** chỉ can thiệp vào các tài nguyên hình ảnh và màu sắc hiển thị nằm trong bộ nhớ đệm (`app_flutter/app_theme`) của MB Bank.
- Ứng dụng **KHÔNG can thiệp**, **KHÔNG chỉnh sửa mã nguồn**, **KHÔNG sửa bytecode (`.dex`)**, và **KHÔNG bypass bất kỳ cơ chế xác thực bảo mật** nào của ngân hàng.
- Dự án được phát triển hoàn toàn vì mục đích nghiên cứu giao diện và cá nhân hóa thẩm mỹ phi thương mại. Người dùng tự chịu trách nhiệm khi sử dụng trên thiết bị cá nhân.

---

<div align="center">
Made with 💛 by anhiutangerine & MBCP Community
</div>
