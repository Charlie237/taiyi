package io.github.charlie237.taiyi.util;

import io.github.charlie237.taiyi.config.TaiyiProperties;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 密码验证工具类
 */
@Component
@RequiredArgsConstructor
public class PasswordValidator {
    
    private final TaiyiProperties taiyiProperties;
    
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile(".*[A-Z].*");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile(".*[a-z].*");
    private static final Pattern DIGIT_PATTERN = Pattern.compile(".*\\d.*");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*");
    
    /**
     * 验证密码强度
     */
    public PasswordValidationResult validatePassword(String password) {
        List<String> errors = new ArrayList<>();
        TaiyiProperties.Security.Password config = taiyiProperties.getSecurity().getPassword();
        
        if (StringUtils.isBlank(password)) {
            errors.add("密码不能为空");
            return new PasswordValidationResult(false, errors);
        }
        
        // 长度检查
        if (password.length() < config.getMinLength()) {
            errors.add(String.format("密码长度不能少于%d位", config.getMinLength()));
        }
        
        if (password.length() > config.getMaxLength()) {
            errors.add(String.format("密码长度不能超过%d位", config.getMaxLength()));
        }
        
        // 大写字母检查
        if (config.isRequireUppercase() && !UPPERCASE_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个大写字母");
        }
        
        // 小写字母检查
        if (config.isRequireLowercase() && !LOWERCASE_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个小写字母");
        }
        
        // 数字检查
        if (config.isRequireDigits() && !DIGIT_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个数字");
        }
        
        // 特殊字符检查
        if (config.isRequireSpecialChars() && !SPECIAL_CHAR_PATTERN.matcher(password).matches()) {
            errors.add("密码必须包含至少一个特殊字符");
        }
        
        // 常见弱密码检查
        if (isCommonWeakPassword(password)) {
            errors.add("密码过于简单，请使用更复杂的密码");
        }
        
        return new PasswordValidationResult(errors.isEmpty(), errors);
    }
    
    /**
     * 检查是否为常见弱密码
     */
    private boolean isCommonWeakPassword(String password) {
        String[] weakPasswords = {
                "123456", "password", "123456789", "12345678", "12345",
                "1234567", "1234567890", "qwerty", "abc123", "111111",
                "123123", "admin", "letmein", "welcome", "monkey",
                "password123", "123qwe", "qwe123", "admin123"
        };
        
        String lowerPassword = password.toLowerCase();
        for (String weak : weakPasswords) {
            if (lowerPassword.equals(weak)) {
                return true;
            }
        }
        
        // 检查连续数字或字母
        if (isSequential(password)) {
            return true;
        }
        
        // 检查重复字符
        if (isRepeating(password)) {
            return true;
        }
        
        return false;
    }
    
    /**
     * 检查是否为连续字符
     */
    private boolean isSequential(String password) {
        if (password.length() < 3) {
            return false;
        }
        
        for (int i = 0; i < password.length() - 2; i++) {
            char c1 = password.charAt(i);
            char c2 = password.charAt(i + 1);
            char c3 = password.charAt(i + 2);
            
            if (c2 == c1 + 1 && c3 == c2 + 1) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查是否为重复字符
     */
    private boolean isRepeating(String password) {
        if (password.length() < 3) {
            return false;
        }
        
        for (int i = 0; i < password.length() - 2; i++) {
            char c = password.charAt(i);
            if (password.charAt(i + 1) == c && password.charAt(i + 2) == c) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 计算密码强度分数
     */
    public int calculatePasswordStrength(String password) {
        if (StringUtils.isBlank(password)) {
            return 0;
        }
        
        int score = 0;
        
        // 长度分数
        if (password.length() >= 8) score += 25;
        else if (password.length() >= 6) score += 15;
        else score += 5;
        
        // 字符类型分数
        if (UPPERCASE_PATTERN.matcher(password).matches()) score += 15;
        if (LOWERCASE_PATTERN.matcher(password).matches()) score += 15;
        if (DIGIT_PATTERN.matcher(password).matches()) score += 15;
        if (SPECIAL_CHAR_PATTERN.matcher(password).matches()) score += 20;
        
        // 复杂度分数
        if (password.length() >= 12) score += 10;
        if (!isCommonWeakPassword(password)) score += 10;
        
        return Math.min(score, 100);
    }
    
    /**
     * 获取密码强度等级
     */
    public PasswordStrength getPasswordStrength(String password) {
        int score = calculatePasswordStrength(password);
        
        if (score >= 80) return PasswordStrength.STRONG;
        if (score >= 60) return PasswordStrength.MEDIUM;
        if (score >= 40) return PasswordStrength.WEAK;
        return PasswordStrength.VERY_WEAK;
    }
    
    /**
     * 密码验证结果
     */
    public static class PasswordValidationResult {
        private final boolean valid;
        private final List<String> errors;
        
        public PasswordValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }
        
        public boolean isValid() {
            return valid;
        }
        
        public List<String> getErrors() {
            return errors;
        }
        
        public String getErrorMessage() {
            return String.join("; ", errors);
        }
    }
    
    /**
     * 密码强度枚举
     */
    public enum PasswordStrength {
        VERY_WEAK("很弱"),
        WEAK("弱"),
        MEDIUM("中等"),
        STRONG("强");
        
        private final String description;
        
        PasswordStrength(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
}
