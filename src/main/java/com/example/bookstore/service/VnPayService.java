package com.example.bookstore.service;

import com.example.bookstore.dto.CreatePaymentRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class VnPayService {

    @Value("${app.vnpay.pay-url}")
    private String vnpPayUrl;

    @Value("${app.vnpay.tmn-code}")
    private String vnpTmnCode;

    @Value("${app.vnpay.hash-secret}")
    private String vnpHashSecret;

    @Value("${app.vnpay.return-url}")
    private String vnpReturnUrl;

    @Value("${app.vnpay.command}")
    private String vnpCommand;

    @Value("${app.vnpay.version}")
    private String vnpVersion;

    @Value("${app.vnpay.order-type}")
    private String vnpOrderType;

    public String createPaymentUrl(CreatePaymentRequest req) throws Exception {
        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnpVersion);
        vnpParams.put("vnp_Command", vnpCommand);
        vnpParams.put("vnp_TmnCode", vnpTmnCode);

        long amount = req.getAmount() == null ? 0L : req.getAmount();
        // VNPay expects amount in smallest unit (multiply by 100)
        vnpParams.put("vnp_Amount", String.valueOf(amount * 100));

        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", req.getOrderId());
        vnpParams.put("vnp_OrderInfo", req.getOrderInfo());
        vnpParams.put("vnp_OrderType", vnpOrderType);
        vnpParams.put("vnp_Locale", req.getLanguage() == null ? "vn" : req.getLanguage());
        vnpParams.put("vnp_ReturnUrl", vnpReturnUrl);

        String createDate = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        vnpParams.put("vnp_CreateDate", createDate);

        if (req.getBankCode() != null && !req.getBankCode().isEmpty()) {
            vnpParams.put("vnp_BankCode", req.getBankCode());
        }

        // build data string
        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = vnpParams.get(fieldName);
            if (value != null && value.length() > 0) {
                // build hash data
                if (hashData.length() > 0) {
                    hashData.append('&');
                }
                hashData.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));

                // build query
                if (query.length() > 0) query.append('&');
                query.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
            }
        }

        String secureHash = hmacSHA512(vnpHashSecret, hashData.toString());
        query.append("&vnp_SecureHashType=HMACSHA512&vnp_SecureHash=").append(secureHash);

        return vnpPayUrl + "?" + query.toString();
    }

    public boolean validateSignature(Map<String, String> params) throws Exception {
        // copy params and remove secure hash
        Map<String, String> copy = new HashMap<>(params);
        String secureHash = copy.remove("vnp_SecureHash");
        copy.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(copy.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String value = copy.get(fieldName);
            if (value != null && value.length() > 0) {
                if (hashData.length() > 0) hashData.append('&');
                hashData.append(fieldName).append('=').append(URLEncoder.encode(value, StandardCharsets.US_ASCII.toString()));
            }
        }

        String calculated = hmacSHA512(vnpHashSecret, hashData.toString());
        return calculated.equalsIgnoreCase(secureHash);
    }

    private String hmacSHA512(String key, String data) throws Exception {
        Mac hmac = Mac.getInstance("HmacSHA512");
        byte[] hmacKeyBytes = key.getBytes(StandardCharsets.UTF_8);
        SecretKeySpec secretKey = new SecretKeySpec(hmacKeyBytes, "HmacSHA512");
        hmac.init(secretKey);
        byte[] hashBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(2 * hashBytes.length);
        for (byte b : hashBytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }
}
