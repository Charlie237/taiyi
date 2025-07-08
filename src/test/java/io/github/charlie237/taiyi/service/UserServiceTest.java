package io.github.charlie237.taiyi.service;

import io.github.charlie237.taiyi.entity.User;
import io.github.charlie237.taiyi.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 用户服务测试
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setUsername("testuser");
        testUser.setPassword("password123");
        testUser.setEmail("test@example.com");
        testUser.setRealName("测试用户");
    }
    
    @Test
    void testRegisterUser() {
        // 测试用户注册
        User savedUser = userService.register(testUser);
        
        assertNotNull(savedUser.getId());
        assertEquals(testUser.getUsername(), savedUser.getUsername());
        assertEquals(testUser.getEmail(), savedUser.getEmail());
        assertEquals(User.Role.USER, savedUser.getRole());
        assertEquals(User.Status.ACTIVE, savedUser.getStatus());
        assertNotEquals(testUser.getPassword(), savedUser.getPassword()); // 密码应该被加密
    }
    
    @Test
    void testRegisterDuplicateUsername() {
        // 先注册一个用户
        userService.register(testUser);
        
        // 尝试注册相同用户名的用户
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser");
        duplicateUser.setPassword("password456");
        duplicateUser.setEmail("test2@example.com");
        
        assertThrows(RuntimeException.class, () -> userService.register(duplicateUser));
    }
    
    @Test
    void testRegisterDuplicateEmail() {
        // 先注册一个用户
        userService.register(testUser);
        
        // 尝试注册相同邮箱的用户
        User duplicateUser = new User();
        duplicateUser.setUsername("testuser2");
        duplicateUser.setPassword("password456");
        duplicateUser.setEmail("test@example.com");
        
        assertThrows(RuntimeException.class, () -> userService.register(duplicateUser));
    }
    
    @Test
    void testValidatePassword() {
        // 注册用户
        userService.register(testUser);
        
        // 验证正确密码
        assertTrue(userService.validatePassword("testuser", "password123"));
        
        // 验证错误密码
        assertFalse(userService.validatePassword("testuser", "wrongpassword"));
        
        // 验证不存在的用户
        assertFalse(userService.validatePassword("nonexistent", "password123"));
    }
    
    @Test
    void testFindByUsername() {
        // 注册用户
        userService.register(testUser);
        
        // 查找用户
        var foundUser = userService.findByUsername("testuser");
        assertTrue(foundUser.isPresent());
        assertEquals(testUser.getUsername(), foundUser.get().getUsername());
        
        // 查找不存在的用户
        var notFoundUser = userService.findByUsername("nonexistent");
        assertFalse(notFoundUser.isPresent());
    }
    
    @Test
    void testChangePassword() {
        // 注册用户
        User savedUser = userService.register(testUser);
        
        // 修改密码
        userService.changePassword(savedUser.getId(), "password123", "newpassword456");
        
        // 验证新密码
        assertTrue(userService.validatePassword("testuser", "newpassword456"));
        
        // 验证旧密码不再有效
        assertFalse(userService.validatePassword("testuser", "password123"));
    }
    
    @Test
    void testChangePasswordWithWrongOldPassword() {
        // 注册用户
        User savedUser = userService.register(testUser);
        
        // 尝试用错误的旧密码修改密码
        assertThrows(RuntimeException.class, () -> 
                userService.changePassword(savedUser.getId(), "wrongpassword", "newpassword456"));
    }
}
