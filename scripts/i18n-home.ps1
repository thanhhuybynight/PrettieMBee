$path = "C:\Users\anhiutangerine\PrettieMBee\app\src\main\java\anhiutangerine\prettiembee\ui\screens\HomeScreen.kt"
$c = Get-Content $path -Raw -Encoding UTF8

$pairs = @(
  @('"Đổi gói MB Bank mục tiêu"', 'stringResource(R.string.package_dialog_title)')
  @('"Thông tin hệ thống"', 'stringResource(R.string.home_system_info)')
  @('"Phiên bản MB Bank"', 'stringResource(R.string.home_mb_version)')
  @('"Phiên bản Android"', 'stringResource(R.string.home_android_version)')
  @('"Thiết bị"', 'stringResource(R.string.home_device)')
  @('"Phiên bản PrettieMBee"', 'stringResource(R.string.home_app_version)')
  @('"Tìm theme theo tên hoặc tác giả..."', 'stringResource(R.string.store_search_hint)')
  @('"Đồng bộ từ GitHub"', 'stringResource(R.string.store_sync_github)')
  @('"Cấu hình MB Bank"', 'stringResource(R.string.settings_mb_config)')
  @('"Gói ứng dụng mục tiêu"', 'stringResource(R.string.settings_target_package)')
  @('"Theme đang kích hoạt"', 'stringResource(R.string.settings_active_theme)')
  @('"Giao diện & Chủ đề"', 'stringResource(R.string.settings_ui_theme)')
  @('"Chế độ nền"', 'stringResource(R.string.settings_theme_mode)')
  @('"Màu chủ đạo"', 'stringResource(R.string.settings_accent_color)')
  @('"Đang dùng màu từ hình nền"', 'stringResource(R.string.accent_seed_active)')
  @('"Lấy màu từ hình nền"', 'stringResource(R.string.settings_seed_color)')
  @('"Tự động trích xuất màu nhấn từ ảnh nền app"', 'stringResource(R.string.settings_seed_color_on)')
  @('"Cần đặt hình nền app trước"', 'stringResource(R.string.settings_seed_color_off)')
  @('"Nền Status Card"', 'stringResource(R.string.settings_status_card_bg)')
  @('"Đã cài ảnh nền riêng cho thẻ"', 'stringResource(R.string.settings_status_card_bg_set)')
  @('"Nền toàn ứng dụng"', 'stringResource(R.string.settings_app_bg)')
  @('"Đã cài hình nền app"', 'stringResource(R.string.settings_app_bg_set)')
  @('"Độ tối nền"', 'stringResource(R.string.settings_bg_dim)')
  @('"Điều chỉnh độ sẫm của hình nền app"', 'stringResource(R.string.settings_bg_dim_desc)')
  @('"Độ trong suốt thẻ (Card Alpha)"', 'stringResource(R.string.settings_card_alpha)')
  @('"Độ mờ thẻ hiển thị xuyên hình nền"', 'stringResource(R.string.settings_card_alpha_desc)')
  @('"DPI ứng dụng"', 'stringResource(R.string.settings_app_dpi)')
  @('"Hệ thống & Dữ liệu"', 'stringResource(R.string.settings_system_data)')
  @('"Làm mới trạng thái"', 'stringResource(R.string.settings_refresh_status)')
  @('"Quét lại quyền root và kiểm tra ứng dụng MB"', 'stringResource(R.string.settings_refresh_status_desc)')
  @('"Thư mục lưu trữ theme"', 'stringResource(R.string.settings_theme_folder)')
  @('"Xoá toàn bộ theme MB Bank"', 'stringResource(R.string.settings_reset_themes)')
  @('"Gỡ bỏ tất cả theme đã nạp và hoàn tác về mặc định"', 'stringResource(R.string.settings_reset_themes_desc)')
  @('"Yêu cầu quyền root để thực hiện!"', 'context.getString(R.string.reset_needs_root)')
  @('"Mặc định (Theo hệ thống)"', 'stringResource(R.string.dpi_value)')
  @('"Mật độ hiển thị (DPI)"', 'stringResource(R.string.dpi_dialog_title)')
  @('"Điều chỉnh tỷ lệ giao diện riêng cho PrettieMBee:"', 'stringResource(R.string.dpi_dialog_hint)')
  @('"Tuỳ chỉnh:"', 'stringResource(R.string.dpi_custom)')
  @('"Khôi phục theme mặc định?"', 'stringResource(R.string.reset_dialog_title)')
  @('"Chế độ giao diện"', 'stringResource(R.string.theme_mode_dialog_title)')
  @('"Đã xoá toàn bộ theme và khôi phục mặc định thành công!"', 'context.getString(R.string.reset_success)')
  @('"Đã cập nhật ảnh nền Status Card"', 'context.getString(R.string.toast_status_bg_set)')
  @('"Đã áp dụng hình nền toàn ứng dụng"', 'context.getString(R.string.toast_app_bg_set)')
  @('"Đã xoá nền thẻ"', 'context.getString(R.string.toast_status_bg_removed)')
  @('"Đã xoá hình nền ứng dụng"', 'context.getString(R.string.toast_app_bg_removed)')
  @('"Nạp ZIP"', 'stringResource(R.string.store_load_zip)')
  @('"Tất cả"', 'stringResource(R.string.store_all)')
  @('"Lưu"', 'stringResource(R.string.action_save)')
  @('"Huỷ"', 'stringResource(R.string.action_cancel)')
  @('"Đặt lại"', 'stringResource(R.string.action_reset)')
  @('"Chọn ảnh"', 'stringResource(R.string.action_choose_image)')
  @('"Đổi"', 'stringResource(R.string.action_change)')
)

foreach ($p in $pairs) {
  $c = $c.Replace($p[0], $p[1])
}

$c = $c.Replace('"${filteredThemes.size} theme"', 'stringResource(R.string.store_theme_count, filteredThemes.size)')
$c = $c.Replace('ThemeConfig.themeMode.title', 'stringResource(ThemeConfig.themeMode.titleRes)')
$c = $c.Replace('ThemeConfig.themeAccent.title', 'stringResource(ThemeConfig.themeAccent.titleRes)')
$c = $c.Replace('it.title', 'stringResource(it.titleRes)')
$c = $c.Replace('text = "PrettieMBee"', 'text = stringResource(R.string.home_title)')
$c = $c.Replace('"Mặc định (Không nền)"', 'stringResource(R.string.settings_status_card_bg_none)')
$c = $c.Replace('"Đang làm mới dữ liệu và đồng bộ kho theme..."', 'context.getString(R.string.toast_refreshing)')

if ($c -notmatch 'import androidx.compose.ui.res.stringResource') {
  $c = $c.Replace(
    'import androidx.compose.ui.res.painterResource',
    "import androidx.compose.ui.res.painterResource`nimport androidx.compose.ui.res.stringResource"
  )
}

# Add onLanguageChange param after onResetThemes
if ($c -notmatch 'onLanguageChange') {
  $c = $c.Replace(
    'onResetThemes: (suspend () -> Result<Unit>)? = null,',
    "onResetThemes: (suspend () -> Result<Unit>)? = null,`n    onLanguageChange: (String) -> Unit = {},"
  )
}

Set-Content -Path $path -Value $c -Encoding UTF8
Write-Output "done"
Select-String -Path $path -Pattern 'stringResource' | Measure-Object | Select-Object Count
Select-String -Path $path -Pattern 'onLanguageChange' | ForEach-Object { $_.Line.Trim() }