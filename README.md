# PrettieMBee - MBCP Community Themes Store

Kho lưu trữ theme cộng đồng chính thức dành cho ứng dụng **PrettieMBee**. Nhánh `theme` được lưu trữ độc lập để cung cấp trực tiếp tài nguyên giao diện cho ứng dụng di động qua mạng CDN GitHub.

---

## 🎨 Danh sách Theme hiện có

| ID | Tên Theme | Chủ đề / Series | Tác giả | Kích thước |
|---|---|---|---|---|
| `furina` | Furina (Regina of Waters) | Genshin Impact | MBCP Community | 7.4 MB |
| `elysia` | Elysia (Miss Pink Elf) | Honkai Impact 3rd | MBCP Community | 10.8 MB |
| `hina` | Sorasaki Hina (Gehenna) | Blue Archive | MBCP Community | 30.6 MB |
| `seia` | Yurizono Seia (Tea Party) | Blue Archive | MBCP Community | 5.9 MB |
| `pitchblack` | Pitch Black (AMOLED Dark) | Minimalist / Utility | Fukiame | 4.0 MB |
| `march7th` | March 7th (Astral Express) | Honkai: Star Rail | MBCP Community | 11.1 MB |
| `varesa` | Varesa Pastel Gradient | Aesthetic | MBCP Community | 10.9 MB |

---

## 📂 Cấu trúc thư mục

```
.
├── community_catalog.json   # Tệp danh mục chính thức được ứng dụng đọc
├── themes/
│   ├── elysia.zip
│   ├── furina.zip
│   ├── hina.zip
│   ├── march7th.zip
│   ├── pitchblack.zip
│   ├── seia.zip
│   └── varesa.zip
└── README.md
```

Mỗi file ZIP đều tuân thủ cấu trúc nạp theme tiêu chuẩn:
- `images/`: Chứa các tài nguyên hình ảnh được tùy biến (PNG/JPG)
- `theme/token.json`: Định nghĩa bảng màu và style giao diện
- `theme/token_priority.json`: Bảng màu tối ưu hóa dành cho tài khoản MB Priority (nếu có)
