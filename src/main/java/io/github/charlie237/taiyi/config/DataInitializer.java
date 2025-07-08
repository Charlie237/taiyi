package io.github.charlie237.taiyi.config;

import io.github.charlie237.taiyi.common.Constants;
import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Override
    public void run(String... args) throws Exception {
        initializeAdminUser();
    }
    
    /**
     * 初始化管理员用户
     */
    private void initializeAdminUser() {
        if (!userRepository.existsByUsername(Constants.System.DEFAULT_ADMIN_USERNAME)) {
            User admin = new User();
            admin.setUsername(Constants.System.DEFAULT_ADMIN_USERNAME);
            admin.setPassword(passwordEncoder.encode(Constants.System.DEFAULT_ADMIN_PASSWORD));
            admin.setEmail(Constants.System.DEFAULT_ADMIN_EMAIL);
            admin.setRealName("系统管理员");
            admin.setRole(User.Role.ADMIN);
            admin.setStatus(User.Status.ACTIVE);
            
            userRepository.save(admin);
            log.info("默认管理员用户创建成功: {}", Constants.System.DEFAULT_ADMIN_USERNAME);
        } else {
            log.info("管理员用户已存在，跳过初始化");
        }
    }
}
