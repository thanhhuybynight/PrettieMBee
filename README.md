# PrettieMBee - MBCP Community Themes Store

Kho lưu trữ theme cộng đồng chính thức dành cho ứng dụng **PrettieMBee**. Nhánh `theme` được lưu trữ độc lập để cung cấp trực tiếp tài nguyên giao diện cho ứng dụng di động qua mạng CDN GitHub.

---

## Danh sách Theme hiện có

| ID | Tên Theme | Chủ đề / Series | Tác giả | Kích thước |
|---|---|---|---|---|
| `furina` | Furina (Regina of Waters) | Genshin Impact | @TsumugiShiina0 | 7.4 MB |
| `ganyu` | Ganyu (Secretary of Yuehai Pavilion) | Genshin Impact | cuynu, TsumugiShiina0 | 5.1 MB |
| `castorice` | Castorice | Honkai | cuynu, @nguyencaoantuong | 21.8 MB |
| `miku` | Miku (Vocaloid) | Vocaloid | cuynu, @nguyencaoantuong | 11.1 MB |
| `mikuv2` | Miku V2 (Full Assets) | Vocaloid | @nguyencaoantuong | 10.5 MB |
| `lilith` | Lilith | Original / Fantasy | @TsumugiShiina0 | 9.7 MB |
| `cloudy` | Cloudy | Aesthetic | cuynu, @minhwuangbeo | 12.8 MB |
| `nightlyirl` | Nightly IRL | Aesthetic | cuynu | 31.5 MB |
| `hoshino` | Hoshino (Blue Archive) | Blue Archive | TsumugiShiina0, Meowice | 33.3 MB |
| `paimon` | Paimon (Emergency Food) | Genshin Impact | TsumugiShiina0 | 14.0 MB |
| `elysia` | Elysia (Miss Pink Elf) | Honkai Impact 3rd | MBCP Community | 10.8 MB |
| `hina` | Sorasaki Hina (Gehenna) | Blue Archive | MBCP Community | 30.6 MB |
| `seia` | Yurizono Seia (Tea Party) | Blue Archive | MBCP Community | 5.9 MB |
| `pitchblack` | Pitch Black (AMOLED Dark) | Minimalist / Utility | Fukiame | 4.0 MB |
| `march7th` | March 7th (Astral Express) | Honkai: Star Rail | MBCP Community | 11.1 MB |
| `varesa` | Varesa Pastel Gradient | Aesthetic | MBCP Community | 10.9 MB |

---

## Cấu trúc thư mục

```
.
├── community_catalog.json   # Tệp danh mục chính thức được ứng dụng đọc
├── themes/
│   ├── castorice.zip
│   ├── cloudy.zip
│   ├── elysia.zip
│   ├── furina.zip
│   ├── ganyu.zip
│   ├── hina.zip
│   ├── hoshino.zip
│   ├── lilith.zip
│   ├── march7th.zip
│   ├── miku.zip
│   ├── mikuv2.zip
│   ├── nightlyirl.zip
│   ├── paimon.zip
│   ├── pitchblack.zip
│   ├── seia.zip
│   └── varesa.zip
└── README.md
```

Mỗi file ZIP đều tuân thủ cấu trúc nạp theme tiêu chuẩn:
- `images/`: Chứa các tài nguyên hình ảnh được tùy biến (PNG)
- `theme/token.json`: Định nghĩa bảng màu và style giao diện
- `theme/token_priority.json`: Bảng màu tối ưu hóa dành cho tài khoản MB Priority (nếu có)

Các theme được convert từ gói Magisk MBCP (`MBCP_*.zip`) đã được unwrap `.bin` lồng nhau và flatten về cấu trúc chuẩn PrettieMBee.

---

## Nguồn

- Gốc Magisk/MBCP: cộng đồng MBCP (cuynu, TsumugiShiina0, Meowice, nguyencaoantuong, Fukiame…)
- App PrettieMBee: https://github.com/thanhhuybynight/PrettieMBee

## License

Xem LICENSE ở repo chính.
