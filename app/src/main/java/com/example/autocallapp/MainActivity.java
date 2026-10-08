package com.example.autocallapp;

import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import java.io.File;
import java.io.FileWriter;

public class MainActivity extends AppCompatActivity {
    private WebView webView;
    private static final String TARGET_URL = "https://v9.800best.com/sea-web/?layout=TopLayout&loginOrgCode=102462&lang=vi-VN&loginSiteName=260000169&loginUserName=732683489&loginSiteId=260000169&loginUserId=732683489&loginUser=NVTH1&loginSite=FS+T%C3%82N+TH%E1%BA%A0NH+%C4%90%C3%94NG#/common-web/order/siteKanban";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo WebView ngầm xử lý cookie và cào dữ liệu
        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(), "AndroidBridge");

        Button btnLoginWeb = findViewById(R.id.btnLoginWeb);
        Button btnFilterCancel = findViewById(R.id.btnFilterCancel);

        // 1. Nút mở web đăng nhập thủ công (Cookie tự động lưu trên thiết bị)
        btnLoginWeb.setOnClickListener(v -> {
            webView.setWebViewClient(new WebViewClient());
            webView.loadUrl("https://v9.800best.com/");
            setContentView(webView);
            Toast.makeText(this, "Hãy đăng nhập tài khoản. Xong xuôi bấm nút Back để về màn hình chính!", Toast.LENGTH_LONG).show();
        });

        // 2. Nút "Lọc Hàng Hủy" -> Tự động vào trang, click tab và cào 3 cột dữ liệu
        btnFilterCancel.setOnClickListener(v -> {
            Toast.makeText(this, "Đang kết nối và cào dữ liệu hàng hủy...", Toast.LENGTH_SHORT).show();
            
            webView.setWebViewClient(new WebViewClient() {
                @Override
                public void onPageFinished(WebView view, String url) {
                    super.onPageFinished(view, url);
                    
                    if (url.contains("siteKanban")) {
                        // Mã JavaScript tự động bấm tab "Kiện hàng đợi trả về" và cào các cột dữ liệu tương ứng
                        String jsScript = "javascript:(function() {" +
                                "var spans = document.querySelectorAll('span');" +
                                "for(var i=0; i<spans.length; i++) {" +
                                "   if(spans[i].innerText.includes('Kiện hàng đợi trả về')) {" +
                                "       spans[i].click();" +
                                "       break;" +
                                "   }" +
                                "}" +
                                "setTimeout(function() {" +
                                "   var elements = document.querySelectorAll('div.best-brick-multiLine');" +
                                "   var data = [];" +
                                "   for(var j=0; j<elements.length; j++) {" +
                                "       data.push(elements[j].innerText.trim());" +
                                "   }" +
                                "   AndroidBridge.processScrapedData(JSON.stringify(data));" +
                                "}, 3000);" +
                                "})();";
                        
                        view.loadUrl(jsScript);
                    }
                }
            });

            // Tải thẳng đường dẫn đích, Cookie hệ thống tự động xác thực
            webView.loadUrl(TARGET_URL);
        });
    }

    // Giao tiếp nhận dữ liệu cào từ JavaScript trả về Java
    public class WebAppInterface {
        @JavascriptInterface
        public void processScrapedData(String jsonData) {
            try {
                JSONArray jsonArray = new JSONArray(jsonData);
                StringBuilder fileContent = new StringBuilder();

                // Gom nhóm 3 phần tử thành 1 dòng: Mã vận đơn - Số đặt hàng - Trạng thái
                for (int i = 0; i < jsonArray.length(); i += 3) {
                    String maVanDon = (i < jsonArray.length()) ? jsonArray.getString(i) : "";
                    String soDatHang = (i + 1 < jsonArray.length()) ? jsonArray.getString(i + 1) : "";
                    String trangThai = (i + 2 < jsonArray.length()) ? jsonArray.getString(i + 2) : "";

                    fileContent.append(maVanDon).append(" | ").append(soDatHang).append(" | ").append(trangThai).append("\n");
                }

                // Lưu file vào thư mục nội bộ của ứng dụng
                File file = new File(getExternalFilesDir(null), "DanhSachLocHangHuy.txt");
                FileWriter writer = new FileWriter(file, false);
                writer.write(fileContent.toString());
                writer.flush();
                writer.close();

                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Đã lưu thành công vào file DanhSachLocHangHuy.txt!", Toast.LENGTH_LONG).show());

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> Toast.makeText(MainActivity.this, "Lỗi xử lý: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }
    }

    @Override
    public void onBackPressed() {
        // Xử lý nút Back của điện thoại để tránh app bị thoát ngột khi đang xem WebView
        if (webView != null && webView.copyBackForwardList().getCurrentIndex() > 0) {
            setContentView(R.layout.activity_main);
            // Khởi tạo lại nút bấm vì layout vừa bị thay đổi
            recreate();
        } else {
            super.onBackPressed();
        }
    }
}
