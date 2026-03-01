package com.maxxvll.common.vo;


public class CaptchaVO {
    /** 验证码 Key (用于提交时校验) */
    private String captchaKey;
    /** 验证码图片 Base64 */
    private String captchaBase64;

    // 手动添加构造方法和getter/setter
    public CaptchaVO() {}
    
    public CaptchaVO(String captchaKey, String captchaBase64) {
        this.captchaKey = captchaKey;
        this.captchaBase64 = captchaBase64;
    }
    
    public static CaptchaVOBuilder builder() {
        return new CaptchaVOBuilder();
    }
    
    public String getCaptchaKey() { return captchaKey; }
    public void setCaptchaKey(String captchaKey) { this.captchaKey = captchaKey; }
    
    public String getCaptchaBase64() { return captchaBase64; }
    public void setCaptchaBase64(String captchaBase64) { this.captchaBase64 = captchaBase64; }
    
    public static class CaptchaVOBuilder {
        private String captchaKey;
        private String captchaBase64;
        
        public CaptchaVOBuilder captchaKey(String captchaKey) {
            this.captchaKey = captchaKey;
            return this;
        }
        
        public CaptchaVOBuilder captchaBase64(String captchaBase64) {
            this.captchaBase64 = captchaBase64;
            return this;
        }
        
        public CaptchaVO build() {
            CaptchaVO vo = new CaptchaVO();
            vo.setCaptchaKey(this.captchaKey);
            vo.setCaptchaBase64(this.captchaBase64);
            return vo;
        }
    }
}
