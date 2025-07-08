package io.github.charlie237.taiyi.repository;

import io.github.charlie237.taiyi.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户数据访问接口
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    /**
     * 根据用户名查找用户
     */
    Optional<User> findByUsername(String username);
    
    /**
     * 根据邮箱查找用户
     */
    Optional<User> findByEmail(String email);
    
    /**
     * 检查用户名是否存在
     */
    boolean existsByUsername(String username);
    
    /**
     * 检查邮箱是否存在
     */
    boolean existsByEmail(String email);
    
    /**
     * 根据状态查找用户
     */
    List<User> findByStatus(User.Status status);
    
    /**
     * 根据角色查找用户
     */
    List<User> findByRole(User.Role role);
    
    /**
     * 查找指定时间后创建的用户
     */
    List<User> findByCreatedAtAfter(LocalDateTime dateTime);
    
    /**
     * 查找指定时间后登录的用户
     */
    List<User> findByLastLoginAtAfter(LocalDateTime dateTime);
    
    /**
     * 根据用户名或邮箱模糊查询
     */
    @Query("SELECT u FROM User u WHERE u.username LIKE %:keyword% OR u.email LIKE %:keyword% OR u.realName LIKE %:keyword%")
    List<User> findByKeyword(@Param("keyword") String keyword);
    
    /**
     * 统计用户数量
     */
    @Query("SELECT COUNT(u) FROM User u WHERE u.status = :status")
    long countByStatus(@Param("status") User.Status status);
}
